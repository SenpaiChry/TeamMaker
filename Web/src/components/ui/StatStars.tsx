import { HEIGHT_VALUES, STAT_MAX, STAT_STEP, type StatKey } from '@/domain/constants'

/**
 * Statistica come sequenza di stelle, portata da PlayerInfoAdapter.
 *
 * Il conteggio riproduce il Java, indice `i` incluso: le stelle totali sono
 * `max/step + 1` e quelle piene `valore/step + 1`. C'è quindi sempre almeno
 * una stella piena, anche a statistica zero.
 *
 * L'altezza fa eccezione: invece delle stelle mostra la fascia in centimetri.
 */
export function StatStars({ stat, value }: { stat: StatKey; value: number }) {
  if (stat === 'height') {
    const index = Math.min(Math.max(Math.trunc(value), 0), HEIGHT_VALUES.length - 1)
    return <span className="text-sm text-list-text-secondary">{HEIGHT_VALUES[index]}</span>
  }

  const total = Math.floor(STAT_MAX[stat] / STAT_STEP[stat]) + 1
  const filled = Math.floor(value / STAT_STEP[stat]) + 1

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
