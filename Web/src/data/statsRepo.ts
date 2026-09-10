import { onValue } from 'firebase/database'
import type { StatDefinition, StatType } from '@/domain/statCatalog'
import { TYPE_RANGE, TYPE_STARS } from '@/domain/statCatalog'
import { dbRef } from './firebase'

/**
 * Catalogo delle statistiche — porta StatsUtility.
 *
 * Le definizioni vivono nel nodo `teammaker/stats/`. La lettura è in tempo
 * reale: un cambio di label, ordine o step si riflette immediatamente nella
 * UI e nel calcolo del voto (come su Android). Nessuna scrittura da qui: la
 * gestione del catalogo è un'area a parte, ancora da portare.
 */

type RawNode = Record<string, unknown>

function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number') return Number.isFinite(value) ? value : fallback
  if (typeof value === 'string') {
    const parsed = Number(value.trim())
    return Number.isFinite(parsed) ? parsed : fallback
  }
  return fallback
}

function toString(value: unknown, fallback = ''): string {
  return typeof value === 'string' ? value : fallback
}

function toBoolean(value: unknown, fallback = false): boolean {
  if (typeof value === 'boolean') return value
  if (typeof value === 'string') return value.toLowerCase() === 'true'
  return fallback
}

/**
 * Un tipo sconosciuto ricade su STARS: come su Android, cambiare tipo di una
 * stat esistente rompe i valori salvati, quindi meglio un default sensato che
 * un crash.
 */
function toStatType(raw: unknown): StatType {
  const value = toString(raw).toUpperCase()
  return value === TYPE_RANGE ? TYPE_RANGE : TYPE_STARS
}

/** Etichette delle fasce per RANGE, come array ordinato. */
function parseValues(raw: unknown): string[] {
  if (raw === null || raw === undefined) return []
  const node = raw as RawNode
  return Object.entries(node)
    .map(([k, v]): [number, string] => [Number(k), toString(v)])
    .sort((a, b) => a[0] - b[0])
    .map(([, s]) => s)
}

function parseStatDefinition(key: string, raw: unknown): StatDefinition | null {
  const node = (raw ?? {}) as RawNode
  const label = toString(node['label'])
  if (label.length === 0) return null

  const type = toStatType(node['type'])
  return {
    key,
    label,
    type,
    max: toNumber(node['max'], 4),
    step: toNumber(node['step'], 1),
    order: toNumber(node['order']),
    // `allow_bonus` ha senso solo per STARS; nell'app Android per RANGE il
    // campo non viene neppure scritto. Ignoriamo eventuali valori spuri.
    allowBonus: type === TYPE_STARS && toBoolean(node['allow_bonus']),
    values: type === TYPE_RANGE ? parseValues(node['values']) : [],
  }
}

/**
 * Ascolta il catalogo delle stat in tempo reale.
 *
 * Restituisce sempre l'elenco già ordinato per `order` (così i consumatori
 * non devono preoccuparsene). Chiama `onChange` col catalogo attuale a ogni
 * modifica lato server, incluso il primo caricamento.
 *
 * @returns una funzione per interrompere l'ascolto.
 */
export function subscribeStats(
  onChange: (catalog: StatDefinition[]) => void,
  onError?: (error: Error) => void,
): () => void {
  return onValue(
    dbRef('stats'),
    (snapshot) => {
      const raw = (snapshot.val() ?? {}) as RawNode
      const defs: StatDefinition[] = []
      for (const [key, value] of Object.entries(raw)) {
        const def = parseStatDefinition(key, value)
        if (def !== null) defs.push(def)
      }
      defs.sort((a, b) => a.order - b.order)
      onChange(defs)
    },
    (error) => onError?.(error),
  )
}
