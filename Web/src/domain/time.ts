/**
 * Gestione degli orari, portata da TimeUtility.java.
 *
 * ⚠️ Il database salva gli orari come "9:30": l'ora NON è zero-padded, i minuti
 * sì. L'app Android in alcuni punti ordina le partite confrontando questa
 * stringa direttamente, e così "9:30" finisce dopo "10:00". Qui gli orari si
 * confrontano SEMPRE convertendoli in minuti; `formatTime` riproduce però il
 * formato originale, perché è quello che l'app Android si aspetta di leggere.
 */

/** Minuti dalla mezzanotte, oppure `null` se la stringa non è un orario valido. */
export function parseTime(time: string): number | null {
  const parts = time.split(':')
  if (parts.length !== 2) return null

  const hours = Number(parts[0])
  const minutes = Number(parts[1])

  if (!Number.isInteger(hours) || !Number.isInteger(minutes)) return null
  if (hours < 0 || hours > 24) return null
  if (minutes < 0 || minutes > 59) return null

  return hours * 60 + minutes
}

/** Porta `TimeUtility.isValidTime`. */
export function isValidTime(time: string): boolean {
  return parseTime(time) !== null
}

/**
 * Formatta i minuti dalla mezzanotte nel formato del database: "9:30", "14:05".
 * Equivale a `String.format("%d:%02d", hours, minutes)` del Java.
 */
export function formatTime(totalMinutes: number): string {
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  return `${hours}:${String(minutes).padStart(2, '0')}`
}

/**
 * `true` se `second` viene dopo `first`.
 * Porta `TimeUtility.isAfter(time1, time2)`, che — nonostante il nome — verifica
 * che il **secondo** argomento sia successivo al primo.
 */
export function isAfter(first: string, second: string): boolean {
  const a = parseTime(first)
  const b = parseTime(second)
  if (a === null || b === null) return false
  return b > a
}

/** Confronto fra partite: prima per giornata, poi per orario crescente. */
export function compareByDayAndTime(
  a: { day: number; time: string },
  b: { day: number; time: string },
): number {
  if (a.day !== b.day) return a.day - b.day
  return (parseTime(a.time) ?? 0) - (parseTime(b.time) ?? 0)
}

/** Fascia oraria disponibile in una giornata per programmare le partite. */
export interface TimeSlot {
  day: number
  /** Orario di inizio, formato "H:mm". */
  start: string
  /** Orario di fine, formato "H:mm". */
  end: string
}

/**
 * Quante partite entrano complessivamente nelle fasce indicate.
 * Porta `TimeUtility.checkInputTime`, che sommava le capienze delle fasce.
 */
export function countAvailableSlots(slots: TimeSlot[], minutesPerMatch: number): number {
  if (minutesPerMatch <= 0) return 0

  let total = 0
  for (const slot of slots) {
    const start = parseTime(slot.start)
    const end = parseTime(slot.end)
    if (start === null || end === null || end <= start) continue
    total += Math.floor((end - start) / minutesPerMatch)
  }
  return total
}

/** `true` se le fasce indicate bastano a ospitare `matchCount` partite. */
export function hasEnoughTime(
  slots: TimeSlot[],
  minutesPerMatch: number,
  matchCount: number,
): boolean {
  return countAvailableSlots(slots, minutesPerMatch) >= matchCount
}
