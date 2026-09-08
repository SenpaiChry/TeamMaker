import { describe, expect, it } from 'vitest'
import type { Tournament } from './models'
import {
  availableFinalSizes,
  generateFinalStages,
  isPendingRef,
  pendingRefIndex,
  PENDING_REF_PREFIX,
  resolveAllFinalStages,
  resolveSlot,
  slotLabel,
  SOURCE_GROUP_STANDING,
  SOURCE_LOSER,
  SOURCE_STANDING,
  SOURCE_WINNER,
  TO_DO_KEY,
} from './finalStages'
import { FINAL, QUARTER, SEMIFINAL, THIRD } from './phases'
import { makeMatch, makeTeam } from './testing'

function makeTournament(overrides: Partial<Tournament> = {}): Tournament {
  return {
    key: 'tour1',
    name: 'Test',
    nBracket: 1,
    date: null,
    isValid: true,
    teams: [],
    matches: [],
    ...overrides,
  }
}

describe('availableFinalSizes', () => {
  it('senza gironi propone tutte le dimensioni ≤ numero squadre', () => {
    const t = makeTournament({
      teams: Array.from({ length: 8 }, (_, i) => makeTeam(`t${i + 1}`, [], 'A')),
    })
    expect(availableFinalSizes(t)).toEqual([8, 4, 2])
  })

  it('con 3 squadre solo la finale è possibile', () => {
    const t = makeTournament({
      teams: [makeTeam('t1'), makeTeam('t2'), makeTeam('t3')],
    })
    expect(availableFinalSizes(t)).toEqual([2])
  })

  it('con gironi richiede size multiplo del numero gironi', () => {
    // 2 gironi da 3 → posso fare semifinali (4) e finale (2), non quarti (8)
    const t = makeTournament({
      nBracket: 2,
      teams: [
        makeTeam('t1', [], 'A'),
        makeTeam('t2', [], 'A'),
        makeTeam('t3', [], 'A'),
        makeTeam('t4', [], 'B'),
        makeTeam('t5', [], 'B'),
        makeTeam('t6', [], 'B'),
      ],
    })
    expect(availableFinalSizes(t)).toEqual([4, 2])
  })
})

