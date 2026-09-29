import type { StatDefinition } from '@/domain/statCatalog'
import { TYPE_RANGE } from '@/domain/statCatalog'

/**
 * Statistica come sequenza di stelle, portata da PlayerInfoAdapter.
 *
 * Dal formato 0.8 (Android 43ef524) `def.max` è direttamente il numero
 * massimo di stelle e `value` è il numero di stelle scelte, entrambi interi:
 * il conteggio è quindi diretto — `max + 1` stelle totali, `value + 1` piene
 * (indice 0 incluso, come su Android, per avere sempre una stella accesa
 * anche a valore zero).
 *
 * Le stat di tipo RANGE (l'altezza è l'esempio classico) mostrano invece
 * la fascia scelta. Il valore salvato è l'indice della fascia; per prudenza
 * lo clampiamo ai limiti dell'elenco.
 */
export function StatStars({ stat, value }: { stat: StatDefinition; value: number }) {
  if (stat.type === TYPE_RANGE) {
    if (stat.values.length === 0) {
      return <span className="text-sm text-list-text-muted">—</span>
    }
    const index = Math.min(Math.max(Math.trunc(value), 0), stat.values.length - 1)
    return <span className="text-sm text-list-text-secondary">{stat.values[index]}</span>
  }

  const total = Math.max(Math.floor(stat.max), 0) + 1
  const filled = Math.max(Math.floor(value), 0) + 1

  return (
    <span className="tracking-tight" aria-label={`${filled} stelle su ${total}`}>
      {Array.from({ length: total }, (_, i) => (
        <span key={i} className={i < filled ? 'text-stars' : 'text-list-text-muted/40'}>
          ★
        </span>
      ))}
    </span>
  )
}
