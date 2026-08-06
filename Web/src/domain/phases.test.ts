import { describe, expect, it } from 'vitest'
import { availablePhases, formatPhase } from './phases'
import { countsForStandings } from './standings'

function match(type: string) {
  return { key: 'm', keyTeam1: 'a', keyTeam2: 'b', day: 1, time: '9:00', points1: 0, points2: 0, type }
}

describe('availablePhases', () => {
  it('propone sempre le fasi a eliminazione diretta', () => {
    expect(availablePhases(1)).toEqual(
      expect.arrayContaining(['FINALE', 'FINALINA', 'SEMIFINALE', 'QUARTI']),
    )
  })

  it('con un solo girone propone la fase che il generatore scrive davvero', () => {
    expect(availablePhases(1)).toContain('GIRONE ')
    expect(availablePhases(0)).toContain('GIRONE ')
  })

  it('con più gironi propone un’etichetta per ciascuno', () => {
    const phases = availablePhases(3)
    expect(phases).toContain('BRACKET A')
    expect(phases).toContain('BRACKET B')
    expect(phases).toContain('BRACKET C')
    expect(phases).not.toContain('BRACKET D')
  })
})

describe('coerenza con la classifica', () => {
  it('le fasi di girone contano', () => {
    for (const phase of availablePhases(2).filter((p) => p.includes('BRACKET'))) {
      expect(countsForStandings(match(phase))).toBe(true)
    }
    expect(countsForStandings(match('GIRONE '))).toBe(true)
  })

  it('le fasi a eliminazione diretta non contano', () => {
    for (const phase of ['FINALE', 'FINALINA', 'SEMIFINALE', 'QUARTI']) {
      expect(countsForStandings(match(phase))).toBe(false)
    }
  })
})

describe('formatPhase', () => {
  it('toglie gli spazi di riempimento', () => {
    expect(formatPhase('GIRONE ')).toBe('GIRONE')
  })

  it('mostra un trattino se la fase è vuota', () => {
    expect(formatPhase('')).toBe('—')
    expect(formatPhase('   ')).toBe('—')
  })
})
