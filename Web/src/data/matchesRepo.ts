import { push, remove, set, update } from 'firebase/database'
import type { Match } from '@/domain/models'
import { dbRef } from './firebase'
import { serializeMatch } from './mappers'

/**
 * Scritture sul nodo `matches/` di un torneo.
 * Porta MatchUtility.
 *
 * ⚠️ `day`, `points1` e `points2` vengono scritti come STRINGHE: è il formato
 * che l'app Android si aspetta di rileggere. Se ne occupa `serializeMatch`.
 */

/**
 * Salva il risultato di una partita.
 * Tocca solo i punti, così un salvataggio non può alterare per sbaglio orario,
 * giornata, squadre o tipo.
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

/** Sostituisce interamente una partita. Porta editMatch. */
export async function updateMatch(tournamentKey: string, match: Match): Promise<void> {
  await update(dbRef(`tournaments/${tournamentKey}/matches/${match.key}`), serializeMatch(match))
}

/**
 * Scrive in blocco le partite di un calendario appena generato.
 *
 * Differenza voluta rispetto al Java: `addNewMatch` scriveva una partita alla
 * volta, campo per campo, e fra una e l'altra ricreava l'activity. Qui il
 * calendario viene composto in memoria e scritto in una sola operazione, così
 * non può restare a metà.
 */
export async function replaceMatches(
  tournamentKey: string,
  matches: Omit<Match, 'key'>[],
): Promise<void> {
  const node: Record<string, unknown> = {}

  for (const match of matches) {
    const ref = push(dbRef(`tournaments/${tournamentKey}/matches`))
    if (ref.key === null) continue
    node[ref.key] = serializeMatch(match)
  }

  await set(dbRef(`tournaments/${tournamentKey}/matches`), node)
}

/** Aggiunge una singola partita. Porta addNewMatch. */
export async function addMatch(
  tournamentKey: string,
  match: Omit<Match, 'key'>,
): Promise<string> {
  const ref = push(dbRef(`tournaments/${tournamentKey}/matches`))
  if (ref.key === null) throw new Error('Firebase non ha restituito una chiave per la partita.')

  await set(ref, serializeMatch(match))
  return ref.key
}

export async function deleteMatch(tournamentKey: string, matchKey: string): Promise<void> {
  await remove(dbRef(`tournaments/${tournamentKey}/matches/${matchKey}`))
}

/**
 * Cancella il calendario e riporta le squadre senza girone.
 * Porta deleteEveryMatch.
 */
export async function deleteAllMatches(
  tournamentKey: string,
  teamKeys: string[],
): Promise<void> {
  await remove(dbRef(`tournaments/${tournamentKey}/matches`))
  await set(dbRef(`tournaments/${tournamentKey}/nBracket`), 0)
  await Promise.all(
    teamKeys.map((key) => set(dbRef(`tournaments/${tournamentKey}/teams/${key}/bracket`), '')),
  )
}
