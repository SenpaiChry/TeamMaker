import type { Player, Team } from './models'
import { getVote } from './player'
import { createRng, type Rng } from './rng'

/**
 * Generazione delle squadre bilanciate, portata da TeamGeneratorUtility.java.
 *
 * Due algoritmi:
 *   - `generateSerpentine` (algoritmo 2 nel Java): distribuzione a serpentina,
 *     istantanea ma approssimativa.
 *   - `searchBalancedTeams` (algoritmo 5): ricerca con vincoli, è quella usata
 *     in produzione dall'app. Bilancia contemporaneamente il voto totale delle
 *     squadre e la distribuzione delle giocatrici (scarto massimo di 1).
 *
 * ⚠️ Il Java lancia la ricerca su N thread e attende fino a 5 minuti. Sul web
 * questo modulo resta sincrono e puro: la parallelizzazione la gestisce il
 * worker in `src/workers/`, lanciando più istanze con semi diversi.
 *
 * Nota sulla precisione: il Java usa `float`, JavaScript usa sempre `double`.
 * I voti sono somme di valori con step 0.5, esattamente rappresentabili in
 * entrambi, quindi i risultati coincidono; una divergenza sarebbe possibile
 * solo per confronti esattamente sul limite della tolleranza.
 */

export interface GenerateOptions {
  /** Giocatori per squadra: 2, 3, 4 o 5. */
  playersPerTeam: number
  /**
   * Differenza di voto massima accettata fra la squadra più forte e la più
   * debole. L'app Android la forza a 0 (precisione massima): la tolleranza
   * effettiva parte comunque da `baseTolerance` e si allarga a ogni tentativo.
   */
  maxDifference?: number
  /** Tentativi massimi prima di arrendersi. Il Java usa 50 000. */
  maxRetries?: number
  /** Seme del generatore pseudocasuale, per risultati riproducibili. */
  seed?: number
  /**
   * Indice del worker: entra nel calcolo della forza di perturbazione, così
   * istanze diverse esplorano lo spazio delle soluzioni in modo diverso.
   */
  threadIndex?: number
}

export interface GenerationResult {
  teams: Team[]
  /** Tentativo al quale è stata trovata la soluzione (0-based). */
  retries: number
  /** Differenza di voto fra la squadra più forte e la più debole. */
  difference: number
}

const DEFAULT_MAX_RETRIES = 50_000

/** Numero di squadre necessarie per ospitare tutti i giocatori. */
export function countTeams(playerCount: number, playersPerTeam: number): number {
  return Math.ceil(playerCount / playersPerTeam)
}

/**
 * `true` se ci sono abbastanza giocatori per formare almeno due squadre.
 * Porta il controllo `playersSelected.size() >= btnSelectedPlayer * 2 - 1`
 * della schermata di generazione.
 */
export function hasEnoughPlayers(playerCount: number, playersPerTeam: number): boolean {
  return playerCount >= playersPerTeam * 2 - 1
}

/**
 * Algoritmo 2: distribuzione a serpentina.
 *
 * Ordina i giocatori per voto decrescente e li distribuisce a rotazione,
 * invertendo l'ordine delle squadre a ogni giro completo. Porta `algorithm2`
 * compresa l'inversione della lista, che determina l'ordine finale delle squadre.
 */
export function generateSerpentine(players: Player[], playersPerTeam: number): Team[] {
  const nTeams = countTeams(players.length, playersPerTeam)
  if (nTeams === 0) return []

  const sorted = [...players].sort((a, b) => getVote(b) - getVote(a))
  let buckets: Player[][] = Array.from({ length: nTeams }, () => [])

  sorted.forEach((player, i) => {
    if (i % nTeams === 0 && i !== 0) {
      buckets.reverse()
    }
    buckets[i % nTeams]!.push(player)
  })

  return buckets.map((playersInTeam, i) => ({
    key: `generated-${i + 1}`,
    bracket: '',
    players: playersInTeam,
  }))
}

/**
 * Algoritmo 5: ricerca di una ripartizione che rispetti entrambi i vincoli.
 *
 * A ogni tentativo perturba l'ordine dei giocatori, assegna con una greedy
 * "il prossimo va alla squadra più debole", poi prova a correggere con degli
 * scambi mirati. La tolleranza si allarga di 1/10000 a ogni tentativo, così
 * la ricerca converge sempre invece di girare a vuoto.
 *
 * Restituisce `null` se non trova nulla entro `maxRetries`.
 */
