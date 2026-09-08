import type { Match, Team, Tournament } from './models'
import { FINAL, QUARTER, SEMIFINAL, THIRD, isGroup } from './phases'
import { computeStandings } from './standings'

/**
 * Fasi finali a eliminazione diretta — porta FinalStageGenerator e
 * FinalStageResolver, unificati qui perché usano lo stesso vocabolario dei
 * placeholder degli slot squadra.
 *
 * Ogni slot di partita può nascere già assegnato (`keyTeamN`) oppure con una
 * SORGENTE (`sourceNType`/`sourceNRef`) che indica come derivare la squadra:
 *   - `STANDING`     posizione della classifica generale ("1", "2"…)
 *   - `GROUP_STANDING`  posizione dentro un girone ("A1", "B2"…)
 *   - `WINNER`       vincente di una partita sorgente (ref = key)
 *   - `LOSER`        perdente di una partita sorgente (ref = key)
 *
 * `STANDING` e `GROUP_STANDING` si risolvono solo quando la fase iniziale è
 * completa; `WINNER`/`LOSER` appena la partita sorgente ha un esito.
 */

// ---------------------------------------------------------------------------
// Costanti e sorgenti
// ---------------------------------------------------------------------------

export const SOURCE_STANDING = 'STANDING'
export const SOURCE_GROUP_STANDING = 'GROUP_STANDING'
export const SOURCE_WINNER = 'WINNER'
export const SOURCE_LOSER = 'LOSER'

/** Segnaposto usato dall'app Android nelle key delle partite senza squadra. */
export const TO_DO_KEY = 'TO DO'

/** Dimensioni tabellone valide. */
export const QUARTER_SIZE = 8
export const SEMI_SIZE = 4
export const FINAL_SIZE = 2

// ---------------------------------------------------------------------------
// Generatore fasi finali
// ---------------------------------------------------------------------------

/**
 * Dimensioni tabellone disponibili per questo torneo:
 * - senza gironi: ogni dimensione ≤ numero di squadre
 * - con gironi: `size` deve essere multiplo del numero di gironi e ogni
 *   posizione seed ≤ dimensione del girone più piccolo (altrimenti resterebbe
 *   uno slot senza squadra alla risoluzione)
 */
export function availableFinalSizes(tournament: Tournament): number[] {
  const sizes: number[] = []
  const teamCount = tournament.teams.length
  const groups = tournament.nBracket >= 2

  for (const size of [QUARTER_SIZE, SEMI_SIZE, FINAL_SIZE]) {
    if (groups) {
      const g = tournament.nBracket
      const perGroup = size / g
      if (size % g === 0 && perGroup <= minGroupSize(tournament) && perGroup >= 1) {
        sizes.push(size)
      }
    } else if (teamCount >= size) {
      sizes.push(size)
    }
  }
  return sizes
}

/**
 * Partita nuova in fase finale, senza key né orario. La UI le assegnerà day/time
 * (le fasce orarie sono gestite come nel calendario di girone).
 */
export type FinalStageMatch = Omit<Match, 'key'> & { key?: string }

/**
 * Prefisso dei ref "pendenti" usati in `source1Ref`/`source2Ref` durante la
 * generazione: `pending:<indice-nel-batch>`. Il repo li rimpiazza con la key
 * push() vera della partita target prima di scrivere. Non arrivano mai su
 * Firebase.
 */
export const PENDING_REF_PREFIX = 'pending:'

/**
 * `true` se un ref è un placeholder di generazione da rimpiazzare col la key
 * vera prima della scrittura su Firebase.
 */
export function isPendingRef(ref: string): boolean {
  return ref.startsWith(PENDING_REF_PREFIX)
}

/** Indice della partita target nel batch da un ref pendente. */
export function pendingRefIndex(ref: string): number {
  return Number(ref.substring(PENDING_REF_PREFIX.length))
}

/**
 * Costruisce le partite delle fasi finali con i placeholder degli slot.
 *
 * Le key vengono lasciate `undefined`: WINNER/LOSER dei turni successivi
 * puntano alle partite precedenti con ref pendenti `pending:<n>`, dove `n` è
 * l'indice nel batch restituito. Il repo che salva assegna le key vere e
 * rimpiazza i ref pendenti in una sola scrittura.
 */