describe('generateFinalStages', () => {
  const eightTeams = makeTournament({
    teams: Array.from({ length: 8 }, (_, i) => makeTeam(`t${i + 1}`, [], 'A')),
  })

  it('con quarti crea 4 quarti + 2 semi + 1 finale = 7 partite', () => {
    const matches = generateFinalStages(eightTeams, 8, false)
    const quarters = matches.filter((m) => m.type === QUARTER)
    const semis = matches.filter((m) => m.type === SEMIFINAL)
    const finals = matches.filter((m) => m.type === FINAL)
    expect(quarters).toHaveLength(4)
    expect(semis).toHaveLength(2)
    expect(finals).toHaveLength(1)
  })

  it("con 3°/4° posto aggiunge una partita in più", () => {
    expect(generateFinalStages(eightTeams, 8, false)).toHaveLength(7)
    expect(generateFinalStages(eightTeams, 8, true)).toHaveLength(8)
  })

  it('i quarti usano seed STANDING con ordine 1-8, 4-5, 2-7, 3-6', () => {
    const quarters = generateFinalStages(eightTeams, 8, false).filter((m) => m.type === QUARTER)
    expect(quarters[0]!.source1Ref).toBe('1')
    expect(quarters[0]!.source2Ref).toBe('8')
    expect(quarters[1]!.source1Ref).toBe('4')
    expect(quarters[1]!.source2Ref).toBe('5')
    expect(quarters[2]!.source1Ref).toBe('2')
    expect(quarters[2]!.source2Ref).toBe('7')
    expect(quarters[3]!.source1Ref).toBe('3')
    expect(quarters[3]!.source2Ref).toBe('6')
  })

  it('le semifinali puntano ai vincenti dei quarti con ref pendenti', () => {
    const matches = generateFinalStages(eightTeams, 8, false)
    const semis = matches.filter((m) => m.type === SEMIFINAL)

    expect(semis[0]!.source1Type).toBe(SOURCE_WINNER)
    expect(isPendingRef(semis[0]!.source1Ref)).toBe(true)
    expect(pendingRefIndex(semis[0]!.source1Ref)).toBe(0) // primo quarto
    expect(pendingRefIndex(semis[0]!.source2Ref)).toBe(1) // secondo quarto
    expect(pendingRefIndex(semis[1]!.source1Ref)).toBe(2)
    expect(pendingRefIndex(semis[1]!.source2Ref)).toBe(3)
  })

  it('la finale 3°/4° dopo le semi usa i LOSER delle semi', () => {
    const matches = generateFinalStages(eightTeams, 8, true)
    const third = matches.find((m) => m.type === THIRD)!
    expect(third.source1Type).toBe(SOURCE_LOSER)
    expect(third.source2Type).toBe(SOURCE_LOSER)
  })

  it('la finale 3°/4° partendo dalla finale usa la 3ª e 4ª di classifica', () => {
    // 4 squadre, size = 2 (solo finale)
    const t = makeTournament({
      teams: Array.from({ length: 4 }, (_, i) => makeTeam(`t${i + 1}`, [], 'A')),
    })
    const matches = generateFinalStages(t, 2, true)
    const third = matches.find((m) => m.type === THIRD)!
    expect(third.source1Type).toBe(SOURCE_STANDING)
    expect(third.source1Ref).toBe('3')
    expect(third.source2Type).toBe(SOURCE_STANDING)
    expect(third.source2Ref).toBe('4')
  })

  it('con gironi usa GROUP_STANDING con incrocio delle teste di serie', () => {
    const withGroups = makeTournament({
      nBracket: 2,
      teams: [
        makeTeam('a1', [], 'A'),
        makeTeam('a2', [], 'A'),
        makeTeam('b1', [], 'B'),
        makeTeam('b2', [], 'B'),
      ],
    })
    // Size 4 con 2 gironi: seed(g=0,p=0)=1 → A1, seed(g=1,p=0)=2 → B1,
    // seed(g=0,p=1)=3 → A2, seed(g=1,p=1)=4 → B2
    // Ordine tabellone 4: 1, 4, 2, 3 → A1 vs B2, B1 vs A2
    const semis = generateFinalStages(withGroups, 4, false).filter((m) => m.type === SEMIFINAL)
    expect(semis[0]!.source1Type).toBe(SOURCE_GROUP_STANDING)
    expect(semis[0]!.source1Ref).toBe('A1')
    expect(semis[0]!.source2Ref).toBe('B2')
    expect(semis[1]!.source1Ref).toBe('B1')
    expect(semis[1]!.source2Ref).toBe('A2')
  })
})

describe('resolveSlot', () => {
  it('WINNER: nessun esito → null', () => {
    const t = makeTournament({
      matches: [makeMatch({ key: 'q1', keyTeam1: 't1', keyTeam2: 't2', points1: 0, points2: 0 })],
    })
    expect(resolveSlot(t, SOURCE_WINNER, 'q1')).toBeNull()
  })

  it('WINNER: con esito ritorna la key del vincitore', () => {
    const t = makeTournament({
      matches: [makeMatch({ key: 'q1', keyTeam1: 't1', keyTeam2: 't2', points1: 2, points2: 1 })],
    })
    expect(resolveSlot(t, SOURCE_WINNER, 'q1')).toBe('t1')
    expect(resolveSlot(t, SOURCE_LOSER, 'q1')).toBe('t2')
  })

  it('STANDING: fase iniziale non completa → null', () => {
    const t = makeTournament({
      teams: [makeTeam('t1', [], 'A'), makeTeam('t2', [], 'A')],
      matches: [
        makeMatch({ key: 'g1', keyTeam1: 't1', keyTeam2: 't2', points1: 0, points2: 0 }),
      ],
    })
    expect(resolveSlot(t, SOURCE_STANDING, '1')).toBeNull()
  })

  it('STANDING: fase completa → posizione ritorna la squadra', () => {
    const t = makeTournament({
      teams: [makeTeam('t1', [], 'A'), makeTeam('t2', [], 'A')],
      matches: [
        makeMatch({ key: 'g1', keyTeam1: 't1', keyTeam2: 't2', points1: 15, points2: 8 }),
      ],
    })
    expect(resolveSlot(t, SOURCE_STANDING, '1')).toBe('t1')
    expect(resolveSlot(t, SOURCE_STANDING, '2')).toBe('t2')
  })
})

