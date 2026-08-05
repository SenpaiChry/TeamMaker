import { BYE_KEY } from './constants'
import type { Match, Team } from './models'
import { formatTime, parseTime, type TimeSlot } from './time'

/**
 * Generazione del calendario, portata da TournamentScheduleUtility.java.
 *
 * Due modalità:
 *   - girone all'italiana: tutte le squadre in un unico girone "A", con le
 *     partite ordinate per massimizzare il riposo fra un impegno e il successivo;
 *   - gironi multipli: le squadre distribuite a rotazione su N gironi, con le
 *     partite dei vari gironi alternate una a una.
 *
 * Tutte le funzioni sono pure: restituiscono le partite da salvare, non le
 * scrivono. La persistenza è compito di `src/data/`.
 */

/** Partita non ancora programmata: manca l'assegnazione di giornata e orario. */
export type PlannedMatch = Omit<Match, 'key' | 'day' | 'time'>

export interface Schedule {
  /** Partite in ordine di gioco, con giornata e orario già assegnati. */
  matches: Omit<Match, 'key'>[]
  /** Girone assegnato a ciascuna squadra, indicizzato per chiave. */
  bracketByTeam: Map<string, string>
  /** Numero di gironi generati. */
  bracketCount: number
}

/** Le fasce orarie non bastano a ospitare tutte le partite generate. */
export class NotEnoughTimeError extends Error {
  constructor(
    readonly scheduled: number,
    readonly total: number,
  ) {
    super(
      `Fasce orarie esaurite: programmate ${scheduled} partite su ${total}. ` +
        'Aggiungi una fascia oraria o riduci la durata delle partite.',
    )
    this.name = 'NotEnoughTimeError'
  }
}

// ---------------------------------------------------------------------------
// Etichette dei gironi
// ---------------------------------------------------------------------------

/** Lettera del girone: 0 → "A", 25 → "Z", 26 → "AA". Porta `getBracketLetter`. */
export function getBracketLetter(index: number): string {
  let label = ''
  let i = index
  while (i >= 0) {
    label = String.fromCharCode(65 + (i % 26)) + label
    i = Math.floor(i / 26) - 1
  }
  return label
}

/** Etichetta completa usata nel campo `type` delle partite. Porta `getBracketLabel`. */
export function getBracketLabel(index: number): string {
  return `BRACKET ${getBracketLetter(index)}`
}

/**
 * Etichetta del girone all'italiana.
 * ⚠️ Lo spazio finale è intenzionale: è quello che scrive l'app Android e la
 * classifica filtra le partite con `type.includes('GIRONE')`.
 */
export const ITALIAN_BRACKET_LABEL = 'GIRONE '

// ---------------------------------------------------------------------------
// Round robin
// ---------------------------------------------------------------------------

/**
 * Turni di un girone all'italiana con il metodo del cerchio.
 * Con un numero dispari di squadre ne aggiunge una fittizia, e le partite
 * contro di essa (il turno di riposo) vengono scartate.
 * Porta `buildRoundRobinRounds`.
 */
export function buildRoundRobinRounds(teams: Team[], typeLabel: string): PlannedMatch[][] {
  const keys = teams.map((t) => t.key)
  if (keys.length % 2 === 1) keys.push(BYE_KEY)

  const n = keys.length
  if (n < 2) return []

  const half = n / 2
  let rotation = [...keys]
  const rounds: PlannedMatch[][] = []

  for (let r = 0; r < n - 1; r++) {
    const round: PlannedMatch[] = []
    for (let i = 0; i < half; i++) {
      const key1 = rotation[i]!
      const key2 = rotation[n - 1 - i]!
      if (key1 !== BYE_KEY && key2 !== BYE_KEY) {
        round.push({ keyTeam1: key1, keyTeam2: key2, points1: 0, points2: 0, type: typeLabel })
      }
    }
    rounds.push(round)

    // La prima squadra resta ferma, le altre ruotano di una posizione.
    rotation = [rotation[0]!, rotation[n - 1]!, ...rotation.slice(1, n - 1)]
  }

  return rounds
}

/**
 * Ordina le partite di ogni turno scegliendo ogni volta quella fra le squadre
 * che hanno riposato più a lungo, così nessuno gioca due volte di fila.
 * Porta `orderRoundsBySpacing`.
 */
export function orderRoundsBySpacing(rounds: PlannedMatch[][]): PlannedMatch[] {
  const ordered: PlannedMatch[] = []
  const lastPlayedAt = new Map<string, number>()
  let slot = 0

  for (const round of rounds) {
    const pool = [...round]

    while (pool.length > 0) {
      let bestIndex = 0
      let bestMinRest = Number.NEGATIVE_INFINITY
      let bestSumRest = Number.NEGATIVE_INFINITY

      pool.forEach((match, i) => {
        const rest1 = slot - (lastPlayedAt.get(match.keyTeam1) ?? -1000)
        const rest2 = slot - (lastPlayedAt.get(match.keyTeam2) ?? -1000)
        const minRest = Math.min(rest1, rest2)
        const sumRest = rest1 + rest2

        if (minRest > bestMinRest || (minRest === bestMinRest && sumRest > bestSumRest)) {
          bestIndex = i
          bestMinRest = minRest
          bestSumRest = sumRest
        }
      })

      const [best] = pool.splice(bestIndex, 1)
      ordered.push(best!)
      lastPlayedAt.set(best!.keyTeam1, slot)
      lastPlayedAt.set(best!.keyTeam2, slot)
      slot++
    }
  }

  return ordered
}

