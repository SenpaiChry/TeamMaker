import { describe, expect, it } from 'vitest'
import {
  countFemales,
  formatFullNames,
  formatNames,
  formatShortNames,
  formatTruncatedNames,
  getTeamNumber,
  getTeamVote,
  teamMatchesQuery,
} from './team'
import { makePlayer, makeTeam } from './testing'

const marco = makePlayer('p1', 10, 'M', { name: 'Marco', surname: 'Rossini' })
const anna = makePlayer('p2', 8, 'F', { name: 'Anna', surname: 'Bianchi', nickname: 'Anny' })
const luca = makePlayer('p3', 6, 'M', { name: 'Luca', surname: '' })

const team = makeTeam('t1', [marco, anna, luca])

describe('getTeamVote', () => {
  it('somma i voti dei giocatori', () => {
    expect(getTeamVote(team)).toBe(24)
  })

  it('arrotonda a un decimale dopo ogni inserimento, come Team.addPlayer', () => {
    const decimals = makeTeam('t1', [1.5, 1.5, 1.5].map((v, i) => makePlayer(`p${i}`, v)))
    expect(getTeamVote(decimals)).toBe(4.5)
  })

  it('vale 0 per una squadra vuota', () => {
    expect(getTeamVote(makeTeam('t1'))).toBe(0)
  })
})

describe('countFemales', () => {
  it('conta le giocatrici', () => {
    expect(countFemales(team)).toBe(1)
    expect(countFemales(makeTeam('t2', [marco, luca]))).toBe(0)
  })
})

describe('formattazione dei nomi', () => {
  it('solo i nomi', () => {
    expect(formatNames(team)).toBe('Marco - Anna - Luca')
  })

  it('nome e iniziale del cognome, saltando i cognomi vuoti', () => {
    expect(formatShortNames(team)).toBe('Marco R - Anna B - Luca')
  })

  it('nome e cognome completo', () => {
    expect(formatFullNames(team)).toBe('Marco Rossini - Anna Bianchi - Luca')
  })

  it('cognome troncato', () => {
    expect(formatTruncatedNames(team, 3)).toBe('Marco Ros. - Anna Bia. - Luca')
  })
})

describe('getTeamNumber', () => {
  const teams = [makeTeam('a'), makeTeam('b'), makeTeam('c')]

  it('numera dalla posizione nella lista, partendo da 1', () => {
    expect(getTeamNumber(teams, 'a')).toBe(1)
    expect(getTeamNumber(teams, 'c')).toBe(3)
  })

  it('restituisce 0 per una chiave sconosciuta', () => {
    expect(getTeamNumber(teams, 'sparita')).toBe(0)
  })
})

describe('teamMatchesQuery', () => {
  it('trova la squadra dal nome di un suo giocatore', () => {
    expect(teamMatchesQuery(team, 'anna')).toBe(true)
    expect(teamMatchesQuery(team, 'rossini')).toBe(true)
  })

  it('trova anche dal soprannome', () => {
    expect(teamMatchesQuery(team, 'anny')).toBe(true)
  })

  it('non trova chi non c’è', () => {
    expect(teamMatchesQuery(team, 'giovanni')).toBe(false)
  })

  it('con query vuota non evidenzia nessuna squadra', () => {
    expect(teamMatchesQuery(team, '')).toBe(false)
    expect(teamMatchesQuery(team, '   ')).toBe(false)
  })

  it('sopporta una squadra inesistente', () => {
    // Le partite possono citare squadre cancellate dal torneo.
    expect(teamMatchesQuery(undefined, 'anna')).toBe(false)
  })
})
