import { describe, expect, it } from 'vitest'
import { getNameAndSurname, getSurnameOrNickname, getVote, matchesQuery } from './player'
import { makePlayer, makeStats } from './testing'

describe('getVote', () => {
  it('somma tutte e 12 le statistiche', () => {
    const player = makePlayer('a', 0)
    player.stats = makeStats({ height: 4, attack: 4.5, serve: 3, 'game-vision': 2, bonus: 1 })

    expect(getVote(player)).toBe(14.5)
  })

  it('vale 0 quando le statistiche sono tutte a zero', () => {
    expect(getVote(makePlayer('a', 0))).toBe(0)
  })

  it('tratta come 0 una statistica mancante', () => {
    const player = makePlayer('a', 0)
    // Il database può contenere giocatori salvati prima dell'aggiunta di una statistica.
    delete (player.stats as Partial<typeof player.stats>).bonus

    expect(getVote(player)).toBe(0)
  })
})

describe('getSurnameOrNickname', () => {
  it('preferisce il cognome', () => {
    const player = makePlayer('a', 0, 'M', { surname: 'Rossi', nickname: 'Ross' })
    expect(getSurnameOrNickname(player)).toBe('Rossi')
  })

  it('ripiega sul soprannome se il cognome manca', () => {
    const player = makePlayer('a', 0, 'M', { surname: '', nickname: 'Ross' })
    expect(getSurnameOrNickname(player)).toBe('Ross')
  })
})

describe('getNameAndSurname', () => {
  it('tronca il cognome più lungo del limite e aggiunge il punto', () => {
    const player = makePlayer('a', 0, 'M', { name: 'Marco', surname: 'Rossini' })
    expect(getNameAndSurname(player, 3)).toBe('Marco Ros.')
  })

  it('lascia intatto il cognome entro il limite', () => {
    const player = makePlayer('a', 0, 'M', { name: 'Marco', surname: 'Ros' })
    expect(getNameAndSurname(player, 3)).toBe('Marco Ros')
  })

  it('restituisce solo il nome se il cognome è vuoto', () => {
    const player = makePlayer('a', 0, 'M', { name: 'Marco', surname: '' })
    expect(getNameAndSurname(player, 3)).toBe('Marco')
  })
})

describe('matchesQuery', () => {
  const player = makePlayer('a', 0, 'M', {
    name: 'Marco',
    surname: 'Rossi',
    nickname: 'Ciccio',
  })

  it('trova per nome, cognome o soprannome, ignorando le maiuscole', () => {
    expect(matchesQuery(player, 'mar')).toBe(true)
    expect(matchesQuery(player, 'ROSS')).toBe(true)
    expect(matchesQuery(player, 'ciccio')).toBe(true)
  })

  it('trova per "nome cognome" scritto per esteso', () => {
    expect(matchesQuery(player, 'marco rossi')).toBe(true)
  })

  it('non trova ciò che non c’è', () => {
    expect(matchesQuery(player, 'luigi')).toBe(false)
  })

  it('con query vuota non evidenzia nessuno', () => {
    expect(matchesQuery(player, '')).toBe(false)
  })
})
