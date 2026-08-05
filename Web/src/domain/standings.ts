import type { Match, Team, Tournament } from './models'

/**
 * Classifica del torneo, portata da Utility.generateTable().
 *
 * Regola di assegnazione dei punti (pallavolo):
 *   - vittoria "netta" (il vincitore chiude a 15 o a 25):  3 punti al vincitore, 0 al perdente
 *   - vittoria ai vantaggi (punteggio diverso da 15/25):   2 punti al vincitore, 1 al perdente
 *   - pareggio o partita non giocata:                      nessun punto
 *
 * Contano solo le partite di girone: il Java filtra su `type` contenente
 * "BRACKET" o "GIRONE", escludendo così eventuali partite di altro tipo.
 */

/** Punteggi che indicano un set chiuso senza andare ai vantaggi. */
const CLEAN_WIN_SCORES = [15, 25]

/** `true` se la partita concorre alla classifica. */
export function countsForStandings(match: Match): boolean {
  return match.type.includes('BRACKET') || match.type.includes('GIRONE')
}

/**
 * Punti assegnati da una singola partita, come coppia [punti squadra 1, punti squadra 2].
 */
export function pointsForMatch(match: Match): [number, number] {
  if (!countsForStandings(match)) return [0, 0]

  if (match.points1 > match.points2) {
    return CLEAN_WIN_SCORES.includes(match.points1) ? [3, 0] : [2, 1]
  }
  if (match.points2 > match.points1) {
    return CLEAN_WIN_SCORES.includes(match.points2) ? [0, 3] : [1, 2]
  }
  return [0, 0]
}

/**
 * Punti di ogni squadra, indicizzati per chiave.
 *
 * Differenza voluta rispetto al Java: le partite che citano una squadra non più
 * presente nel torneo vengono ignorate. `Utility.generateTable` in quel caso
 * indicizzava con -1 e faceva crashare l'app.
 */
export function computePoints(tournament: Tournament): Map<string, number> {
  const points = new Map<string, number>()
  for (const team of tournament.teams) {
    points.set(team.key, 0)
  }

  for (const match of tournament.matches) {
    const [p1, p2] = pointsForMatch(match)
    if (p1 === 0 && p2 === 0) continue

    const current1 = points.get(match.keyTeam1)
    const current2 = points.get(match.keyTeam2)
    if (current1 === undefined || current2 === undefined) continue

    points.set(match.keyTeam1, current1 + p1)
    points.set(match.keyTeam2, current2 + p2)
  }

  return points
}

/** Riga di classifica: la squadra, i suoi punti e la posizione nel proprio girone. */
export interface StandingRow {
  team: Team
  points: number
  /** Posizione all'interno del girone, 1-based. */
  position: number
}

/** Classifica di un girone, già ordinata. */
export interface BracketStandings {
  /** Lettera del girone; stringa vuota se il calendario non è stato generato. */
  bracket: string
  rows: StandingRow[]
}

/**
 * Classifica completa, raggruppata per girone e ordinata per punti decrescenti.
 * Porta la logica del costruttore di TournamentTableAdapter, che ordinava per
 * girone e poi per punti e inseriva un'intestazione a ogni cambio di girone.
 */
export function computeStandings(tournament: Tournament): BracketStandings[] {
  const points = computePoints(tournament)

  const sorted = [...tournament.teams].sort((a, b) => {
    const byBracket = a.bracket.localeCompare(b.bracket)
    if (byBracket !== 0) return byBracket
    return (points.get(b.key) ?? 0) - (points.get(a.key) ?? 0)
  })

  const brackets: BracketStandings[] = []
  for (const team of sorted) {
    let group = brackets.at(-1)
    if (group === undefined || group.bracket !== team.bracket) {
      group = { bracket: team.bracket, rows: [] }
      brackets.push(group)
    }
    group.rows.push({
      team,
      points: points.get(team.key) ?? 0,
      position: group.rows.length + 1,
    })
  }

  return brackets
}
