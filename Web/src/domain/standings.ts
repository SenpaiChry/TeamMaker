import type { Match, Team, Tournament } from './models'
import { isGroup } from './phases'

/**
 * Classifica del torneo — porta StandingsUtility.java.
 *
 * Catena tie-break (dalla più forte alla più debole):
 *   1) punti classifica
 *   2) vittorie
 *   3) quoziente set (setsWon / setsLost)
 *   4) quoziente punti (pointsFor / pointsAgainst)
 *   5) mini-classifica ricorsiva fra le squadre ancora pari (solo gli scontri
 *      diretti fra loro): 2 pari = scontro diretto, 3+ = mini-lega, Champions =
 *      se le pari non si sono affrontate restano pari.
 *
 * Regola punti classifica (aggiornata al formato set + detail):
 *   - più set (winSets ≥ 2): scarto ≥ 2 → 3/0; scarto 1 → 2/1
 *   - set unico (1-0) con detail:  vantaggi (max > 25) → 2/1, altrimenti 3/0
 *   - legacy senza detail (punti > 5): vecchia regola 15/25 = netta (3/0),
 *     tutto il resto ai vantaggi (2/1)
 *
 * Interpretazione di points1/points2 sulla stessa partita:
 *   - se `detail` è pieno → sono i SET vinti
 *   - altrimenti euristica per grandezza: max ≤ 5 = set, altrimenti punti
 *   (i due numeri sono sempre esclusivi, mai entrambi)
 */

/** Sotto/uguale a questa soglia i due numeri sono set, sopra sono punti. */
const SET_SCORE_MAX = 5

// ---------------------------------------------------------------------------
// Riga di classifica e statistiche interne
// ---------------------------------------------------------------------------

/** Statistiche di una squadra calcolate su un insieme di partite. */
interface TeamStats {
  classificaPoints: number
  wins: number
  setsWon: number
  setsLost: number
  pointsFor: number
  pointsAgainst: number
}

function emptyStats(): TeamStats {
  return {
    classificaPoints: 0,
    wins: 0,
    setsWon: 0,
    setsLost: 0,
    pointsFor: 0,
    pointsAgainst: 0,
  }
}

/** Riga di classifica: squadra + statistiche + posizione. */
export interface StandingRow {
  team: Team
  classificaPoints: number
  wins: number
  setsWon: number
  setsLost: number
  pointsFor: number
  pointsAgainst: number
  /**
   * Posizione della squadra. Le squadre pari su TUTTI i criteri (chain +
   * scontro diretto) condividono lo stesso rank.
   */
  rank: number
  /**
   * `true` quando è la sola posizione condivisa con una squadra vicina che le
   * separa lo scontro diretto: la UI ci mette un piccolo "SD" per segnalare
   * che l'ordine è dovuto a quello. Marca sempre entrambe le squadre coinvolte.
   */
  directClash: boolean
}

/** Classifica di un girone, già ordinata. */
export interface BracketStandings {
  /** Lettera del girone; stringa vuota se il calendario non è stato generato. */
  bracket: string
  rows: StandingRow[]
}

// ---------------------------------------------------------------------------
// API pubblica
// ---------------------------------------------------------------------------

/** `true` se la partita concorre alla classifica. */
export function countsForStandings(match: Match): boolean {
  return isGroup(match.type)
}

/**
 * Punti classifica assegnati da una singola partita, come coppia
 * [punti squadra 1, punti squadra 2]. `[0, 0]` se la partita non è di girone o
 * non è ancora stata giocata (points1 == points2).
 */
export function pointsForMatch(match: Match): [number, number] {
  if (!countsForStandings(match)) return [0, 0]
  if (match.points1 === match.points2) return [0, 0]

  const team1Wins = match.points1 > match.points2
  const [winnerPts, loserPts] = classificaPointsFor(match)

  return team1Wins ? [winnerPts, loserPts] : [loserPts, winnerPts]
}

/**
 * Classifica completa, raggruppata per girone e ordinata con la catena
 * tie-break.
 */
export function computeStandings(tournament: Tournament): BracketStandings[] {
  // Le partite di girone sono le uniche che contano: filtrarle una volta sola
  // evita di rifarlo dentro ogni chiamata ricorsiva.
  const groupMatches = tournament.matches.filter(countsForStandings)

  const byBracket = new Map<string, Team[]>()
  for (const team of tournament.teams) {
    const list = byBracket.get(team.bracket)
    if (list === undefined) byBracket.set(team.bracket, [team])
    else list.push(team)
  }

  const sortedBrackets = [...byBracket.entries()].sort(([a], [b]) => a.localeCompare(b))

  return sortedBrackets.map(([bracket, teams]) => ({
    bracket,
    rows: buildRows(teams, groupMatches),
  }))
}

