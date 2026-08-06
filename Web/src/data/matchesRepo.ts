import { update } from 'firebase/database'
import { dbRef } from './firebase'

/**
 * Scritture sul nodo `matches/` di un torneo.
 * Porta la parte di MatchUtility che serve al segnapunti.
 */

/**
 * Salva il risultato di una partita.
 *
 * ⚠️ `points1` e `points2` vanno scritti come STRINGHE: è il formato che l'app
 * Android si aspetta di rileggere. Nessun altro campo viene toccato, così un
 * salvataggio non può alterare per sbaglio orario, giornata o squadre.
 */
export async function saveMatchResult(
  tournamentKey: string,
  matchKey: string,
  points1: number,
  points2: number,
): Promise<void> {
  await update(dbRef(`tournaments/${tournamentKey}/matches/${matchKey}`), {
    points1: String(points1),
    points2: String(points2),
  })
}
