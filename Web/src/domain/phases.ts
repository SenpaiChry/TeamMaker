/**
 * Fasi di una partita — portate da PhaseUtility.java.
 *
 * Ogni fase è salvata come CODICE canonico indipendente dalla lingua:
 *   GROUP (girone unico), GROUP_A / GROUP_B… (gironi), QUARTER, SEMIFINAL,
 *   FINAL, THIRD.
 *
 * I dati vecchi/localizzati (GIRONE, BRACKET A, FINALE, QUARTI…) vengono
 * convertiti da `normalize()` in lettura, così il confronto funziona sempre
 * anche con i tornei creati prima di questa modifica.
 *
 * A schermo si mostra l'etichetta tradotta da `label()`.
 */

// ---------------------------------------------------------------------------
// Codici canonici
// ---------------------------------------------------------------------------

export const GROUP = 'GROUP'
export const QUARTER = 'QUARTER'
export const SEMIFINAL = 'SEMIFINAL'
export const FINAL = 'FINAL'
export const THIRD = 'THIRD'
export const GROUP_PREFIX = 'GROUP_'

/** Codice del girone per lettera: groupCode("A") → "GROUP_A". */
export function groupCode(letter: string): string {
  return GROUP_PREFIX + letter.trim().toUpperCase()
}

// ---------------------------------------------------------------------------
// Normalize — legacy → canonico
// ---------------------------------------------------------------------------

/**
 * Converte un type salvato (anche legacy/localizzato) nel codice canonico.
 *
 * Copre:
 *   - codici già canonici (GROUP, QUARTER, GROUP_A…)
 *   - gironi legacy: "BRACKET A" / "GIRONE A" / "GIRONE " / "BRACKET "
 *   - finali legacy (it/en): QUARTI, SEMIFINALE, FINALE, FINALINA
 *   - stringhe vuote / segnaposto ("---", "-")
 *
 * Se il valore non corrisponde a nessun pattern, viene restituito com'è
 * (trimmed), così non si perdono eventuali fasi custom.
 */
export function normalize(stored: string | null | undefined): string {
  if (stored === null || stored === undefined) return ''
  const t = stored.trim()
  if (t.length === 0 || t === '---' || t === '-') return ''

  const u = t.toUpperCase()

  // Già canonico
  if (u === GROUP || u === QUARTER || u === SEMIFINAL || u === FINAL || u === THIRD) return u
  if (u.startsWith(GROUP_PREFIX)) return u

  // Gironi legacy: "BRACKET A" / "GIRONE A" → GROUP_A
  if (u.startsWith('BRACKET ') || u.startsWith('GIRONE ')) {
    const letter = u.substring(u.indexOf(' ') + 1).trim()
    return letter.length === 0 ? GROUP : GROUP_PREFIX + letter
  }
  if (u === 'BRACKET' || u === 'GIRONE') return GROUP

  // Finali legacy (it/en)
  if (u === 'QUARTI' || u === 'QUARTER') return QUARTER
  if (u === 'SEMIFINALE' || u === 'SEMIFINAL') return SEMIFINAL
  if (u === 'FINALINA' || u === 'THIRD') return THIRD
  if (u === 'FINALE' || u === 'FINAL') return FINAL

  // Sconosciuto: restituisci il valore originale pulito
  return t
}

// ---------------------------------------------------------------------------
// isGroup — filtro per la classifica
// ---------------------------------------------------------------------------

/** `true` se la fase è di girone (conta per la classifica). */
export function isGroup(type: string): boolean {
  const code = normalize(type)
  return code === GROUP || code.startsWith(GROUP_PREFIX)
}

// ---------------------------------------------------------------------------
// Label — etichetta tradotta per la UI
// ---------------------------------------------------------------------------

const LABELS: Record<string, string> = {
  [GROUP]: 'GIRONE',
  [QUARTER]: 'QUARTI',
  [SEMIFINAL]: 'SEMIFINALE',
  [FINAL]: 'FINALE',
  [THIRD]: 'FINALINA',
}

/**
 * Etichetta leggibile per un type (codice canonico o legacy).
 * Restituisce "—" per la stringa vuota: in UI un trattino è meglio di un buco.
 */
export function label(type: string): string {
  const code = normalize(type)
  if (code.length === 0) return '—'

  if (code.startsWith(GROUP_PREFIX)) {
    return `GIRONE ${code.substring(GROUP_PREFIX.length)}`
  }
  return LABELS[code] ?? code
}

// ---------------------------------------------------------------------------
// availablePhases — fasi selezionabili per un torneo
// ---------------------------------------------------------------------------

/** Fasi a eliminazione diretta. */
const KNOCKOUT_CODES = [QUARTER, SEMIFINAL, FINAL, THIRD] as const

/**
 * Fasi proponibili nel select dell'editor partita, in base a quanti gironi ha
 * il torneo. I codici sono canonici; nella UI vanno mostrati con `label()`.
 */
export function availablePhases(bracketCount: number): string[] {
  const phases: string[] = [...KNOCKOUT_CODES]

  if (bracketCount > 1) {
    for (let i = 0; i < bracketCount; i++) {
      phases.push(GROUP_PREFIX + String.fromCharCode(65 + (i % 26)))
    }
  } else {
    phases.push(GROUP)
  }

  return phases
}