export function generateFinalStages(
  tournament: Tournament,
  bracketSize: number,
  thirdPlace: boolean,
): FinalStageMatch[] {
  if (!availableFinalSizes(tournament).includes(bracketSize)) return []

  const groups = tournament.nBracket >= 2
  const g = groups ? tournament.nBracket : 1

  const all: FinalStageMatch[] = []
  const indexOf = new Map<FinalStageMatch, number>()

  const push = (m: FinalStageMatch): number => {
    all.push(m)
    const i = all.length - 1
    indexOf.set(m, i)
    return i
  }
  const pendingTo = (target: FinalStageMatch): string =>
    `${PENDING_REF_PREFIX}${indexOf.get(target)}`

  // Primo turno: coppie dall'ordine standard del tabellone
  const order = seedOrder(bracketSize)
  const firstRound: FinalStageMatch[] = []
  const firstType = labelForCount(bracketSize / 2)
  for (let i = 0; i < bracketSize; i += 2) {
    const seed1 = order[i]!
    const seed2 = order[i + 1]!
    const match = emptyFinalMatch(firstType)
    setSeedSource(match, 1, seed1, groups, g)
    setSeedSource(match, 2, seed2, groups, g)
    push(match)
    firstRound.push(match)
  }

  // Turni successivi: vincente vs vincente, fino alla finale
  let round = firstRound
  let semis: FinalStageMatch[] | null = null
  while (round.length > 1) {
    if (round.length === 2) {
      semis = round // livello che alimenta la finale = semifinali
    }
    const next: FinalStageMatch[] = []
    const type = labelForCount(round.length / 2)
    for (let i = 0; i < round.length; i += 2) {
      const m = emptyFinalMatch(type)
      m.source1Type = SOURCE_WINNER
      m.source1Ref = pendingTo(round[i]!)
      m.source2Type = SOURCE_WINNER
      m.source2Ref = pendingTo(round[i + 1]!)
      push(m)
      next.push(m)
    }
    round = next
  }

  // Finale 3°/4° posto (opzionale)
  if (thirdPlace) {
    if (semis !== null) {
      // Partenza da quarti/semi: perdenti delle semifinali
      const m = emptyFinalMatch(THIRD)
      m.source1Type = SOURCE_LOSER
      m.source1Ref = pendingTo(semis[0]!)
      m.source2Type = SOURCE_LOSER
      m.source2Ref = pendingTo(semis[1]!)
      push(m)
    } else if (bracketSize === FINAL_SIZE && availableFinalSizes(tournament).includes(SEMI_SIZE)) {
      // Partenza dalla finale: 3ª e 4ª di classifica (seed 3 e 4)
      const m = emptyFinalMatch(THIRD)
      setSeedSource(m, 1, 3, groups, g)
      setSeedSource(m, 2, 4, groups, g)
      push(m)
    }
  }

  return all
}

/**
 * Ordine standard del tabellone: 1 e 2 su lati opposti.
 * Es. N=8 → 1, 8, 4, 5, 2, 7, 3, 6.
 */
function seedOrder(n: number): number[] {
  let order: number[] = [1]
  for (let size = 2; size <= n; size *= 2) {
    const next: number[] = []
    for (const seed of order) {
      next.push(seed)
      next.push(size + 1 - seed)
    }
    order = next
  }
  return order
}

/**
 * Etichetta di fase (codice canonico) dato il numero di partite di quel turno.
 * 4 partite → QUARTER, 2 → SEMIFINAL, 1 → FINAL.
 */
function labelForCount(matchCount: number): string {
  if (matchCount >= 4) return QUARTER
  if (matchCount === 2) return SEMIFINAL
  return FINAL
}

/**
 * Riempie la sorgente `slot` di `match` col seed. Per i gironi il seeding
 * canonico incrocia le teste di serie: `seed(g, p) = p*G + g + 1`, così le
 * prime dei gironi finiscono su lati opposti.
 */
function setSeedSource(
  match: FinalStageMatch,
  slot: 1 | 2,
  seed: number,
  groups: boolean,
  g: number,
): void {
  let type: string
  let ref: string
  if (groups) {
    const s0 = seed - 1
    const groupIndex = s0 % g
    const position = Math.floor(s0 / g) + 1
    type = SOURCE_GROUP_STANDING
    ref = String.fromCharCode(65 + groupIndex) + position
  } else {
    type = SOURCE_STANDING
    ref = String(seed)
  }
  if (slot === 1) {
    match.source1Type = type
    match.source1Ref = ref
  } else {
    match.source2Type = type
    match.source2Ref = ref
  }
}

function emptyFinalMatch(type: string): FinalStageMatch {
  return {
    keyTeam1: TO_DO_KEY,
    keyTeam2: TO_DO_KEY,
    day: 0,
    time: '0:00',
    points1: 0,
    points2: 0,
    detail: [],
    type,
    source1Type: '',
    source1Ref: '',
    source2Type: '',
    source2Ref: '',
  }
}

