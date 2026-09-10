import type { StatDefinition } from '@/domain/statCatalog'
import { TYPE_RANGE } from '@/domain/statCatalog'

/**
 * Statistica come sequenza di stelle, portata da PlayerInfoAdapter.
 *
 * Il conteggio riproduce il Java, indice `i` incluso: le stelle totali sono
 * `max/step + 1` e quelle piene `valore/step + 1`. C'è quindi sempre almeno
 * una stella piena, anche a statistica zero.
 *
 * Se la stat è di tipo RANGE (es. l'altezza) invece delle stelle mostra la
 * fascia scelta dall'utente. L'indice della fascia è `valore/step`, con
 * clamping ai limiti dell'elenco per non uscire mai fuori.
 */
export function StatStars({ stat, value }: { stat: StatDefinition; value: number }) {
  if (stat.type === TYPE_RANGE) {
    if (stat.values.length === 0) {
      return <span className="text-sm text-list-text-muted">—</span>
    }
    const rawIndex = stat.step > 0 ? Math.trunc(value / stat.step) : 0
    const index = Math.min(Math.max(rawIndex, 0), stat.values.length - 1)
    return <span className="text-sm text-list-text-secondary">{stat.values[index]}</span>
  }

  const step = stat.step > 0 ? stat.step : 1
  const total = Math.floor(stat.max / step) + 1
  const filled = Math.floor(value / step) + 1

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
