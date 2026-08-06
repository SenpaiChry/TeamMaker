import { onValue, push, remove, set, update } from 'firebase/database'
import type { Player, Team, Tournament } from '@/domain/models'
import { dbRef } from './firebase'
import { formatDate, parseTournaments, serializeTeam } from './mappers'

/**
 * Accesso al nodo `tournaments/`.
 * Porta TournamentUtility e TournamentTeamUtility.
 *
 * ⚠️ `is_valid` è la STRINGA "true"/"false", non un booleano: è così che l'app
 * Android lo scrive e lo rilegge.
 */

/**
 * Ascolta i tornei. I tornei referenziano i giocatori solo per chiave, quindi
 * serve l'anagrafica già risolta: è lo stesso motivo per cui l'app Android
 * avviava questo listener solo dopo quello dei giocatori.
 */
export function subscribeToTournaments(
  players: Map<string, Player>,
  onChange: (tournaments: Tournament[]) => void,
  onError?: (error: Error) => void,
): () => void {
  return onValue(
    dbRef('tournaments'),
    (snapshot) => {
      onChange(parseTournaments(snapshot.val(), players))
    },
    (error) => onError?.(error),
  )
}

/** Disattiva tutti i tornei: ce ne può essere al massimo uno attivo. */
export async function deactivateAllTournaments(tournaments: Tournament[]): Promise<void> {
  await Promise.all(
    tournaments
      .filter((t) => t.isValid)
      .map((t) => set(dbRef(`tournaments/${t.key}/is_valid`), 'false')),
  )
}

/** Rende attivo un torneo, disattivando quello che lo era. */
export async function setActiveTournament(
  tournaments: Tournament[],
  tournamentKey: string,
): Promise<void> {
  await deactivateAllTournaments(tournaments)
  await set(dbRef(`tournaments/${tournamentKey}/is_valid`), 'true')
}

/**
 * Crea un torneo dalle squadre appena generate e lo rende attivo.
 * Porta saveNewTournamentTeams.
 */
export async function createTournament(
  existing: Tournament[],
  name: string,
  date: Date,
  teams: Team[],
): Promise<string> {
  const ref = push(dbRef('tournaments'))
  if (ref.key === null) throw new Error('Firebase non ha restituito una chiave per il torneo.')

  await deactivateAllTournaments(existing)

  const teamsNode: Record<string, unknown> = {}
  for (const team of teams) {
    const teamRef = push(dbRef(`tournaments/${ref.key}/teams`))
    if (teamRef.key === null) continue
    teamsNode[teamRef.key] = serializeTeam({ bracket: '', players: team.players })
  }

  await set(ref, {
    is_valid: 'true',
    name,
    nBracket: 0,
    date: formatDate(date),
    teams: teamsNode,
  })

  return ref.key
}

/** Aggiorna nome e data. Porta updateNameAndDateTournament. */
export async function updateTournamentDetails(
  tournamentKey: string,
  name: string,
  date: Date,
): Promise<void> {
  await update(dbRef(`tournaments/${tournamentKey}`), { name, date: formatDate(date) })
}

export async function deleteTournament(tournamentKey: string): Promise<void> {
  await remove(dbRef(`tournaments/${tournamentKey}`))
}

/** Scrive il girone assegnato a ciascuna squadra. Porta saveBracketForTeams. */
export async function saveTeamBrackets(
  tournamentKey: string,
  bracketByTeam: Map<string, string>,
): Promise<void> {
  await Promise.all(
    [...bracketByTeam].map(([teamKey, bracket]) =>
      set(dbRef(`tournaments/${tournamentKey}/teams/${teamKey}/bracket`), bracket),
    ),
  )
}

export async function updateBracketCount(tournamentKey: string, count: number): Promise<void> {
  await set(dbRef(`tournaments/${tournamentKey}/nBracket`), count)
}
