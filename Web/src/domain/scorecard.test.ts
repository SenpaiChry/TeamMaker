import { describe, expect, it } from 'vitest'
import {
  changePoints,
  changeSets,
  INITIAL_SCORE,
  pointsOnSide,
  resultToSave,
  setsOnSide,
  swapSides,
  teamOnSide,
  type ScoreState,
} from './scorecard'

function score(overrides: Partial<ScoreState> = {}): ScoreState {
  return { ...INITIAL_SCORE, ...overrides }
}

describe('teamOnSide', () => {
  it('senza swap il lato coincide con la squadra', () => {
    const s = score()
    expect(teamOnSide(s, 1)).toBe(1)
    expect(teamOnSide(s, 2)).toBe(2)
  })

  it('con lo swap i lati si invertono', () => {
    const s = score({ swapped: true })
    expect(teamOnSide(s, 1)).toBe(2)
    expect(teamOnSide(s, 2)).toBe(1)
  })
})

describe('changePoints', () => {
  it('aggiunge un punto alla squadra mostrata su quel lato', () => {
    expect(changePoints(score(), 1, 1).points1).toBe(1)
    expect(changePoints(score(), 2, 1).points2).toBe(1)
  })

  it('con lo swap il punto va comunque alla squadra giusta', () => {
    // Lato 1 mostra la squadra 2: il punto deve finire su points2.
    const s = changePoints(score({ swapped: true }), 1, 1)
    expect(s.points2).toBe(1)
    expect(s.points1).toBe(0)
  })

  it('toglie un punto', () => {
    expect(changePoints(score({ points1: 5 }), 1, -1).points1).toBe(4)
  })

  it('non scende sotto zero', () => {
    const s = score()
    expect(changePoints(s, 1, -1)).toBe(s)
    expect(changePoints(s, 2, -1)).toBe(s)
  })

  it('non tocca il punteggio dell’altra squadra', () => {
    const s = changePoints(score({ points2: 7 }), 1, 1)
    expect(s.points2).toBe(7)
  })
})

describe('changeSets', () => {
  it('aggiunge un set alla squadra mostrata su quel lato', () => {
    expect(changeSets(score(), 1, 1).sets1).toBe(1)
  })

  it('con lo swap il set va alla squadra giusta', () => {
    const s = changeSets(score({ swapped: true }), 1, 1)
    expect(s.sets2).toBe(1)
    expect(s.sets1).toBe(0)
  })

  it('azzera i punti di ENTRAMBE le squadre', () => {
    const s = changeSets(score({ points1: 25, points2: 23 }), 1, 1)
    expect(s.points1).toBe(0)
    expect(s.points2).toBe(0)
  })

  it('registra i punti del set corrente nel detail (chiusura set)', () => {
    const s = changeSets(score({ points1: 25, points2: 23 }), 1, 1)
    expect(s.detail).toEqual([[25, 23]])
  })

  it('non registra un set senza punti (nessun tocco)', () => {
    const s = changeSets(score(), 1, 1)
    expect(s.detail).toEqual([])
  })

  it("l'annullo del set (-1) ripristina i punti dell'ultimo set", () => {
    // Chiudo un set 25-20 poi lo annullo: sets torna a 0 e punti tornano 25-20
    let s = changeSets(score({ points1: 25, points2: 20 }), 1, 1)
    expect(s).toMatchObject({ sets1: 1, points1: 0, points2: 0, detail: [[25, 20]] })

    s = changeSets(s, 1, -1)
    expect(s).toMatchObject({ sets1: 0, points1: 25, points2: 20, detail: [] })
  })

  it("l'annullo senza detail lascia i punti a zero", () => {
    // Sets contati "a mano" senza chiusura di un set con punti: -1 non ripristina nulla
    const s = changeSets(score({ sets1: 1 }), 1, -1)
    expect(s).toMatchObject({ sets1: 0, points1: 0, points2: 0, detail: [] })
  })

  it('non scende sotto zero', () => {
    const s = score()
    expect(changeSets(s, 1, -1)).toBe(s)
  })
})

