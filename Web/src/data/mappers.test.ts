import { describe, expect, it } from 'vitest'
import {
  formatDate,
  parseDate,
  parseLiveMatch,
  parseMatch,
  parsePlayer,
  parsePlayers,
  parseTeam,
  parseTeamPlayerKeys,
  parseTournament,
  serializeMatch,
  serializePlayer,
  serializeTeam,
} from './mappers'
import type { Player } from '@/domain/models'

/**
 * Questi test difendono il contratto con l'app Android: ogni asserzione qui
 * descrive come i dati sono realmente scritti nel Realtime Database.
 */

describe('parsePlayer', () => {
  it('legge un giocatore completo', () => {
    const player = parsePlayer('k1', {
      name: 'Marco',
      surname: 'Rossi',
      nickname: 'Ciccio',
      gender: 'M',
      is_active: true,
      stats: { height: 4, attack: 4.5, bonus: 1 },
    })

    expect(player).toMatchObject({
      key: 'k1',
      name: 'Marco',
      surname: 'Rossi',
      gender: 'M',
      isActive: true,
    })
    expect(player.stats.height).toBe(4)
    expect(player.stats.attack).toBe(4.5)
  })

  it('azzera le statistiche mancanti invece di lasciarle indefinite', () => {
    const player = parsePlayer('k1', { name: 'Marco', stats: { height: 4 } })
    expect(player.stats.serve).toBe(0)
    expect(player.stats.mentality).toBe(0)
  })

  it('sopporta cognome e soprannome assenti', () => {
    const player = parsePlayer('k1', { name: 'Marco' })
    expect(player.surname).toBe('')
    expect(player.nickname).toBe('')
  })

  it('considera attivo un giocatore senza il campo is_active', () => {
    expect(parsePlayer('k1', { name: 'Marco' }).isActive).toBe(true)
  })

  it('normalizza il genere su M quando non è F', () => {
    expect(parsePlayer('k1', { gender: 'f' }).gender).toBe('F')
    expect(parsePlayer('k1', { gender: 'X' }).gender).toBe('M')
    expect(parsePlayer('k1', {}).gender).toBe('M')
  })

  it('non esplode su un nodo vuoto', () => {
    expect(() => parsePlayer('k1', null)).not.toThrow()
  })
})

describe('serializePlayer', () => {
  it('scrive tutte e 12 le statistiche e non include la chiave', () => {
    const player = parsePlayer('k1', { name: 'Marco', stats: { height: 4 } })
    const raw = serializePlayer(player)

    expect(raw['key']).toBeUndefined()
    expect(raw['is_active']).toBe(true)
    expect(Object.keys(raw['stats'] as object)).toHaveLength(12)
  })

  it('fa il giro completo senza perdere nulla', () => {
    const original = parsePlayer('k1', {
      name: 'Marco',
      surname: 'Rossi',
      nickname: 'Ciccio',
      gender: 'F',
      is_active: false,
      stats: { height: 4, attack: 4.5, serve: 3 },
    })

    expect(parsePlayer('k1', serializePlayer(original))).toEqual(original)
  })
})

describe('parsePlayers', () => {
  it('indicizza per chiave', () => {
    const players = parsePlayers({ a: { name: 'Anna' }, b: { name: 'Bruno' } })
    expect(players.get('a')?.name).toBe('Anna')
    expect(players.size).toBe(2)
  })

  it('restituisce una mappa vuota se il nodo non esiste', () => {
    expect(parsePlayers(null).size).toBe(0)
  })
})

describe('parseMatch', () => {
  it('converte in numeri i campi che il database salva come stringhe', () => {
    const match = parseMatch('m1', {
      day: '2',
      time: '9:30',
      team1: 't1',
      team2: 't2',
      points1: '15',
      points2: '9',
      type: 'GIRONE ',
    })

    expect(match.day).toBe(2)
    expect(match.points1).toBe(15)
    expect(match.points2).toBe(9)
    expect(match.time).toBe('9:30')
  })

  it('accetta anche i valori già numerici', () => {
    const match = parseMatch('m1', { day: 2, points1: 15, points2: 9 })
    expect(match.day).toBe(2)
    expect(match.points1).toBe(15)
  })

  it('sopporta il campo type assente', () => {
    expect(parseMatch('m1', { day: '1' }).type).toBe('')
  })
})