// ---------------------------------------------------------------------------
// Ordinamento delle squadre
// ---------------------------------------------------------------------------

/**
 * Ordina le squadre di un girone secondo la catena di criteri, poi calcola per
 * ciascuna la posizione (con rank condiviso per le pari totali) e il flag
 * `directClash` per chi è ordinata solo grazie allo scontro diretto.
 */
function buildRows(teams: Team[], matches: Match[]): StandingRow[] {
  const stats = computeStats(teams, matches)
  const ordered = orderTeams(teams, matches, stats)

  const rows: StandingRow[] = ordered.map((team) => {
    const s = stats.get(team.key) ?? emptyStats()
    return {
      team,
      classificaPoints: s.classificaPoints,
      wins: s.wins,
      setsWon: s.setsWon,
      setsLost: s.setsLost,
      pointsFor: s.pointsFor,
      pointsAgainst: s.pointsAgainst,
      rank: 1,
      directClash: false,
    }
  })

  // Rank condiviso per squadre pari su tutto, marchio SD per chi le separa
  // solo lo scontro diretto.
  for (let i = 1; i < rows.length; i++) {
    const cur = rows[i]!
    const prev = rows[i - 1]!
    const chainTied = compareByChain(stats.get(prev.team.key)!, stats.get(cur.team.key)!) === 0
    const headSeparates = chainTied && !headToHeadEqual(prev.team, cur.team, matches)

    cur.rank = chainTied && !headSeparates ? prev.rank : i + 1
    if (headSeparates) {
      prev.directClash = true
      cur.directClash = true
    }
  }

  return rows
}

/**
 * Applica la catena di criteri, poi ririsolve ricorsivamente i gruppi di
 * squadre pari sui criteri 1–4 con una mini-classifica sui soli scontri diretti
 * fra loro.
 *
 * Le `stats` iniziali sono calcolate sul girone intero e servono per il primo
 * passaggio; nei rami ricorsivi ne calcoliamo di nuove, ristrette al gruppo.
 */
function orderTeams(
  teams: Team[],
  matches: Match[],
  stats: Map<string, TeamStats>,
): Team[] {
  if (teams.length <= 1) return [...teams]

  const sorted = [...teams].sort((a, b) =>
    compareByChain(stats.get(a.key)!, stats.get(b.key)!),
  )

  const result: Team[] = []
  let i = 0
  while (i < sorted.length) {
    let j = i + 1
    while (
      j < sorted.length &&
      compareByChain(stats.get(sorted[i]!.key)!, stats.get(sorted[j]!.key)!) === 0
    ) {
      j++
    }

    const tied = sorted.slice(i, j)
    if (tied.length === 1 || tied.length === sorted.length) {
      // Singola, oppure tutte pari e irriducibili: nessuna mini-classifica
      // possibile — mantiene l'ordine corrente.
      result.push(...tied)
    } else {
      // Mini-classifica: stessi criteri, ma solo gli scontri fra le pari.
      const miniStats = computeStats(tied, matches)
      result.push(...orderTeams(tied, matches, miniStats))
    }
    i = j
  }
  return result
}

/**
 * `true` se lo scontro diretto fra le due squadre non le separa (non si sono
 * affrontate, oppure la mini-lega a due le lascia pari).
 */
function headToHeadEqual(a: Team, b: Team, matches: Match[]): boolean {
  const mini = computeStats([a, b], matches)
  return compareByChain(mini.get(a.key)!, mini.get(b.key)!) === 0
}

// ---------------------------------------------------------------------------
// Statistiche
// ---------------------------------------------------------------------------

/**
 * Calcola le statistiche di ogni squadra a partire dalle partite di girone
 * giocate FRA le squadre passate (le altre vengono ignorate). Serve sia sul
 * girone intero, sia sui sottoinsiemi per la mini-classifica.
 */
