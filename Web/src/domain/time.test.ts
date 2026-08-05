import { describe, expect, it } from 'vitest'
import {
  compareByDayAndTime,
  countAvailableSlots,
  formatTime,
  isAfter,
  isValidTime,
  parseTime,
} from './time'

describe('parseTime', () => {
  it('converte in minuti dalla mezzanotte', () => {
    expect(parseTime('9:30')).toBe(570)
    expect(parseTime('0:00')).toBe(0)
    expect(parseTime('14:05')).toBe(845)
  })

  it('accetta anche l’ora con lo zero davanti', () => {
    expect(parseTime('09:30')).toBe(570)
  })

  it('rifiuta ciò che non è un orario', () => {
    expect(parseTime('9')).toBeNull()
    expect(parseTime('9:30:00')).toBeNull()
    expect(parseTime('9:60')).toBeNull()
    expect(parseTime('25:00')).toBeNull()
    expect(parseTime('abc')).toBeNull()
    expect(parseTime('')).toBeNull()
  })
})

describe('formatTime', () => {
  it('non mette lo zero davanti all’ora, ma sì ai minuti', () => {
    // È il formato che l'app Android scrive e si aspetta di leggere.
    expect(formatTime(570)).toBe('9:30')
    expect(formatTime(845)).toBe('14:05')
    expect(formatTime(0)).toBe('0:00')
  })

  it('è l’inverso di parseTime', () => {
    for (const time of ['9:30', '14:05', '0:00', '23:59']) {
      expect(formatTime(parseTime(time)!)).toBe(time)
    }
  })
})

describe('isValidTime', () => {
  it('riconosce gli orari validi', () => {
    expect(isValidTime('9:30')).toBe(true)
    expect(isValidTime('24:00')).toBe(true)
    expect(isValidTime('24:01')).toBe(true)
    expect(isValidTime('-1:00')).toBe(false)
  })
})

describe('isAfter', () => {
  it('verifica che il secondo orario venga dopo il primo', () => {
    expect(isAfter('9:00', '10:00')).toBe(true)
    expect(isAfter('10:00', '9:00')).toBe(false)
    expect(isAfter('9:00', '9:00')).toBe(false)
  })

  it('è insensibile allo zero davanti all’ora', () => {
    expect(isAfter('9:30', '10:00')).toBe(true)
  })
})

describe('compareByDayAndTime', () => {
  it('ordina prima per giornata', () => {
    const a = { day: 1, time: '18:00' }
    const b = { day: 2, time: '9:00' }
    expect(compareByDayAndTime(a, b)).toBeLessThan(0)
  })

  it('ordina "9:30" prima di "10:00" nonostante il confronto fra stringhe dica il contrario', () => {
    // È il bug dell'app Android che qui non deve ripresentarsi.
    expect('9:30' < '10:00').toBe(false)

    const a = { day: 1, time: '9:30' }
    const b = { day: 1, time: '10:00' }
    expect(compareByDayAndTime(a, b)).toBeLessThan(0)
  })

  it('ordina correttamente una giornata intera', () => {
    const matches = [
      { day: 1, time: '11:00' },
      { day: 1, time: '9:30' },
      { day: 2, time: '9:00' },
      { day: 1, time: '10:00' },
    ]

    expect([...matches].sort(compareByDayAndTime).map((m) => `${m.day}/${m.time}`)).toEqual([
      '1/9:30',
      '1/10:00',
      '1/11:00',
      '2/9:00',
    ])
  })
})

describe('countAvailableSlots', () => {
  it('somma le capienze delle fasce', () => {
    const slots = [
      { day: 1, start: '9:00', end: '12:00' }, // 180 min → 6 partite da 30
      { day: 2, start: '14:00', end: '15:30' }, // 90 min → 3 partite
    ]
    expect(countAvailableSlots(slots, 30)).toBe(9)
  })

  it('ignora le fasce con orari invertiti o non validi', () => {
    const slots = [
      { day: 1, start: '12:00', end: '9:00' },
      { day: 2, start: 'boh', end: '15:30' },
    ]
    expect(countAvailableSlots(slots, 30)).toBe(0)
  })
})
