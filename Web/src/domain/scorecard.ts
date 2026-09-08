/**
 * Stato del segnapunti — porta ActivityScorecard.
 *
 * ⚠️ Il punto delicato: `points1`/`sets1` appartengono SEMPRE alla squadra
 * `keyTeam1` della partita e `points2`/`sets2` a `keyTeam2`, indipendentemente
 * da come sono disposte a schermo. Lo swap cambia solo il LATO su cui una
 * squadra è mostrata, mai a chi appartiene il punteggio: è ciò che garantisce
 * che il risultato salvato resti corretto anche dopo aver invertito i lati.
 *
 * Il "lato" è quindi una nozione puramente visiva: 1 = sinistra/sopra,
 * 2 = destra/sotto.
 *
 * `detail` accumula i punti dei set già conclusi come `[puntiT1, puntiT2]`.
 * Un set viene "chiuso" premendo +set (o al salvataggio se sono rimasti punti
 * pendenti); l'annullo di un set (-set) toglie l'ultima riga e ripristina i
 * suoi punti sulla scacchiera.
 */

export interface ScoreState {
  points1: number
  points2: number
  sets1: number
  sets2: number
  /** Punti dei set completati, in ordine. */
  detail: number[][]
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
  detail: [],
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
 * Aggiunge o toglie un set al lato indicato, replicando `changeSet` dell'Android:
 *
 * - +set: se sul tabellone ci sono punti, il set viene "chiuso" registrandoli
 *   in `detail`; poi si incrementa il conteggio set del team e si azzerano i
 *   punti.
 * - −set: si decrementa il conteggio set del team (se > 0) e, se `detail` non
 *   è vuoto, si ripristinano i punti dell'ultimo set registrato (annullo).
 */
export function changeSets(state: ScoreState, side: Side, delta: number): ScoreState {
  const team = teamOnSide(state, side)

  if (delta > 0) {
    const hadPoints = state.points1 > 0 || state.points2 > 0
    const detail = hadPoints ? [...state.detail, [state.points1, state.points2]] : state.detail

    if (team === 1) {
      return { ...state, sets1: state.sets1 + 1, points1: 0, points2: 0, detail }
    }
    return { ...state, sets2: state.sets2 + 1, points1: 0, points2: 0, detail }
  }

  // delta < 0: annullo del set
  if (team === 1) {
    if (state.sets1 === 0) return state
    return popLastSet({ ...state, sets1: state.sets1 - 1 })
  }
  if (state.sets2 === 0) return state
  return popLastSet({ ...state, sets2: state.sets2 - 1 })
}

/** Ripristina i punti dell'ultimo set registrato. */
function popLastSet(state: ScoreState): ScoreState {
  if (state.detail.length === 0) return { ...state, points1: 0, points2: 0 }
  const last = state.detail[state.detail.length - 1]!
  return {
    ...state,
    points1: last[0] ?? 0,
    points2: last[1] ?? 0,
    detail: state.detail.slice(0, -1),
  }
}

/** Inverte i lati a schermo, senza toccare l'appartenenza dei punteggi. */
export function swapSides(state: ScoreState): ScoreState {
  return { ...state, swapped: !state.swapped }
}

/**
 * Risultato da salvare sulla partita.
 *
 * Se ci sono set (già conclusi o `detail` non vuoto) e a schermo sono rimasti
 * punti, quest'ultimo set "in corso" viene chiuso automaticamente — come fa
 * `saveAndOpenActivity` dell'Android quando premi salva senza aver premuto
 * +set. Il risultato salvato è allora `sets1 / sets2` con il `detail` dei set.
 *
 * Se nessun set è mai stato aperto e non c'è `detail`, resta il formato legacy
 * a set unico: si salvano direttamente i punti mostrati, senza detail.
 */
export function resultToSave(state: ScoreState): {
  points1: number
  points2: number
  detail: number[][]
} {
  const isSetMatch = state.sets1 !== 0 || state.sets2 !== 0 || state.detail.length !== 0

  if (!isSetMatch) {
    // Partita a set unico legacy: salva i punti così come sono.
    return { points1: state.points1, points2: state.points2, detail: [] }
  }

  // Formato set + detail: finalizza il set in corso se ci sono punti pendenti.
  let sets1 = state.sets1
  let sets2 = state.sets2
  const detail = [...state.detail]

  const hasPending = state.points1 > 0 || state.points2 > 0
  if (hasPending) {
    detail.push([state.points1, state.points2])
    if (state.points1 > state.points2) sets1++
    else if (state.points2 > state.points1) sets2++
  }

  return { points1: sets1, points2: sets2, detail }
}