export function searchBalancedTeams(
  players: Player[],
  options: GenerateOptions,
): GenerationResult | null {
  const {
    playersPerTeam,
    maxDifference = 0,
    maxRetries = DEFAULT_MAX_RETRIES,
    seed = Date.now(),
    threadIndex = 0,
  } = options

  const nPlayers = players.length
  const nTeams = countTeams(nPlayers, playersPerTeam)
  if (nPlayers === 0 || nTeams === 0) return null

  const rng = createRng(seed)
  const votes = players.map(getVote)
  const isFemale = players.map((p) => p.gender === 'F')

  // Ordine di partenza: dal più forte al più debole.
  const indices = votes
    .map((_, i) => i)
    .sort((a, b) => votes[b]! - votes[a]!)

  const baseTolerance = computeBaseTolerance(votes, nTeams)
  const floorTolerance = Math.max(maxDifference, baseTolerance)

  const assignment = new Array<number>(nPlayers).fill(0)
  const teamVotes = new Float64Array(nTeams)
  const teamSizes = new Int32Array(nTeams)
  const teamFemales = new Int32Array(nTeams)

  for (let retry = 0; retry < maxRetries; retry++) {
    // La perturbazione è cumulativa: `indices` non viene mai riportato
    // all'ordine iniziale, esattamente come nel Java.
    perturb(indices, rng, 1 + (retry % 4) + threadIndex)
    teamVotes.fill(0)
    teamSizes.fill(0)
    teamFemales.fill(0)

    for (let i = 0; i < nPlayers; i++) {
      const playerIdx = indices[i]!
      const weakest = findWeakestTeam(teamVotes, teamSizes, playersPerTeam)
      if (weakest === -1) break

      assignment[playerIdx] = weakest
      teamVotes[weakest] = teamVotes[weakest]! + votes[playerIdx]!
      teamSizes[weakest]! += 1
      if (isFemale[playerIdx]) teamFemales[weakest]! += 1
    }

    const tolerance = floorTolerance + retry / 10_000

    if (isValid(teamVotes, teamFemales, tolerance)) {
      return commit(players, assignment, nTeams, retry, teamVotes)
    }

    if (!isFemaleBalanced(teamFemales)) {
      balanceFemales(assignment, votes, isFemale, teamVotes, teamFemales, nPlayers, nTeams)
      if (isValid(teamVotes, teamFemales, tolerance)) {
        return commit(players, assignment, nTeams, retry, teamVotes)
      }
    }

    balanceVotes(assignment, votes, isFemale, teamVotes, teamFemales, nPlayers, nTeams, tolerance)
    if (isValid(teamVotes, teamFemales, tolerance)) {
      return commit(players, assignment, nTeams, retry, teamVotes)
    }
  }

  return null
}

// ---------------------------------------------------------------------------
// Funzioni interne — porting 1:1 dei metodi privati di TeamGeneratorUtility
// ---------------------------------------------------------------------------

/** Squadra con il voto più basso fra quelle non ancora piene; -1 se sono tutte piene. */
function findWeakestTeam(
  teamVotes: Float64Array,
  teamSizes: Int32Array,
  maxSize: number,
): number {
  let best = -1
  for (let i = 0; i < teamVotes.length; i++) {
    if (teamSizes[i]! >= maxSize) continue
    if (best === -1 || teamVotes[i]! < teamVotes[best]!) best = i
  }
  return best
}

/** Entrambi i vincoli soddisfatti: scarto di voto entro la tolleranza e squadre bilanciate per genere. */
function isValid(teamVotes: Float64Array, teamFemales: Int32Array, tolerance: number): boolean {
  const { min, max } = minMax(teamVotes)
  return max - min <= tolerance && isFemaleBalanced(teamFemales)
}

/** Le giocatrici sono distribuite con uno scarto massimo di 1 fra le squadre. */
function isFemaleBalanced(teamFemales: Int32Array): boolean {
  let min = Number.MAX_SAFE_INTEGER
  let max = 0
  for (const f of teamFemales) {
    if (f < min) min = f
    if (f > max) max = f
  }
  return max - min <= 1
}

function minMax(values: Float64Array): { min: number; max: number } {
  let min = Number.MAX_VALUE
  let max = 0
  for (const v of values) {
    if (v < min) min = v
    if (v > max) max = v
  }
  return { min, max }
}

/**
 * Riequilibra le giocatrici scambiando una donna della squadra che ne ha di più
 * con un uomo di quella che ne ha di meno, scegliendo la coppia con i voti più
 * simili per non sbilanciare la classifica dei voti.
 */
function balanceFemales(
  assignment: number[],
  votes: number[],
  isFemale: boolean[],
  teamVotes: Float64Array,
  teamFemales: Int32Array,
  nPlayers: number,
  nTeams: number,
): void {
  for (let pass = 0; pass < nTeams; pass++) {
    let richTeam = 0
    let poorTeam = 0
    for (let i = 1; i < nTeams; i++) {
      if (teamFemales[i]! > teamFemales[richTeam]!) richTeam = i
      if (teamFemales[i]! < teamFemales[poorTeam]!) poorTeam = i
    }
    if (teamFemales[richTeam]! - teamFemales[poorTeam]! <= 1) break

    let bestFemale = -1
    let bestMale = -1
    let bestDelta = Number.MAX_VALUE

    for (let p = 0; p < nPlayers; p++) {
      if (assignment[p] !== richTeam || !isFemale[p]) continue
      for (let q = 0; q < nPlayers; q++) {
        if (assignment[q] !== poorTeam || isFemale[q]) continue
        const delta = Math.abs(votes[p]! - votes[q]!)
        if (delta < bestDelta) {
          bestDelta = delta
          bestFemale = p
          bestMale = q
        }
      }
    }

    if (bestFemale === -1 || bestMale === -1) break

    teamVotes[richTeam] = teamVotes[richTeam]! + votes[bestMale]! - votes[bestFemale]!
    teamVotes[poorTeam] = teamVotes[poorTeam]! + votes[bestFemale]! - votes[bestMale]!
    teamFemales[richTeam]! -= 1
    teamFemales[poorTeam]! += 1
    assignment[bestFemale] = poorTeam
    assignment[bestMale] = richTeam
  }
}

