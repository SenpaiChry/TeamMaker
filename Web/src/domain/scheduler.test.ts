import { describe, expect, it } from 'vitest'
import {
  assignSlots,
  buildRoundRobinRounds,
  countItalianBracketMatches,
  countMultipleBracketMatches,
  generateItalianBracket,
  generateMultipleBrackets,
  getBracketLabel,
  getBracketLetter,
  interleaveOneByOne,
  NotEnoughTimeError,
  orderRoundsBySpacing,
} from './scheduler'
import type { TimeSlot } from './time'
import { makeTeams } from './testing'

const wholeDay: TimeSlot[] = [{ day: 1, start: '9:00', end: '20:00' }]

describe('getBracketLetter', () => {
  it('numera i gironi come le colonne di un foglio di calcolo', () => {
    expect(getBracketLetter(0)).toBe('A')
    expect(getBracketLetter(25)).toBe('Z')
    expect(getBracketLetter(26)).toBe('AA')
    expect(getBracketLetter(27)).toBe('AB')
  })

  it('compone l’etichetta usata nel campo type', () => {
    expect(getBracketLabel(1)).toBe('BRACKET B')
  })
})

describe('buildRoundRobinRounds', () => {
  it('con 4 squadre produce 3 turni da 2 partite', () => {
    const rounds = buildRoundRobinRounds(makeTeams(4), 'GIRONE ')

    expect(rounds).toHaveLength(3)
    expect(rounds.every((r) => r.length === 2)).toBe(true)
  })

  it('fa incontrare ogni coppia esattamente una volta', () => {
    const rounds = buildRoundRobinRounds(makeTeams(6), 'GIRONE ')
    const pairs = rounds.flat().map((m) => [m.keyTeam1, m.keyTeam2].sort().join('-'))

    expect(pairs).toHaveLength(15) // 6·5/2
    expect(new Set(pairs).size).toBe(15)
  })

  it('con un numero dispari di squadre salta il turno di riposo', () => {
    const rounds = buildRoundRobinRounds(makeTeams(5), 'GIRONE ')
    const matches = rounds.flat()

    expect(rounds).toHaveLength(5) // 5 turni, uno di riposo a testa
    expect(matches).toHaveLength(10) // 5·4/2
    expect(matches.some((m) => m.keyTeam1.includes('BYE') || m.keyTeam2.includes('BYE'))).toBe(false)
  })

  it('in ogni turno una squadra gioca al massimo una volta', () => {
    for (const round of buildRoundRobinRounds(makeTeams(8), 'GIRONE ')) {
      const involved = round.flatMap((m) => [m.keyTeam1, m.keyTeam2])
      expect(new Set(involved).size).toBe(involved.length)
    }
  })

  it('con meno di due squadre non genera partite', () => {
    // Con una sola squadra il metodo del cerchio produce comunque un turno,
    // che però resta vuoto perché l'unico accoppiamento è contro il BYE.
    expect(buildRoundRobinRounds(makeTeams(1), 'GIRONE ').flat()).toEqual([])
    expect(buildRoundRobinRounds(makeTeams(0), 'GIRONE ').flat()).toEqual([])
  })

  it('applica l’etichetta a tutte le partite', () => {
    const matches = buildRoundRobinRounds(makeTeams(4), 'BRACKET A').flat()
    expect(matches.every((m) => m.type === 'BRACKET A')).toBe(true)
  })
})

describe('orderRoundsBySpacing', () => {
  it('non fa giocare la stessa squadra due volte di fila', () => {
    const ordered = orderRoundsBySpacing(buildRoundRobinRounds(makeTeams(6), 'GIRONE '))

    for (let i = 1; i < ordered.length; i++) {
      const previous = new Set([ordered[i - 1]!.keyTeam1, ordered[i - 1]!.keyTeam2])
      const current = [ordered[i]!.keyTeam1, ordered[i]!.keyTeam2]
      expect(current.some((k) => previous.has(k))).toBe(false)
    }
  })

  it('conserva tutte le partite', () => {
    const rounds = buildRoundRobinRounds(makeTeams(6), 'GIRONE ')
    expect(orderRoundsBySpacing(rounds)).toHaveLength(rounds.flat().length)
  })
})

describe('interleaveOneByOne', () => {
  it('alterna le partite dei gironi una a una', () => {
    const a = [{ type: 'A1' }, { type: 'A2' }, { type: 'A3' }]
    const b = [{ type: 'B1' }, { type: 'B2' }]

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const out = interleaveOneByOne([a, b] as any)
    expect(out.map((m) => m.type)).toEqual(['A1', 'B1', 'A2', 'B2', 'A3'])
  })

  it('svuota il girone più lungo dopo che gli altri sono finiti', () => {
    const a = [{ type: 'A1' }, { type: 'A2' }, { type: 'A3' }, { type: 'A4' }]
    const b = [{ type: 'B1' }]

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const out = interleaveOneByOne([a, b] as any)
    expect(out.map((m) => m.type)).toEqual(['A1', 'B1', 'A2', 'A3', 'A4'])
  })
})

