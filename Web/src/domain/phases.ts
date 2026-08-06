import { getBracketLabel, ITALIAN_BRACKET_LABEL } from './scheduler'

/**
 * Fasi assegnabili a una partita, portate da TournamentActivityEditMatch.
 *
 * Le fasi a eliminazione diretta non contano per la classifica, perché il loro
 * nome non contiene né GIRONE né BRACKET: è il filtro di `countsForStandings`.
 * È il motivo per cui nel torneo reale semifinali e finali restano fuori.
 */

export const KNOCKOUT_PHASES = ['FINALE', 'FINALINA', 'SEMIFINALE', 'QUARTI'] as const

/**
 * Fasi proponibili per un torneo, in base a quanti gironi ha.
 *
 * Differenza voluta rispetto al Java: con un solo girone l'app Android
 * proponeva l'etichetta "BRACKET ", mentre il generatore scrive "GIRONE ".
 * Entrambe contano per la classifica, ma proporre quella che il generatore usa
 * davvero evita di ritrovarsi due nomi diversi per la stessa fase.
 */
export function availablePhases(bracketCount: number): string[] {
  const phases: string[] = [...KNOCKOUT_PHASES]

  if (bracketCount > 1) {
    for (let i = 0; i < bracketCount; i++) phases.push(getBracketLabel(i))
  } else {
    phases.push(ITALIAN_BRACKET_LABEL)
  }

  return phases
}

/** Etichetta leggibile per una fase, senza gli spazi di riempimento. */
export function formatPhase(phase: string): string {
  const trimmed = phase.trim()
  return trimmed.length > 0 ? trimmed : '—'
}
