import { push, remove, set, update } from 'firebase/database'
import type { Match, Tournament } from '@/domain/models'
import {
  isPendingRef,
  pendingRefIndex,
  resolveAllFinalStages,
  type FinalStageMatch,
} from '@/domain/finalStages'
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
 *
 * Tocca solo punti e dettaglio dei set, così un salvataggio non può alterare
 * per sbaglio orario, giornata, squadre o tipo. Il `detail` viene scritto
 * sempre — anche se vuoto — per non lasciare i punti di un risultato precedente
 * quando questo viene sovrascritto (es. si è sbagliato a inserirlo).
 */
export async function saveMatchResult(
  tournamentKey: string,
  matchKey: string,
  points1: number,
  points2: number,
  detail: number[][] = [],
): Promise<void> {
  await update(dbRef(`tournaments/${tournamentKey}/matches/${matchKey}`), {
    points1: String(points1),
    points2: String(points2),
    detail: detail.map((set) => ({
      points1: String(set[0] ?? 0),
      points2: String(set[1] ?? 0),
    })),
  })
}

/**
 * Sostituisce interamente una partita. Porta editMatch.
 *
 * Non chiama `resolveFinalStages` da sé: il chiamante che ha in mano il
 * torneo aggiornato lo fa esplicitamente, così la risoluzione tiene conto
 * dei nuovi valori appena scritti senza dover attendere il round trip col
 * listener Firebase.
 */
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

/**
 * Aggiunge le partite di una fase finale in un'unica scrittura, risolvendo i
 * ref pendenti `pending:<n>` con le key push() vere.
 *
 * Porta `MatchUtility.saveMatches` + parte di `ActivityPopUpGenerateFinals`.
 */
export async function addFinalStageMatches(
  tournamentKey: string,
  matches: FinalStageMatch[],
): Promise<void> {
  if (matches.length === 0) return

  // Prima assegno una key vera a ogni partita del batch: è quella che va nei
  // WINNER/LOSER dei turni successivi.
  const keys = matches.map(() => {
    const ref = push(dbRef(`tournaments/${tournamentKey}/matches`))
    if (ref.key === null) throw new Error('Firebase non ha restituito una chiave per la partita.')
    return ref.key
  })

  // Rimpiazzo i ref pendenti con le key vere
  const resolved = matches.map((m) => ({
    ...m,
    source1Ref: isPendingRef(m.source1Ref) ? keys[pendingRefIndex(m.source1Ref)]! : m.source1Ref,
    source2Ref: isPendingRef(m.source2Ref) ? keys[pendingRefIndex(m.source2Ref)]! : m.source2Ref,
  }))

  const updates: Record<string, unknown> = {}
  for (let i = 0; i < resolved.length; i++) {
    updates[keys[i]!] = serializeMatch(resolved[i]!)
  }
  await update(dbRef(`tournaments/${tournamentKey}/matches`), updates)
}

/**
 * Risolve i placeholder delle fasi finali sullo stato attuale del torneo e
 * scrive gli aggiornamenti in una singola operazione.
 *
 * Va chiamato dopo qualunque scrittura che possa cambiare l'esito di una
 * partita (salvataggio punteggio, modifica partita) o rendere la fase iniziale
 * completa: propaga vincenti/perdenti alle partite che ne dipendono.
 */
export async function resolveFinalStages(tournament: Tournament): Promise<void> {
  const { updates } = resolveAllFinalStages(tournament)
  if (updates.size === 0) return

  const flat: Record<string, unknown> = {}
  for (const [matchKey, fields] of updates) {
    if (fields.keyTeam1 !== undefined) flat[`${matchKey}/team1`] = fields.keyTeam1
    if (fields.keyTeam2 !== undefined) flat[`${matchKey}/team2`] = fields.keyTeam2
  }
  await update(dbRef(`tournaments/${tournament.key}/matches`), flat)
}