describe('resolveAllFinalStages', () => {
  it('propaga WINNER lungo la catena quarti → semi → finale', () => {
    // Costruiamo 2 quarti finiti, la semi WINNER dei due
    const t = makeTournament({
      teams: [
        makeTeam('t1'),
        makeTeam('t2'),
        makeTeam('t3'),
        makeTeam('t4'),
      ],
      matches: [
        makeMatch({ key: 'q1', keyTeam1: 't1', keyTeam2: 't2', points1: 2, points2: 0, type: QUARTER }),
        makeMatch({ key: 'q2', keyTeam1: 't3', keyTeam2: 't4', points1: 0, points2: 2, type: QUARTER }),
        makeMatch({
          key: 's1',
          keyTeam1: TO_DO_KEY,
          keyTeam2: TO_DO_KEY,
          type: SEMIFINAL,
          source1Type: SOURCE_WINNER,
          source1Ref: 'q1',
          source2Type: SOURCE_WINNER,
          source2Ref: 'q2',
        }),
      ],
    })

    const { updates } = resolveAllFinalStages(t)
    expect(updates.get('s1')).toEqual({ keyTeam1: 't1', keyTeam2: 't4' })
  })

  it("non tocca partite senza sorgenti", () => {
    const t = makeTournament({
      matches: [makeMatch({ keyTeam1: 't1', keyTeam2: 't2' })],
    })
    expect(resolveAllFinalStages(t).updates.size).toBe(0)
  })
})

describe('slotLabel', () => {
  it('mostra il nome della squadra se conosciuta', () => {
    const t = makeTournament({ teams: [makeTeam('t1')] })
    const m = makeMatch({ keyTeam1: 't1', keyTeam2: 't1' })
    expect(slotLabel(t, m, 1, (k) => `TEAM ${k}`)).toBe('TEAM t1')
  })

  it('mostra "1ª GIR. A" per un GROUP_STANDING non ancora risolto', () => {
    const t = makeTournament({ teams: [makeTeam('t1', [], 'A')] })
    const m = makeMatch({
      keyTeam1: TO_DO_KEY,
      source1Type: SOURCE_GROUP_STANDING,
      source1Ref: 'A1',
    })
    expect(slotLabel(t, m, 1, () => '')).toBe('1ª GIR. A')
  })

  it('mostra "VINC. Q1" per un WINNER di un quarto', () => {
    const t = makeTournament({
      matches: [
        makeMatch({ key: 'q1', day: 1, time: '9:00', type: QUARTER }),
        makeMatch({ key: 'q2', day: 1, time: '9:30', type: QUARTER }),
      ],
    })
    const m = makeMatch({
      keyTeam1: TO_DO_KEY,
      source1Type: SOURCE_WINNER,
      source1Ref: 'q1',
    })
    expect(slotLabel(t, m, 1, () => '')).toBe('VINC. Q1')
  })
})

describe('PENDING_REF_PREFIX', () => {
  it('è "pending:"', () => {
    expect(PENDING_REF_PREFIX).toBe('pending:')
  })
})