/** Numero minimo di squadre in un girone. Con gironi vuoti (edge case) → 0. */
function minGroupSize(tournament: Tournament): number {
  const counts = new Map<string, number>()
  for (const team of tournament.teams) {
    counts.set(team.bracket, (counts.get(team.bracket) ?? 0) + 1)
  }
  let min = Infinity
  for (const c of counts.values()) if (c < min) min = c
  return min === Infinity ? 0 : min
}

// ---------------------------------------------------------------------------
// Resolver: dai placeholder alle squadre reali
// ---------------------------------------------------------------------------

/**
 * Ritorna la key della squadra che occupa lo slot indicato, oppure `null` se
 * non è ancora determinabile (fase iniziale non chiusa, partita sorgente
 * senza esito).
 */
export function resolveSlot(
  tournament: Tournament,
  type: string,
  ref: string,
): string | null {
  if (type.length === 0 || ref.length === 0) return null

  switch (type) {
    case SOURCE_STANDING:
      return isInitialPhaseComplete(tournament)
        ? standingTeam(tournament, tournament.teams, parseIntOrZero(ref))
        : null
    case SOURCE_GROUP_STANDING:
      return isInitialPhaseComplete(tournament) ? groupStandingTeam(tournament, ref) : null
    case SOURCE_WINNER:
      return winnerOf(tournament, ref, true)
    case SOURCE_LOSER:
      return winnerOf(tournament, ref, false)
    default:
      return null
  }
}

/**
 * Aggiornamenti da applicare: `{ matchKey: { keyTeam1?, keyTeam2? } }`.
 * Il repo li scrive con `updateChildren`.
 */
export interface ResolvedUpdates {
  updates: Map<string, { keyTeam1?: string; keyTeam2?: string }>
}

/**
 * Calcola le assegnazioni team1/team2 per tutte le partite che le hanno
 * risolvibili. Ripete finché emergono novità (catene vincente → vincente).
 *
 * Non tocca Firebase: restituisce le modifiche da applicare, così il repo
 * può fare un solo `update` atomico.
 */
export function resolveAllFinalStages(tournament: Tournament): ResolvedUpdates {
  const updates = new Map<string, { keyTeam1?: string; keyTeam2?: string }>()

  // Lavoriamo su una copia mutabile delle partite così le risoluzioni a cascata
  // trovano lo stato aggiornato dei "vincenti" quando arrivano alle partite
  // che ne dipendono (semi → finale).
  const workingMatches = tournament.matches.map((m) => ({ ...m }))
  const working: Tournament = { ...tournament, matches: workingMatches }

  let changed = true
  while (changed) {
    changed = false
    for (const match of workingMatches) {
      if (match.source1Type.length > 0) {
        const key = resolveSlot(working, match.source1Type, match.source1Ref)
        if (key !== null && key !== match.keyTeam1) {
          match.keyTeam1 = key
          patch(updates, match.key, { keyTeam1: key })
          changed = true
        }
      }
      if (match.source2Type.length > 0) {
        const key = resolveSlot(working, match.source2Type, match.source2Ref)
        if (key !== null && key !== match.keyTeam2) {
          match.keyTeam2 = key
          patch(updates, match.key, { keyTeam2: key })
          changed = true
        }
      }
    }
  }

  return { updates }
}

function patch(
  updates: Map<string, { keyTeam1?: string; keyTeam2?: string }>,
  matchKey: string,
  fields: { keyTeam1?: string; keyTeam2?: string },
): void {
  const cur = updates.get(matchKey) ?? {}
  updates.set(matchKey, { ...cur, ...fields })
}

/**
 * La fase iniziale è completa quando esiste almeno una partita di girone e
 * tutte quelle di girone hanno un esito (`points1 !== points2`).
 */
function isInitialPhaseComplete(tournament: Tournament): boolean {
  let hasGroupMatch = false
  for (const m of tournament.matches) {
    if (!isGroup(m.type)) continue
    hasGroupMatch = true
    if (m.points1 === m.points2) return false
  }
  return hasGroupMatch
}

/**
 * Squadra alla posizione richiesta in una classifica calcolata su `pool`.
 * `pool` può essere l'intero torneo (STANDING) o un girone (GROUP_STANDING).
 */
function standingTeam(tournament: Tournament, pool: Team[], position: number): string | null {
  if (pool.length === 0 || position < 1) return null
  // Riusa il computeStandings, prendendo solo il ramo che ci interessa.
  // Costruiamo un finto torneo che contiene SOLO le squadre del pool.
  const virtual: Tournament = { ...tournament, teams: pool }
  const standings = computeStandings(virtual)
  const rows = standings.flatMap((g) => g.rows)
  if (position > rows.length) return null
  return rows[position - 1]!.team.key
}

