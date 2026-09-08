import type { Match } from './models'

/**
 * Formattazione del risultato di una partita — porta `Match.detailString()`
 * più le regole dei layout di visualizzazione.
 *
 * Interpretazione dei numeri `points1` / `points2` della partita:
 *   - se `detail` è pieno → sono i SET vinti (headline "2 - 1")
 *   - altrimenti sono i punti di un set unico (legacy: "25 - 10")
 * L'algoritmo della classifica sa già distinguere i due casi; qui serve solo
 * la stringa da mostrare.
 */

/**
 * Punti dei singoli set formattati, es. `"25-20 · 22-25 · 15-12"`.
 * Ritorna `null` se non c'è dettaglio da mostrare (partite legacy o non
 * ancora giocate a set), così le viste non stampano una riga vuota.
 */
export function formatSetDetail(match: Match): string | null {
  if (match.detail.length === 0) return null
  return match.detail.map((set) => `${set[0] ?? 0}-${set[1] ?? 0}`).join(' · ')
}
