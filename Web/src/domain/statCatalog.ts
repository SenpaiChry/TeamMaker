/**
 * Definizioni delle statistiche — porta StatDefinition + StatsUtility.
 *
 * Il catalogo delle stat non è più cablato nel codice: vive nel nodo
 * `teammaker/stats` di Firebase e viene letto in tempo reale. Ogni stat ha
 * una PUSH-KEY opaca come identità (non la label), così l'admin può
 * rinominare "Attacco" → "Attacco potente" senza rompere i valori salvati
 * nei giocatori.
 *
 * Sul Player le stat vivono in `player.stats[statKey]`; se una stat ammette
 * il bonus (`allowBonus: true`), il flag per-giocatore vive in
 * `player.bonus[statKey]` e aggiunge `def.step` al voto.
 */

export const TYPE_STARS = 'STARS'
export const TYPE_RANGE = 'RANGE'

export type StatType = typeof TYPE_STARS | typeof TYPE_RANGE

export interface StatDefinition {
  /** Push-key opaca su Firebase; è l'identità stabile della stat. */
  key: string
  /** Testo mostrato all'utente (può essere rinominato senza rompere niente). */
  label: string
  /**
   * `STARS` (default) → il valore va da 0 a `max` a passi di `step`,
   * rappresentato con stelle. `RANGE` → indice in `values`, tipico dell'altezza.
   */
  type: StatType
  /** Valore massimo (per STARS). Per RANGE è `values.length - 1`. */
  max: number
  /** Ogni "stella" quanto vale sul voto. Per RANGE, incremento tra fascia e fascia. */
  step: number
  /** Ordine di visualizzazione: `getDefinitions()` ordina sempre per questo. */
  order: number
  /** Se `true`, un giocatore può avere una stella bonus (aggiunge `step` al voto). */
  allowBonus: boolean
  /** Etichette delle fasce per `RANGE` (es. "150-160cm"); vuoto per `STARS`. */
  values: string[]
}
