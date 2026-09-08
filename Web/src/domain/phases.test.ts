import { describe, expect, it } from 'vitest'
import {
  availablePhases,
  FINAL,
  GROUP,
  GROUP_PREFIX,
  isGroup,
  label,
  normalize,
  QUARTER,
  SEMIFINAL,
  THIRD,
} from './phases'
import { countsForStandings } from './standings'

function match(type: string) {
  return {
    key: 'm',
    keyTeam1: 'a',
    keyTeam2: 'b',
    day: 1,
    time: '9:00',
    points1: 0,
    points2: 0,
    detail: [],
    type,
  }
}

describe('normalize', () => {
  it('lascia passare i codici già canonici', () => {
    expect(normalize('GROUP')).toBe('GROUP')
    expect(normalize('GROUP_A')).toBe('GROUP_A')
    expect(normalize('QUARTER')).toBe('QUARTER')
    expect(normalize('SEMIFINAL')).toBe('SEMIFINAL')
    expect(normalize('FINAL')).toBe('FINAL')
    expect(normalize('THIRD')).toBe('THIRD')
  })

  it('converte le stringhe legacy italiane', () => {
    expect(normalize('GIRONE ')).toBe('GROUP')
    expect(normalize('GIRONE A')).toBe('GROUP_A')
    expect(normalize('GIRONE')).toBe('GROUP')
    expect(normalize('BRACKET A')).toBe('GROUP_A')
    expect(normalize('BRACKET B')).toBe('GROUP_B')
    expect(normalize('BRACKET')).toBe('GROUP')
    expect(normalize('QUARTI')).toBe('QUARTER')
    expect(normalize('SEMIFINALE')).toBe('SEMIFINAL')
    expect(normalize('FINALE')).toBe('FINAL')
    expect(normalize('FINALINA')).toBe('THIRD')
  })

  it('gestisce stringhe vuote e segnaposto', () => {
    expect(normalize('')).toBe('')
    expect(normalize('   ')).toBe('')
    expect(normalize('---')).toBe('')
    expect(normalize('-')).toBe('')
    expect(normalize(null)).toBe('')
    expect(normalize(undefined)).toBe('')
  })

  it('è case-insensitive', () => {
    expect(normalize('girone a')).toBe('GROUP_A')
    expect(normalize('Finale')).toBe('FINAL')
    expect(normalize('bracket b')).toBe('GROUP_B')
  })

  it('conserva valori sconosciuti (trimmed)', () => {
    expect(normalize('CUSTOM PHASE')).toBe('CUSTOM PHASE')
  })
})

describe('isGroup', () => {
  it('riconosce GROUP e GROUP_X come fasi di girone', () => {
    expect(isGroup('GROUP')).toBe(true)
    expect(isGroup('GROUP_A')).toBe(true)
    expect(isGroup('GROUP_B')).toBe(true)
  })

  it('riconosce le stringhe legacy come fasi di girone', () => {
    expect(isGroup('GIRONE ')).toBe(true)
    expect(isGroup('BRACKET A')).toBe(true)
  })

  it('non riconosce le fasi a eliminazione diretta', () => {
    expect(isGroup('QUARTER')).toBe(false)
    expect(isGroup('SEMIFINAL')).toBe(false)
    expect(isGroup('FINAL')).toBe(false)
    expect(isGroup('THIRD')).toBe(false)
  })
})

describe('label', () => {
  it('traduce i codici canonici', () => {
    expect(label('GROUP')).toBe('GIRONE')
    expect(label('GROUP_A')).toBe('GIRONE A')
    expect(label('GROUP_B')).toBe('GIRONE B')
    expect(label('QUARTER')).toBe('QUARTI')
    expect(label('SEMIFINAL')).toBe('SEMIFINALE')
    expect(label('FINAL')).toBe('FINALE')
    expect(label('THIRD')).toBe('FINALINA')
  })

  it('traduce le stringhe legacy (via normalize)', () => {
    expect(label('GIRONE ')).toBe('GIRONE')
    expect(label('BRACKET A')).toBe('GIRONE A')
    expect(label('QUARTI')).toBe('QUARTI')
    expect(label('FINALE')).toBe('FINALE')
  })

  it('mostra un trattino per la stringa vuota', () => {
    expect(label('')).toBe('—')
    expect(label('   ')).toBe('—')
  })
})

describe('availablePhases', () => {
  it('propone sempre le fasi a eliminazione diretta come codici canonici', () => {
    expect(availablePhases(1)).toEqual(
      expect.arrayContaining([QUARTER, SEMIFINAL, FINAL, THIRD]),
    )
  })

  it('con un solo girone propone GROUP', () => {
    expect(availablePhases(1)).toContain(GROUP)
    expect(availablePhases(0)).toContain(GROUP)
  })

  it('con più gironi propone GROUP_A, GROUP_B, ecc.', () => {
    const phases = availablePhases(3)
    expect(phases).toContain(`${GROUP_PREFIX}A`)
    expect(phases).toContain(`${GROUP_PREFIX}B`)
    expect(phases).toContain(`${GROUP_PREFIX}C`)
    expect(phases).not.toContain(`${GROUP_PREFIX}D`)
  })
})

describe('coerenza con la classifica', () => {
  it('le fasi di girone contano', () => {
    for (const phase of availablePhases(2).filter((p) => p.startsWith(GROUP_PREFIX))) {
      expect(countsForStandings(match(phase))).toBe(true)
    }
    expect(countsForStandings(match(GROUP))).toBe(true)
    // Anche le stringhe legacy devono contare
    expect(countsForStandings(match('GIRONE '))).toBe(true)
    expect(countsForStandings(match('BRACKET A'))).toBe(true)
  })

  it('le fasi a eliminazione diretta non contano', () => {
    for (const phase of [QUARTER, SEMIFINAL, FINAL, THIRD]) {
      expect(countsForStandings(match(phase))).toBe(false)
    }
    // Anche le stringhe legacy delle finali non devono contare
    for (const phase of ['FINALE', 'FINALINA', 'SEMIFINALE', 'QUARTI']) {
      expect(countsForStandings(match(phase))).toBe(false)
    }
  })
})
