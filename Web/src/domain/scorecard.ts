/**
 * Stato del segnapunti, portato da ActivityScorecard.
 *
 * ⚠️ Il punto delicato: `points1`/`sets1` appartengono SEMPRE alla squadra
 * `keyTeam1` della partita e `points2`/`sets2` a `keyTeam2`, indipendentemente
 * da come sono disposte a schermo. Lo swap cambia solo il LATO su cui una
 * squadra è mostrata, mai a chi appartiene il punteggio: è ciò che garantisce
 * che il risultato salvato resti corretto anche dopo aver invertito i lati.
 *
 * Il "lato" è quindi una nozione puramente visiva: 1 = sinistra/sopra,
 * 2 = destra/sotto.
 */

export interface ScoreState {
  points1: number
  points2: number
  sets1: number
  sets2: number
  /** Se true i due lati sono invertiti a schermo. */
  swapped: boolean
}

export type Side = 1 | 2
export type TeamSlot = 1 | 2

export const INITIAL_SCORE: ScoreState = {
  points1: 0,
  points2: 0,
  sets1: 0,
  sets2: 0,
  swapped: false,
}

/** Quale squadra logica è mostrata su un dato lato dello schermo. */
export function teamOnSide(state: ScoreState, side: Side): TeamSlot {
  if (side === 1) return state.swapped ? 2 : 1
  return state.swapped ? 1 : 2
}

/** Punti da mostrare su un lato. */
export function pointsOnSide(state: ScoreState, side: Side): number {
  return teamOnSide(state, side) === 1 ? state.points1 : state.points2
}

/** Set da mostrare su un lato. */
export function setsOnSide(state: ScoreState, side: Side): number {
  return teamOnSide(state, side) === 1 ? state.sets1 : state.sets2
}

/**
 * Aggiunge (o toglie) un punto al lato indicato.
 * Il punteggio non scende sotto zero, come nell'app Android.
 */
export function changePoints(state: ScoreState, side: Side, delta: number): ScoreState {
  const team = teamOnSide(state, side)

  if (team === 1) {
    if (delta < 0 && state.points1 === 0) return state
    return { ...state, points1: state.points1 + delta }
  }
  if (delta < 0 && state.points2 === 0) return state
  return { ...state, points2: state.points2 + delta }
}

/**
 * Aggiunge (o toglie) un set al lato indicato.
 * Cambiando set i punti si azzerano per entrambe le squadre.
 */
export function changeSets(state: ScoreState, side: Side, delta: number): ScoreState {
  const team = teamOnSide(state, side)

  if (team === 1) {
    if (delta < 0 && state.sets1 === 0) return state
    return { ...state, sets1: state.sets1 + delta, points1: 0, points2: 0 }
  }
  if (delta < 0 && state.sets2 === 0) return state
  return { ...state, sets2: state.sets2 + delta, points1: 0, points2: 0 }
}

/** Inverte i lati a schermo, senza toccare l'appartenenza dei punteggi. */
export function swapSides(state: ScoreState): ScoreState {
  return { ...state, swapped: !state.swapped }
}

/**
 * Risultato da salvare sulla partita, come coppia [squadra 1, squadra 2].
 *
 * Se è stato vinto almeno un set il risultato è il conteggio dei SET, altrimenti
 * quello dei PUNTI. È il motivo per cui in una finale si legge "2 – 1" mentre in
 * una partita di girone si legge "15 – 10".
 */
export function resultToSave(state: ScoreState): [number, number] {
  if (state.sets1 !== 0 || state.sets2 !== 0) return [state.sets1, state.sets2]
  return [state.points1, state.points2]
}