/**
 * Riequilibra i voti scambiando un giocatore forte della squadra più forte con
 * uno debole della più debole, scartando gli scambi che romperebbero il
 * bilanciamento delle giocatrici.
 */
function balanceVotes(
  assignment: number[],
  votes: number[],
  isFemale: boolean[],
  teamVotes: Float64Array,
  teamFemales: Int32Array,
  nPlayers: number,
  nTeams: number,
  tolerance: number,
): void {
  for (let pass = 0; pass < 20; pass++) {
    let richTeam = 0
    let poorTeam = 0
    for (let i = 1; i < nTeams; i++) {
      if (teamVotes[i]! > teamVotes[richTeam]!) richTeam = i
      if (teamVotes[i]! < teamVotes[poorTeam]!) poorTeam = i
    }
    if (teamVotes[richTeam]! - teamVotes[poorTeam]! <= tolerance) break

    let bestA = -1
    let bestB = -1
    let bestImprovement = 0

    for (let p = 0; p < nPlayers; p++) {
      if (assignment[p] !== richTeam) continue
      for (let q = 0; q < nPlayers; q++) {
        if (assignment[q] !== poorTeam) continue
        if (votes[p]! <= votes[q]!) continue

        const newRich = teamVotes[richTeam]! - votes[p]! + votes[q]!
        const newPoor = teamVotes[poorTeam]! - votes[q]! + votes[p]!
        const oldDiff = teamVotes[richTeam]! - teamVotes[poorTeam]!
        const improvement = oldDiff - Math.abs(newRich - newPoor)

        if (improvement <= bestImprovement) continue

        if (isFemale[p] !== isFemale[q]) {
          const simulated = Int32Array.from(teamFemales)
          if (isFemale[p]) {
            simulated[richTeam]! -= 1
            simulated[poorTeam]! += 1
          } else {
            simulated[richTeam]! += 1
            simulated[poorTeam]! -= 1
          }
          if (!isFemaleBalanced(simulated)) continue
        }

        bestImprovement = improvement
        bestA = p
        bestB = q
      }
    }

    if (bestA === -1 || bestB === -1) break

    if (isFemale[bestA] !== isFemale[bestB]) {
      if (isFemale[bestA]) {
        teamFemales[richTeam]! -= 1
        teamFemales[poorTeam]! += 1
      } else {
        teamFemales[richTeam]! += 1
        teamFemales[poorTeam]! -= 1
      }
    }

    teamVotes[richTeam] = teamVotes[richTeam]! + votes[bestB]! - votes[bestA]!
    teamVotes[poorTeam] = teamVotes[poorTeam]! + votes[bestA]! - votes[bestB]!
    assignment[bestA] = poorTeam
    assignment[bestB] = richTeam
  }
}

/**
 * Mescola una finestra scelta a caso dell'ordine di partenza (Fisher-Yates
 * limitato). Più `strength` è alta, più la finestra è ampia e la ricerca si
 * allontana dall'ordine per voto decrescente.
 */
function perturb(indices: number[], rng: Rng, strength: number): void {
  const n = indices.length
  const windowSize = Math.min(n, 4 * strength)
  const start = rng.nextInt(Math.max(1, n - windowSize + 1))

  for (let i = start + windowSize - 1; i > start; i--) {
    const j = start + rng.nextInt(i - start + 1)
    const tmp = indices[i]!
    indices[i] = indices[j]!
    indices[j] = tmp
  }
}

/**
 * Tolleranza di partenza: deviazione standard dei voti divisa per il numero di
 * squadre. Più i giocatori si somigliano, più il vincolo è stretto.
 */
function computeBaseTolerance(votes: number[], nTeams: number): number {
  if (votes.length === 0) return 0

  const mean = votes.reduce((sum, v) => sum + v, 0) / votes.length
  const variance = votes.reduce((sum, v) => sum + (v - mean) ** 2, 0) / votes.length
  return Math.sqrt(variance) / nTeams
}

/** Costruisce il risultato a partire dall'assegnazione trovata. */
function commit(
  players: Player[],
  assignment: number[],
  nTeams: number,
  retry: number,
  teamVotes: Float64Array,
): GenerationResult {
  const buckets: Player[][] = Array.from({ length: nTeams }, () => [])
  players.forEach((player, i) => {
    buckets[assignment[i]!]!.push(player)
  })

  const { min, max } = minMax(teamVotes)

  return {
    teams: buckets.map((playersInTeam, i) => ({
      key: `generated-${i + 1}`,
      bracket: '',
      players: playersInTeam,
    })),
    retries: retry,
    difference: max - min,
  }
}