/** Alterna le partite dei vari gironi una a una. Porta `interleaveOneByOne`. */
export function interleaveOneByOne(perBracket: PlannedMatch[][]): PlannedMatch[] {
  const out: PlannedMatch[] = []
  const cursors = perBracket.map(() => 0)

  let remaining = perBracket.reduce((sum, list) => sum + list.length, 0)
  let bracket = 0

  while (remaining > 0) {
    const list = perBracket[bracket]!
    const cursor = cursors[bracket]!
    if (cursor < list.length) {
      out.push(list[cursor]!)
      cursors[bracket] = cursor + 1
      remaining--
    }
    bracket = (bracket + 1) % perBracket.length
  }

  return out
}

// ---------------------------------------------------------------------------
// Assegnazione di giornata e orario
// ---------------------------------------------------------------------------

/**
 * Assegna giornata e orario alle partite riempiendo una fascia alla volta.
 *
 * Una partita entra nella fascia corrente solo se ci sta per intero; altrimenti
 * si passa alla fascia successiva.
 *
 * ⚠️ Differenza voluta rispetto al Java: `generateItalianBracket` esauriva le
 * fasce in silenzio e assegnava a tutte le partite rimanenti lo stesso orario.
 * Qui, come già faceva `generateMultipleBracket`, viene sollevato un errore.
 */
export function assignSlots(
  matches: PlannedMatch[],
  slots: TimeSlot[],
  minutesPerMatch: number,
): Omit<Match, 'key'>[] {
  if (matches.length === 0) return []
  if (slots.length === 0) throw new NotEnoughTimeError(0, matches.length)
  if (minutesPerMatch <= 0) throw new RangeError('La durata di una partita deve essere positiva.')

  let slotIndex = 0
  let cursor = readSlot(slots, slotIndex)
  const scheduled: Omit<Match, 'key'>[] = []

  for (const match of matches) {
    // Se la partita non entra nella fascia corrente, cerca la prossima utile.
    while (cursor.start + minutesPerMatch > cursor.end) {
      slotIndex++
      if (slotIndex >= slots.length) {
        throw new NotEnoughTimeError(scheduled.length, matches.length)
      }
      cursor = readSlot(slots, slotIndex)
    }

    scheduled.push({ ...match, day: cursor.day, time: formatTime(cursor.start) })
    cursor.start += minutesPerMatch
  }

  return scheduled
}

function readSlot(
  slots: TimeSlot[],
  index: number,
): { day: number; start: number; end: number } {
  const slot = slots[index]!
  const start = parseTime(slot.start)
  const end = parseTime(slot.end)

  if (start === null) throw new RangeError(`Orario di inizio non valido: "${slot.start}"`)
  if (end === null) throw new RangeError(`Orario di fine non valido: "${slot.end}"`)

  return { day: slot.day, start, end }
}

// ---------------------------------------------------------------------------
// Generazione completa
// ---------------------------------------------------------------------------

/**
 * Girone all'italiana: tutte le squadre nel girone "A", tutte contro tutte.
 * Porta `generateItalianBracket` senza la parte di scrittura su Firebase.
 */
export function generateItalianBracket(
  teams: Team[],
  slots: TimeSlot[],
  minutesPerMatch: number,
): Schedule {
  const rounds = buildRoundRobinRounds(teams, ITALIAN_BRACKET_LABEL)
  const ordered = orderRoundsBySpacing(rounds)

  const bracketByTeam = new Map<string, string>()
  for (const team of teams) {
    bracketByTeam.set(team.key, 'A')
  }

  return {
    matches: assignSlots(ordered, slots, minutesPerMatch),
    bracketByTeam,
    bracketCount: 1,
  }
}

/**
 * Gironi multipli: le squadre distribuite a rotazione su `bracketCount` gironi,
 * con le partite dei gironi alternate una a una.
 * Porta `generateMultipleBracket` senza la parte di scrittura su Firebase.
 */
export function generateMultipleBrackets(
  teams: Team[],
  slots: TimeSlot[],
  minutesPerMatch: number,
  bracketCount: number,
): Schedule {
  if (bracketCount < 1) throw new RangeError('Serve almeno un girone.')

  const buckets: Team[][] = Array.from({ length: bracketCount }, () => [])
  const bracketByTeam = new Map<string, string>()

  teams.forEach((team, i) => {
    const b = i % bracketCount
    buckets[b]!.push(team)
    bracketByTeam.set(team.key, getBracketLetter(b))
  })

  // A differenza del girone all'italiana qui i turni non vengono riordinati:
  // ci pensa già l'alternanza fra gironi a distanziare gli impegni.
  const perBracket = buckets.map((bucket, b) =>
    buildRoundRobinRounds(bucket, getBracketLabel(b)).flat(),
  )

  return {
    matches: assignSlots(interleaveOneByOne(perBracket), slots, minutesPerMatch),
    bracketByTeam,
    bracketCount,
  }
}

// ---------------------------------------------------------------------------
// Conteggi, per validare l'input prima di generare
// ---------------------------------------------------------------------------

/** Partite di un girone all'italiana: n·(n-1)/2. Porta `nMatchesItalianBracket`. */
export function countItalianBracketMatches(teamCount: number): number {
  return (teamCount * (teamCount - 1)) / 2
}

/** Partite complessive con più gironi. Porta `nMatchesMultipleBracket`. */
export function countMultipleBracketMatches(teamCount: number, bracketCount: number): number {
  let total = 0
  for (let b = 0; b < bracketCount; b++) {
    // Squadre che finiscono in questo girone distribuendo a rotazione.
    const size = Math.floor((teamCount - b + bracketCount - 1) / bracketCount)
    total += (size * (size - 1)) / 2
  }
  return total
}