describe('serializeMatch', () => {
  it('riscrive come stringhe day, points1 e points2', () => {
    const raw = serializeMatch({
      keyTeam1: 't1',
      keyTeam2: 't2',
      day: 2,
      time: '9:30',
      points1: 15,
      points2: 9,
      type: 'GIRONE ',
    })

    expect(raw['day']).toBe('2')
    expect(raw['points1']).toBe('15')
    expect(raw['points2']).toBe('9')
    expect(raw['time']).toBe('9:30')
    expect(raw['team1']).toBe('t1')
  })

  it('fa il giro completo senza perdere nulla', () => {
    const original = parseMatch('m1', {
      day: '3',
      time: '14:05',
      team1: 't1',
      team2: 't2',
      points1: '21',
      points2: '19',
      type: 'BRACKET A',
    })

    expect(parseMatch('m1', serializeMatch(original))).toEqual(original)
  })
})

describe('parseTeamPlayerKeys', () => {
  it('legge i giocatori in ordine numerico', () => {
    const keys = parseTeamPlayerKeys({
      bracket: 'A',
      player1: 'p1',
      player2: 'p2',
      player3: 'p3',
    })
    expect(keys).toEqual(['p1', 'p2', 'p3'])
  })

  it('ordina per numero, non alfabeticamente', () => {
    const raw: Record<string, string> = { bracket: 'A' }
    for (let i = 1; i <= 12; i++) raw[`player${i}`] = `p${i}`

    expect(parseTeamPlayerKeys(raw)).toEqual(
      Array.from({ length: 12 }, (_, i) => `p${i + 1}`),
    )
  })

  it('legge tutti i giocatori anche senza il nodo bracket', () => {
    // È il caso in cui l'app Android perdeva l'ultimo giocatore, perché
    // contava i figli del nodo invece di guardare i nomi delle chiavi.
    expect(parseTeamPlayerKeys({ player1: 'p1', player2: 'p2' })).toEqual(['p1', 'p2'])
  })

  it('ignora le chiavi che non sono giocatori e i riferimenti vuoti', () => {
    expect(parseTeamPlayerKeys({ bracket: 'A', player1: 'p1', player2: '', note: 'x' })).toEqual([
      'p1',
    ])
  })
})

describe('parseTeam', () => {
  const players = parsePlayers({
    p1: { name: 'Anna' },
    p2: { name: 'Bruno' },
  })

  it('risolve le chiavi in giocatori veri', () => {
    const team = parseTeam('t1', { bracket: 'A', player1: 'p1', player2: 'p2' }, players)

    expect(team.bracket).toBe('A')
    expect(team.players.map((p) => p.name)).toEqual(['Anna', 'Bruno'])
  })

  it('salta i giocatori cancellati dall’anagrafica', () => {
    const team = parseTeam('t1', { player1: 'p1', player2: 'sparito' }, players)
    expect(team.players.map((p) => p.name)).toEqual(['Anna'])
  })

  it('restituisce bracket vuoto se il calendario non è stato generato', () => {
    expect(parseTeam('t1', { player1: 'p1' }, players).bracket).toBe('')
  })
})

describe('serializeTeam', () => {
  it('numera i giocatori da 1', () => {
    const players = [...parsePlayers({ p1: { name: 'Anna' }, p2: { name: 'Bruno' } }).values()]
    const raw = serializeTeam({ bracket: 'B', players })

    expect(raw).toEqual({ bracket: 'B', player1: 'p1', player2: 'p2' })
  })

  it('fa il giro completo', () => {
    const players = parsePlayers({ p1: { name: 'Anna' }, p2: { name: 'Bruno' } })
    const team = parseTeam('t1', { bracket: 'A', player1: 'p1', player2: 'p2' }, players)

    expect(parseTeam('t1', serializeTeam(team), players)).toEqual(team)
  })
})