function computeStats(teams: Team[], matches: Match[]): Map<string, TeamStats> {
  const keys = new Set(teams.map((t) => t.key))
  const stats = new Map<string, TeamStats>()
  for (const team of teams) stats.set(team.key, emptyStats())

  for (const match of matches) {
    // Solo gli scontri fra le squadre passate (per la mini-classifica).
    if (!keys.has(match.keyTeam1) || !keys.has(match.keyTeam2)) continue
    if (match.points1 === match.points2) continue // non giocata / senza esito

    const s1 = stats.get(match.keyTeam1)!
    const s2 = stats.get(match.keyTeam2)!

    const team1Wins = match.points1 > match.points2
    const winner = team1Wins ? s1 : s2
    const loser = team1Wins ? s2 : s1
    winner.wins++

    const [winnerPts, loserPts] = classificaPointsFor(match)
    winner.classificaPoints += winnerPts
    loser.classificaPoints += loserPts

    if (match.detail.length > 0) {
      // Formato nuovo: points1/points2 = set vinti, detail = punti dei set
      s1.setsWon += match.points1
      s1.setsLost += match.points2
      s2.setsWon += match.points2
      s2.setsLost += match.points1
      for (const set of match.detail) {
        const p1 = set[0] ?? 0
        const p2 = set[1] ?? 0
        s1.pointsFor += p1
        s1.pointsAgainst += p2
        s2.pointsFor += p2
        s2.pointsAgainst += p1
      }
    } else {
      // Legacy: euristica per grandezza.
      const isSetMatch = Math.max(match.points1, match.points2) <= SET_SCORE_MAX
      if (isSetMatch) {
        s1.setsWon += match.points1
        s1.setsLost += match.points2
        s2.setsWon += match.points2
        s2.setsLost += match.points1
      } else {
        if (team1Wins) {
          s1.setsWon += 1
          s2.setsLost += 1
        } else {
          s2.setsWon += 1
          s1.setsLost += 1
        }
        s1.pointsFor += match.points1
        s1.pointsAgainst += match.points2
        s2.pointsFor += match.points2
        s2.pointsAgainst += match.points1
      }
    }
  }

  return stats
}

/**
 * Punti classifica per una singola partita, come `[vincitore, perdente]`.
 * Assume che la partita sia già stata riconosciuta come giocata (points diversi)
 * e che concorra alla classifica.
 */
function classificaPointsFor(match: Match): [number, number] {
  const v1 = match.points1
  const v2 = match.points2
  const hasDetail = match.detail.length > 0
  const isSets = hasDetail || Math.max(v1, v2) <= SET_SCORE_MAX

  if (isSets) {
    const winSets = Math.max(v1, v2)
    const loseSets = Math.min(v1, v2)
    if (winSets >= 2) {
      // Più set: scarto ≥ 2 vittoria netta (3/0), scarto 1 al set decisivo (2/1)
      const netta = winSets - loseSets >= 2
      return netta ? [3, 0] : [2, 1]
    }
    // Set unico: ai vantaggi (punti del set > 25) → 2/1, altrimenti netta 3/0
    let vantaggi = false
    if (hasDetail) {
      const last = match.detail[match.detail.length - 1]!
      vantaggi = Math.max(last[0] ?? 0, last[1] ?? 0) > 25
    }
    return vantaggi ? [2, 1] : [3, 0]
  }

  // Legacy non migrata (punti di un set unico): vecchia regola 15/25 = netta.
  const winVal = Math.max(v1, v2)
  const netta = winVal === 15 || winVal === 25
  return netta ? [3, 0] : [2, 1]
}

// ---------------------------------------------------------------------------
// Confronto sui criteri
// ---------------------------------------------------------------------------

/** Confronto sui criteri 1–4 (0 = pari, da spezzare con la mini-classifica). */
function compareByChain(a: TeamStats, b: TeamStats): number {
  if (a.classificaPoints !== b.classificaPoints) {
    return b.classificaPoints - a.classificaPoints
  }
  if (a.wins !== b.wins) {
    return b.wins - a.wins
  }

  const setCmp = compareQuotient(a.setsWon, a.setsLost, b.setsWon, b.setsLost)
  if (setCmp !== 0) return setCmp

  return compareQuotient(a.pointsFor, a.pointsAgainst, b.pointsFor, b.pointsAgainst)
}

/**
 * Confronta due quozienti vinti/persi senza divisioni (evita la divisione per
 * zero): `a/aLost` vs `b/bLost` diventa `aWon*bLost` vs `bWon*aLost`.
 * Quoziente più alto prima. Numeri interi entro `Number.MAX_SAFE_INTEGER`
 * (il massimo prodotto in un torneo reale sta ampiamente dentro).
 */
function compareQuotient(aWon: number, aLost: number, bWon: number, bLost: number): number {
  const left = aWon * bLost
  const right = bWon * aLost
  return right - left
}