describe('swapSides', () => {
  it('inverte solo la visualizzazione, non i punteggi', () => {
    const before = score({ points1: 15, points2: 10, sets1: 1 })
    const after = swapSides(before)

    expect(after.points1).toBe(15)
    expect(after.points2).toBe(10)
    expect(after.sets1).toBe(1)
    expect(after.swapped).toBe(true)
  })

  it('dopo lo swap i punti mostrati sui lati si scambiano', () => {
    const before = score({ points1: 15, points2: 10 })
    expect(pointsOnSide(before, 1)).toBe(15)

    const after = swapSides(before)
    expect(pointsOnSide(after, 1)).toBe(10)
    expect(pointsOnSide(after, 2)).toBe(15)
  })

  it('vale anche per i set', () => {
    const after = swapSides(score({ sets1: 2, sets2: 1 }))
    expect(setsOnSide(after, 1)).toBe(1)
    expect(setsOnSide(after, 2)).toBe(2)
  })
})

describe('resultToSave', () => {
  it('senza set salva i punti (formato legacy set unico)', () => {
    expect(resultToSave(score({ points1: 15, points2: 10 }))).toEqual({
      points1: 15,
      points2: 10,
      detail: [],
    })
  })

  it('senza punti pendenti salva sets1/sets2 e il detail dei set giocati', () => {
    // 2-1: primo set 25-20, secondo 22-25, terzo 15-10 (già chiuso a mano)
    const s = score({
      sets1: 2,
      sets2: 1,
      detail: [
        [25, 20],
        [22, 25],
        [15, 10],
      ],
    })
    expect(resultToSave(s)).toEqual({
      points1: 2,
      points2: 1,
      detail: [
        [25, 20],
        [22, 25],
        [15, 10],
      ],
    })
  })

  it('finalizza il set in corso al salvataggio (come fa Android)', () => {
    // sets 1-1, terzo set in corso 15-10 → chiusura → 2-1 con detail
    const s = score({
      points1: 15,
      points2: 10,
      sets1: 1,
      sets2: 1,
      detail: [
        [25, 20],
        [22, 25],
      ],
    })
    expect(resultToSave(s)).toEqual({
      points1: 2,
      points2: 1,
      detail: [
        [25, 20],
        [22, 25],
        [15, 10],
      ],
    })
  })

  it('finalizza il set in corso incrementando il vincitore', () => {
    // set unico chiuso al momento del save: 25-20 con sets ancora 0-0
    const s = score({ points1: 25, points2: 20, detail: [[25, 20]] })
    // In realtà normalmente al momento del save i sets sarebbero già stati incrementati;
    // testiamo il caso a set unico dove l'utente non ha premuto +set: partiamo con sets=0
    const single = score({ points1: 25, points2: 20, sets1: 0, sets2: 0, detail: [] })
    expect(resultToSave(single)).toEqual({ points1: 25, points2: 20, detail: [] })

    // Se invece detail contiene già un set concluso e ci sono nuovi punti, si finalizza:
    const withOpenSet = score({
      points1: 25,
      points2: 20,
      sets1: 0,
      sets2: 0,
      detail: [[25, 23]],
    })
    expect(resultToSave(withOpenSet)).toEqual({
      points1: 1,
      points2: 0,
      detail: [
        [25, 23],
        [25, 20],
      ],
    })
    void s
  })

  it('a partita non iniziata salva 0 a 0 senza detail', () => {
    expect(resultToSave(score())).toEqual({ points1: 0, points2: 0, detail: [] })
  })
})

describe('lo swap non falsa mai il risultato salvato', () => {
  it('stessa sequenza di tocchi, con e senza swap, produce lo stesso salvataggio', () => {
    // Senza swap: 3 punti al lato 1, 1 al lato 2.
    let plain = score()
    plain = changePoints(plain, 1, 1)
    plain = changePoints(plain, 1, 1)
    plain = changePoints(plain, 1, 1)
    plain = changePoints(plain, 2, 1)

    // Con swap: gli stessi punti vanno toccati sui lati opposti.
    let swappedRun = swapSides(score())
    swappedRun = changePoints(swappedRun, 2, 1)
    swappedRun = changePoints(swappedRun, 2, 1)
    swappedRun = changePoints(swappedRun, 2, 1)
    swappedRun = changePoints(swappedRun, 1, 1)

    expect(resultToSave(plain)).toEqual({ points1: 3, points2: 1, detail: [] })
    expect(resultToSave(swappedRun)).toEqual({ points1: 3, points2: 1, detail: [] })
  })

  it('lo swap a metà partita non sposta i punti già segnati', () => {
    let s = score()
    s = changePoints(s, 1, 1) // squadra 1 → 1
    s = swapSides(s) // ora la squadra 1 è sul lato 2
    s = changePoints(s, 2, 1) // ancora squadra 1 → 2

    expect(resultToSave(s)).toEqual({ points1: 2, points2: 0, detail: [] })
  })
})