/** Girone e posizione da un ref tipo "A1" / "B2". */
function groupStandingTeam(tournament: Tournament, ref: string): string | null {
  let split = 0
  while (split < ref.length && !isDigit(ref[split]!)) split++
  if (split === 0 || split >= ref.length) return null

  const letter = ref.substring(0, split)
  const position = parseIntOrZero(ref.substring(split))

  const groupTeams = tournament.teams.filter((t) => t.bracket.endsWith(letter))
  return standingTeam(tournament, groupTeams, position)
}

/**
 * Vincente o perdente della partita sorgente. `null` se la partita non esiste,
 * non ha esito, oppure ha ancora squadre TO DO (a sua volta non risolta).
 */
function winnerOf(tournament: Tournament, matchKey: string, wantWinner: boolean): string | null {
  const source = tournament.matches.find((m) => m.key === matchKey)
  if (source === undefined) return null
  if (source.points1 === source.points2) return null
  const team1Won = source.points1 > source.points2
  const key = wantWinner === team1Won ? source.keyTeam1 : source.keyTeam2
  return key.length > 0 && key !== TO_DO_KEY ? key : null
}

function isDigit(ch: string): boolean {
  return ch >= '0' && ch <= '9'
}

function parseIntOrZero(value: string): number {
  const n = Number(value.trim())
  return Number.isFinite(n) ? Math.trunc(n) : 0
}

// ---------------------------------------------------------------------------
// Etichette placeholder per la UI (porta MatchLabelUtility)
// ---------------------------------------------------------------------------

/**
 * Etichetta breve dello slot: la squadra reale se nota, altrimenti il
 * placeholder ("1ª GIR. A", "VINC. Q1", "PERD. S2"…).
 */
export function slotLabel(
  tournament: Tournament,
  match: Match,
  slot: 1 | 2,
  fallbackTeamName: (teamKey: string) => string,
): string {
  const key = slot === 1 ? match.keyTeam1 : match.keyTeam2
  const type = slot === 1 ? match.source1Type : match.source2Type
  const ref = slot === 1 ? match.source1Ref : match.source2Ref

  const teamKnown =
    key.length > 0 && key !== TO_DO_KEY && tournament.teams.some((t) => t.key === key)

  if (!teamKnown && type.length > 0) {
    return placeholderLabel(tournament, type, ref)
  }
  return fallbackTeamName(key)
}

function placeholderLabel(tournament: Tournament, type: string, ref: string): string {
  switch (type) {
    case SOURCE_STANDING:
      return `${ref}ª`
    case SOURCE_GROUP_STANDING: {
      let split = 0
      while (split < ref.length && !isDigit(ref[split]!)) split++
      const letter = ref.substring(0, split)
      const pos = ref.substring(split)
      return `${pos}ª GIR. ${letter}`
    }
    case SOURCE_WINNER:
      return `VINC. ${matchCode(tournament, ref)}`
    case SOURCE_LOSER:
      return `PERD. ${matchCode(tournament, ref)}`
    default:
      return 'TO DO'
  }
}

/** Codice breve (Q1..Q4, S1..S2, F) della partita finale per i placeholder. */
function matchCode(tournament: Tournament, matchKey: string): string {
  const quarters: Match[] = []
  const semis: Match[] = []
  let finalMatch: Match | undefined
  for (const m of tournament.matches) {
    if (m.type === QUARTER) quarters.push(m)
    else if (m.type === SEMIFINAL) semis.push(m)
    else if (m.type === FINAL) finalMatch = m
  }
  sortByDayTime(quarters)
  sortByDayTime(semis)

  for (let i = 0; i < quarters.length; i++) {
    if (quarters[i]!.key === matchKey) return `Q${i + 1}`
  }
  for (let i = 0; i < semis.length; i++) {
    if (semis[i]!.key === matchKey) return `S${i + 1}`
  }
  if (finalMatch?.key === matchKey) return 'F'
  return '?'
}

function sortByDayTime(matches: Match[]): void {
  matches.sort((a, b) => {
    if (a.day !== b.day) return a.day - b.day
    return toMinutes(a.time) - toMinutes(b.time)
  })
}

function toMinutes(time: string): number {
  const parts = time.split(':')
  const h = Number(parts[0] ?? 0)
  const m = Number(parts[1] ?? 0)
  return h * 60 + m
}
