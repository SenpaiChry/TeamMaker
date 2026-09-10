import { describe, expect, it } from 'vitest'
import { getNameAndSurname, getSurnameOrNickname, getVote, matchesQuery } from './player'
import { TYPE_STARS, type StatDefinition } from './statCatalog'
import { makePlayer } from './testing'

/** Catalogo che pesa tre stat con step 1 e una stat "bonus" che ammette il flag. */
const catalog: StatDefinition[] = [
  { key: 'attack', label: 'Attacco', type: TYPE_STARS, max: 6, step: 1.5, order: 0, allowBonus: false, values: [] },
  { key: 'serve', label: 'Battuta', type: TYPE_STARS, max: 6, step: 1, order: 1, allowBonus: false, values: [] },
  { key: 'vision', label: 'Visione', type: TYPE_STARS, max: 4, step: 2, order: 2, allowBonus: false, values: [] },
  { key: 'bonus', label: 'Bonus', type: TYPE_STARS, max: 2, step: 1, order: 3, allowBonus: true, values: [] },
]

describe('getVote', () => {
  it("somma i valori delle stat presenti nel catalogo", () => {
    const player = makePlayer('a', 0)
    player.stats = { attack: 4.5, serve: 3, vision: 2, bonus: 1 }

    expect(getVote(player, catalog)).toBe(10.5)
  })

  it('vale 0 quando le statistiche sono tutte a zero', () => {
    const player = makePlayer('a', 0)
    player.stats = {}
    expect(getVote(player, catalog)).toBe(0)
  })

  it('tratta come 0 una statistica mancante', () => {
    const player = makePlayer('a', 0)
    // Il database può contenere giocatori salvati prima dell'aggiunta di una statistica.
    player.stats = { attack: 3 }
    expect(getVote(player, catalog)).toBe(3)
  })

  it('somma def.step quando il giocatore ha il bonus per una stat che lo ammette', () => {
    const player = makePlayer('a', 0)
    player.stats = { bonus: 1 }
    player.bonus = { bonus: true }

    // Valore stat (1) + step del bonus (1) = 2.
    expect(getVote(player, catalog)).toBe(2)
  })

  it('ignora il flag bonus per una stat che non lo ammette', () => {
    const player = makePlayer('a', 0)
    player.stats = { attack: 3 }
    // `attack` non ha `allowBonus`: il flag non deve pesare, anche se presente.
    player.bonus = { attack: true }

    expect(getVote(player, catalog)).toBe(3)
  })

  it('ignora i valori orfani (stat rimosse dal catalogo)', () => {
    const player = makePlayer('a', 0)
    player.stats = { attack: 3, ghost: 100 }
    expect(getVote(player, catalog)).toBe(3)
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
