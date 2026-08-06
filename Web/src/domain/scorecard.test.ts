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
  it('salva i punti se nessun set è stato vinto', () => {
    expect(resultToSave(score({ points1: 15, points2: 10 }))).toEqual([15, 10])
  })

  it('salva i set appena ne è stato vinto almeno uno', () => {
    expect(resultToSave(score({ points1: 3, points2: 1, sets1: 2, sets2: 1 }))).toEqual([2, 1])
  })

  it('salva i set anche se solo la seconda squadra ne ha vinti', () => {
    expect(resultToSave(score({ points1: 20, points2: 22, sets2: 1 }))).toEqual([0, 1])
  })

  it('a partita non iniziata salva 0 a 0', () => {
    expect(resultToSave(score())).toEqual([0, 0])
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

    expect(resultToSave(plain)).toEqual([3, 1])
    expect(resultToSave(swappedRun)).toEqual([3, 1])
  })

  it('lo swap a metà partita non sposta i punti già segnati', () => {
    let s = score()
    s = changePoints(s, 1, 1) // squadra 1 → 1
    s = swapSides(s) // ora la squadra 1 è sul lato 2
    s = changePoints(s, 2, 1) // ancora squadra 1 → 2

    expect(resultToSave(s)).toEqual([2, 0])
  })
})