describe('parseDate / formatDate', () => {
  it('legge il formato italiano', () => {
    const date = parseDate('05/03/2025')
    expect(date?.getDate()).toBe(5)
    expect(date?.getMonth()).toBe(2) // marzo
    expect(date?.getFullYear()).toBe(2025)
  })

  it('scrive il formato italiano con lo zero davanti', () => {
    expect(formatDate(new Date(2025, 2, 5))).toBe('05/03/2025')
  })

  it('fa il giro completo', () => {
    expect(formatDate(parseDate('25/12/2024')!)).toBe('25/12/2024')
  })

  it('restituisce null su date assenti o impossibili', () => {
    expect(parseDate('')).toBeNull()
    expect(parseDate(null)).toBeNull()
    expect(parseDate('2025-03-05')).toBeNull()
    expect(parseDate('31/02/2025')).toBeNull()
  })
})

describe('parseTournament', () => {
  const players = parsePlayers({ p1: { name: 'Anna' }, p2: { name: 'Bruno' } })

  const raw = {
    is_valid: 'true',
    name: 'Torneo di prova',
    nBracket: 2,
    date: '05/03/2025',
    teams: {
      t1: { bracket: 'A', player1: 'p1' },
      t2: { bracket: 'B', player1: 'p2' },
    },
    matches: {
      m2: { day: '1', time: '11:00', team1: 't1', team2: 't2', points1: '0', points2: '0' },
      m1: { day: '1', time: '9:30', team1: 't1', team2: 't2', points1: '15', points2: '9' },
    },
  }

  it('legge is_valid come stringa', () => {
    expect(parseTournament('k', raw, players).isValid).toBe(true)
    expect(parseTournament('k', { ...raw, is_valid: 'false' }, players).isValid).toBe(false)
    expect(parseTournament('k', { ...raw, is_valid: undefined }, players).isValid).toBe(false)
  })

  it('ordina le partite per giornata e orario reale', () => {
    // "9:30" deve venire prima di "11:00" nonostante l'ordine delle chiavi.
    const tournament = parseTournament('k', raw, players)
    expect(tournament.matches.map((m) => m.time)).toEqual(['9:30', '11:00'])
  })

  it('legge squadre e giocatori', () => {
    const tournament = parseTournament('k', raw, players)
    expect(tournament.teams).toHaveLength(2)
    expect(tournament.teams[0]!.players[0]!.name).toBe('Anna')
  })

  it('sopporta un torneo senza squadre né partite', () => {
    const tournament = parseTournament('k', { name: 'Vuoto', is_valid: 'false' }, players)
    expect(tournament.teams).toEqual([])
    expect(tournament.matches).toEqual([])
  })
})

describe('parseLiveMatch', () => {
  it('legge lo stato della partita in corso', () => {
    const live = parseLiveMatch({
      active: true,
      tournament_key: 'k1',
      match_position: 3,
      team1_name: 'TEAM 1',
      points1: 12,
      sets1: 1,
      timestamp: 1700000000000,
    })

    expect(live).toMatchObject({
      active: true,
      tournamentKey: 'k1',
      matchPosition: 3,
      points1: 12,
      sets1: 1,
    })
  })

  it('restituisce null se il nodo non esiste', () => {
    expect(parseLiveMatch(null)).toBeNull()
    expect(parseLiveMatch(undefined)).toBeNull()
  })

  it('considera non attiva una partita senza il campo active', () => {
    expect(parseLiveMatch({ tournament_key: 'k1' })?.active).toBe(false)
  })
})

describe('coerenza fra dominio e database', () => {
  it('un giocatore serializzato è leggibile dallo schema Android', () => {
    const player: Player = parsePlayer('k1', {
      name: 'Marco',
      surname: 'Rossi',
      gender: 'M',
      stats: { height: 4 },
    })
    const raw = serializePlayer(player)

    // I campi che l'app Android legge per nome devono esserci tutti.
    for (const field of ['name', 'surname', 'nickname', 'gender', 'is_active', 'stats']) {
      expect(raw).toHaveProperty(field)
    }
  })
})