describe('assignSlots', () => {
  const matches = Array.from({ length: 4 }, (_, i) => ({
    keyTeam1: `a${i}`,
    keyTeam2: `b${i}`,
    points1: 0,
    points2: 0,
    type: 'GIRONE ',
  }))

  it('incolonna le partite ogni N minuti', () => {
    const scheduled = assignSlots(matches, [{ day: 1, start: '9:00', end: '12:00' }], 30)

    expect(scheduled.map((m) => m.time)).toEqual(['9:00', '9:30', '10:00', '10:30'])
    expect(scheduled.every((m) => m.day === 1)).toBe(true)
  })

  it('passa alla fascia successiva quando la partita non ci sta più', () => {
    const slots: TimeSlot[] = [
      { day: 1, start: '9:00', end: '10:00' }, // 2 partite da 30
      { day: 2, start: '15:00', end: '16:00' },
    ]
    const scheduled = assignSlots(matches, slots, 30)

    expect(scheduled.map((m) => `${m.day}/${m.time}`)).toEqual([
      '1/9:00',
      '1/9:30',
      '2/15:00',
      '2/15:30',
    ])
  })

  it('non programma una partita che sforerebbe la fine della fascia', () => {
    // 50 minuti disponibili, partite da 30: ce ne sta una sola.
    const slots: TimeSlot[] = [
      { day: 1, start: '9:00', end: '9:50' },
      { day: 2, start: '9:00', end: '20:00' },
    ]
    const scheduled = assignSlots(matches, slots, 30)

    expect(scheduled[0]).toMatchObject({ day: 1, time: '9:00' })
    expect(scheduled[1]).toMatchObject({ day: 2, time: '9:00' })
  })

  it('solleva un errore invece di sovrapporre le partite se il tempo finisce', () => {
    // L'app Android qui assegnava a tutte le partite rimaste lo stesso orario.
    const slots: TimeSlot[] = [{ day: 1, start: '9:00', end: '10:00' }]

    expect(() => assignSlots(matches, slots, 30)).toThrow(NotEnoughTimeError)
    expect(() => assignSlots(matches, slots, 30)).toThrow(/2 partite su 4/)
  })

  it('mantiene il formato orario del database', () => {
    const scheduled = assignSlots(matches.slice(0, 1), [{ day: 1, start: '9:00', end: '12:00' }], 30)
    expect(scheduled[0]!.time).toBe('9:00')
    expect(scheduled[0]!.time).not.toBe('09:00')
  })
})

describe('generateItalianBracket', () => {
  it('mette tutte le squadre nel girone A', () => {
    const teams = makeTeams(6)
    const schedule = generateItalianBracket(teams, wholeDay, 30)

    expect(schedule.bracketCount).toBe(1)
    for (const team of teams) {
      expect(schedule.bracketByTeam.get(team.key)).toBe('A')
    }
  })

  it('genera tutte le partite del girone all’italiana', () => {
    const schedule = generateItalianBracket(makeTeams(6), wholeDay, 30)
    expect(schedule.matches).toHaveLength(countItalianBracketMatches(6))
  })

  it('etichetta le partite come GIRONE, così entrano in classifica', () => {
    const schedule = generateItalianBracket(makeTeams(4), wholeDay, 30)
    expect(schedule.matches.every((m) => m.type.includes('GIRONE'))).toBe(true)
  })

  it('programma le partite in orari crescenti', () => {
    const schedule = generateItalianBracket(makeTeams(5), wholeDay, 30)
    const times = schedule.matches.map((m) => m.time)
    expect(times).toEqual([...times])
    expect(new Set(times).size).toBe(times.length)
  })
})

describe('generateMultipleBrackets', () => {
  it('distribuisce le squadre a rotazione sui gironi', () => {
    const teams = makeTeams(6)
    const schedule = generateMultipleBrackets(teams, wholeDay, 30, 2)

    expect(schedule.bracketByTeam.get('t1')).toBe('A')
    expect(schedule.bracketByTeam.get('t2')).toBe('B')
    expect(schedule.bracketByTeam.get('t3')).toBe('A')
    expect(schedule.bracketByTeam.get('t4')).toBe('B')
  })

  it('genera il numero di partite previsto dal conteggio', () => {
    const schedule = generateMultipleBrackets(makeTeams(8), wholeDay, 30, 2)
    expect(schedule.matches).toHaveLength(countMultipleBracketMatches(8, 2))
  })

  it('non fa incontrare squadre di gironi diversi', () => {
    const schedule = generateMultipleBrackets(makeTeams(8), wholeDay, 30, 2)

    for (const match of schedule.matches) {
      expect(schedule.bracketByTeam.get(match.keyTeam1)).toBe(
        schedule.bracketByTeam.get(match.keyTeam2),
      )
    }
  })

  it('rifiuta un numero di gironi non valido', () => {
    expect(() => generateMultipleBrackets(makeTeams(4), wholeDay, 30, 0)).toThrow(RangeError)
  })
})

describe('conteggio partite', () => {
  it('girone all’italiana: n·(n-1)/2', () => {
    expect(countItalianBracketMatches(6)).toBe(15)
    expect(countItalianBracketMatches(1)).toBe(0)
  })

  it('gironi multipli: somma dei gironi, anche di dimensione diversa', () => {
    expect(countMultipleBracketMatches(8, 2)).toBe(12) // 4+4 squadre → 6+6
    expect(countMultipleBracketMatches(7, 2)).toBe(9) // 4+3 squadre → 6+3
    expect(countMultipleBracketMatches(6, 3)).toBe(3) // 2+2+2 squadre → 1+1+1
  })

  it('coincide con quanto generato davvero', () => {
    for (const teamCount of [4, 5, 6, 7, 8, 9]) {
      for (const bracketCount of [1, 2, 3]) {
        const schedule = generateMultipleBrackets(
          makeTeams(teamCount),
          [{ day: 1, start: '0:00', end: '24:00' }],
          10,
          bracketCount,
        )
        expect(schedule.matches).toHaveLength(countMultipleBracketMatches(teamCount, bracketCount))
      }
    }
  })
})
