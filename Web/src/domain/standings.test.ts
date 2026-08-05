import { describe, expect, it } from 'vitest'
import type { Match, Tournament } from './models'
import { computePoints, computeStandings, pointsForMatch } from './standings'
import { makeTeam } from './testing'

function makeMatch(overrides: Partial<Match> = {}): Match {
  return {
    key: 'm1',
    keyTeam1: 't1',
    keyTeam2: 't2',
    day: 1,
    time: '9:00',
    points1: 0,
    points2: 0,
    type: 'GIRONE ',
    ...overrides,
  }
}

function makeTournament(matches: Match[], teamKeys = ['t1', 't2', 't3']): Tournament {
  return {
    key: 'tour1',
    name: 'Test',
    nBracket: 1,
    date: null,
    isValid: true,
    teams: teamKeys.map((k) => makeTeam(k, [], 'A')),
    matches,
  }
}

describe('pointsForMatch', () => {
  it('assegna 3 punti alla vittoria netta a 15', () => {
    expect(pointsForMatch(makeMatch({ points1: 15, points2: 9 }))).toEqual([3, 0])
  })

  it('assegna 3 punti alla vittoria netta a 25', () => {
    expect(pointsForMatch(makeMatch({ points1: 25, points2: 20 }))).toEqual([3, 0])
  })

  it('assegna 2 e 1 alla vittoria ai vantaggi', () => {
    expect(pointsForMatch(makeMatch({ points1: 17, points2: 15 }))).toEqual([2, 1])
    expect(pointsForMatch(makeMatch({ points1: 20, points2: 22 }))).toEqual([1, 2])
  })

  it('funziona simmetricamente per la seconda squadra', () => {
    expect(pointsForMatch(makeMatch({ points1: 9, points2: 15 }))).toEqual([0, 3])
  })

  it('non assegna punti a una partita non giocata o pari', () => {
    expect(pointsForMatch(makeMatch({ points1: 0, points2: 0 }))).toEqual([0, 0])
    expect(pointsForMatch(makeMatch({ points1: 12, points2: 12 }))).toEqual([0, 0])
  })

  it('ignora le partite che non sono di girone', () => {
    expect(pointsForMatch(makeMatch({ points1: 15, points2: 9, type: 'FINALE' }))).toEqual([0, 0])
    expect(pointsForMatch(makeMatch({ points1: 15, points2: 9, type: '' }))).toEqual([0, 0])
  })

  it('conta sia le partite GIRONE sia quelle BRACKET', () => {
    expect(pointsForMatch(makeMatch({ points1: 15, points2: 9, type: 'BRACKET A' }))).toEqual([3, 0])
  })
})

describe('computePoints', () => {
  it('parte da zero per tutte le squadre, anche senza partite', () => {
    const points = computePoints(makeTournament([]))
    expect([...points.values()]).toEqual([0, 0, 0])
  })

  it('somma i punti di più partite', () => {
    const points = computePoints(
      makeTournament([
        makeMatch({ key: 'm1', keyTeam1: 't1', keyTeam2: 't2', points1: 15, points2: 8 }),
        makeMatch({ key: 'm2', keyTeam1: 't1', keyTeam2: 't3', points1: 17, points2: 15 }),
        makeMatch({ key: 'm3', keyTeam1: 't2', keyTeam2: 't3', points1: 10, points2: 15 }),
      ]),
    )

    expect(points.get('t1')).toBe(5) // 3 + 2
    expect(points.get('t2')).toBe(0) // 0 + 0
    expect(points.get('t3')).toBe(4) // 1 + 3
  })

  it('ignora le partite che citano una squadra non più nel torneo', () => {
    // L'app Android in questo caso indicizzava con -1 e crashava.
    const tournament = makeTournament(
      [makeMatch({ keyTeam1: 't1', keyTeam2: 'sparita', points1: 15, points2: 3 })],
      ['t1', 't2'],
    )

    expect(() => computePoints(tournament)).not.toThrow()
    expect(computePoints(tournament).get('t1')).toBe(0)
  })
})

describe('computeStandings', () => {
  it('ordina per punti decrescenti e numera le posizioni', () => {
    const standings = computeStandings(
      makeTournament([
        makeMatch({ key: 'm1', keyTeam1: 't1', keyTeam2: 't2', points1: 15, points2: 8 }),
        makeMatch({ key: 'm2', keyTeam1: 't3', keyTeam2: 't2', points1: 17, points2: 15 }),
      ]),
    )

    expect(standings).toHaveLength(1)
    expect(standings[0]!.rows.map((r) => [r.position, r.team.key, r.points])).toEqual([
      [1, 't1', 3],
      [2, 't3', 2],
      [3, 't2', 1],
    ])
  })

  it('raggruppa per girone e numera le posizioni da capo in ognuno', () => {
    const tournament = makeTournament([], [])
    tournament.teams = [
      makeTeam('t1', [], 'A'),
      makeTeam('t2', [], 'B'),
      makeTeam('t3', [], 'A'),
      makeTeam('t4', [], 'B'),
    ]

    const standings = computeStandings(tournament)

    expect(standings.map((g) => g.bracket)).toEqual(['A', 'B'])
    expect(standings.every((g) => g.rows.map((r) => r.position).join() === '1,2')).toBe(true)
  })

  it('include anche le squadre che non hanno ancora giocato', () => {
    const standings = computeStandings(makeTournament([]))
    expect(standings[0]!.rows).toHaveLength(3)
  })
})
