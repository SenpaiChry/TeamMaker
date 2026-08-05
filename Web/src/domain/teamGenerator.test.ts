import { describe, expect, it } from 'vitest'
import type { Player } from './models'
import { getVote } from './player'
import { getTeamVote } from './team'
import {
  countTeams,
  generateSerpentine,
  hasEnoughPlayers,
  searchBalancedTeams,
} from './teamGenerator'
import { makePlayer } from './testing'

/** Voto grezzo di una squadra, senza l'arrotondamento cumulativo di Team.addPlayer. */
function rawVote(players: Player[]): number {
  return players.reduce((sum, p) => sum + getVote(p), 0)
}

function spread(teams: { players: Player[] }[]): number {
  const votes = teams.map((t) => rawVote(t.players))
  return Math.max(...votes) - Math.min(...votes)
}

/** 12 giocatori con voti da 1 a 12, quattro dei quali donne. */
function roster(): Player[] {
  return Array.from({ length: 12 }, (_, i) =>
    makePlayer(`p${i + 1}`, i + 1, i % 3 === 0 ? 'F' : 'M'),
  )
}

describe('countTeams', () => {
  it('arrotonda per eccesso', () => {
    expect(countTeams(12, 4)).toBe(3)
    expect(countTeams(13, 4)).toBe(4)
    expect(countTeams(0, 4)).toBe(0)
  })
})

describe('hasEnoughPlayers', () => {
  it('richiede almeno due squadre, la seconda anche incompleta', () => {
    // Porta il controllo `size >= playersPerTeam * 2 - 1` della schermata Android.
    expect(hasEnoughPlayers(3, 2)).toBe(true)
    expect(hasEnoughPlayers(2, 2)).toBe(false)
    expect(hasEnoughPlayers(7, 4)).toBe(true)
    expect(hasEnoughPlayers(6, 4)).toBe(false)
  })
})

describe('generateSerpentine', () => {
  it('distribuisce a serpentina invertendo l’ordine a ogni giro', () => {
    // Voti 6,5,4,3,2,1 su 3 squadre: giro 1 → 6,5,4; giro 2 invertito → 3,2,1
    // finiscono rispettivamente nelle squadre che avevano preso 4,5,6.
    const players = [6, 5, 4, 3, 2, 1].map((v, i) => makePlayer(`p${i}`, v))
    const teams = generateSerpentine(players, 2)

    expect(teams).toHaveLength(3)
    expect(teams.map((t) => rawVote(t.players)).sort((a, b) => a - b)).toEqual([7, 7, 7])
  })

  it('assegna a ogni giocatore una e una sola squadra', () => {
    const players = roster()
    const teams = generateSerpentine(players, 4)

    const assigned = teams.flatMap((t) => t.players.map((p) => p.key))
    expect(assigned).toHaveLength(players.length)
    expect(new Set(assigned).size).toBe(players.length)
  })

  it('riempie l’ultima squadra parzialmente quando i giocatori non bastano', () => {
    const teams = generateSerpentine(roster().slice(0, 10), 4)
    expect(teams).toHaveLength(3)
    expect(teams.reduce((sum, t) => sum + t.players.length, 0)).toBe(10)
  })
})

describe('searchBalancedTeams', () => {
  it('rispetta la dimensione massima delle squadre', () => {
    const result = searchBalancedTeams(roster(), { playersPerTeam: 4, seed: 1 })

    expect(result).not.toBeNull()
    expect(result!.teams).toHaveLength(3)
    for (const team of result!.teams) {
      expect(team.players.length).toBeLessThanOrEqual(4)
    }
  })

  it('assegna ogni giocatore esattamente una volta', () => {
    const players = roster()
    const result = searchBalancedTeams(players, { playersPerTeam: 3, seed: 42 })

    const assigned = result!.teams.flatMap((t) => t.players.map((p) => p.key))
    expect(assigned).toHaveLength(players.length)
    expect(new Set(assigned).size).toBe(players.length)
  })

  it('bilancia le giocatrici con uno scarto massimo di 1', () => {
    const players = roster() // 4 donne su 12
    const result = searchBalancedTeams(players, { playersPerTeam: 4, seed: 7 })

    const females = result!.teams.map((t) => t.players.filter((p) => p.gender === 'F').length)
    expect(Math.max(...females) - Math.min(...females)).toBeLessThanOrEqual(1)
  })

  it('bilancia i voti entro la tolleranza dichiarata', () => {
    const result = searchBalancedTeams(roster(), { playersPerTeam: 4, seed: 3 })

    expect(result).not.toBeNull()
    // `difference` è ciò che l'app mostra all'utente come qualità del bilanciamento.
    expect(spread(result!.teams)).toBeCloseTo(result!.difference, 5)
  })

  it('trova la ripartizione perfetta quando esiste', () => {
    // Voti 1..8 su 4 squadre da 2: ogni squadra può totalizzare 9.
    const players = Array.from({ length: 8 }, (_, i) => makePlayer(`p${i + 1}`, i + 1))
    const result = searchBalancedTeams(players, { playersPerTeam: 2, seed: 11 })

    expect(result).not.toBeNull()
    expect(spread(result!.teams)).toBe(0)
  })

  it('è deterministico a parità di seme', () => {
    const players = roster()
    const a = searchBalancedTeams(players, { playersPerTeam: 4, seed: 99 })
    const b = searchBalancedTeams(players, { playersPerTeam: 4, seed: 99 })

    const keys = (r: typeof a) => r!.teams.map((t) => t.players.map((p) => p.key).join(','))
    expect(keys(a)).toEqual(keys(b))
    expect(a!.retries).toBe(b!.retries)
  })

  it('semi diversi esplorano soluzioni diverse quando ne esiste più di una', () => {
    // Con 12 giocatori in squadre da 4 il vincolo è largo e le ripartizioni
    // valide sono molte: semi diversi devono trovarne di diverse.
    // (Con squadre da 3, sullo stesso roster, la soluzione valida è una sola
    // e tutti i semi convergono lì: è il comportamento atteso, non un difetto.)
    const players = roster()
    const shapes = new Set(
      [1, 2, 3, 4, 5, 6, 7, 8].map((seed) =>
        searchBalancedTeams(players, { playersPerTeam: 4, seed })!
          .teams.map((t) => [...t.players.map((p) => p.key)].sort().join(','))
          .sort()
          .join('|'),
      ),
    )

    expect(shapes.size).toBeGreaterThan(1)
  })

  it('trova una soluzione valida qualunque sia il seme', () => {
    const players = roster()

    for (let seed = 1; seed <= 30; seed++) {
      const result = searchBalancedTeams(players, { playersPerTeam: 4, seed })

      expect(result, `nessuna soluzione con seme ${seed}`).not.toBeNull()

      const sizes = result!.teams.map((t) => t.players.length)
      expect(Math.max(...sizes)).toBeLessThanOrEqual(4)

      const females = result!.teams.map((t) => t.players.filter((p) => p.gender === 'F').length)
      expect(Math.max(...females) - Math.min(...females)).toBeLessThanOrEqual(1)
    }
  })

  it('restituisce null se non trova nulla nei tentativi concessi', () => {
    const players = roster()
    const result = searchBalancedTeams(players, {
      playersPerTeam: 4,
      maxRetries: 0,
      seed: 1,
    })

    expect(result).toBeNull()
  })

  it('gestisce l’ultima squadra incompleta', () => {
    const players = Array.from({ length: 10 }, (_, i) => makePlayer(`p${i + 1}`, i + 1))
    const result = searchBalancedTeams(players, { playersPerTeam: 4, seed: 5 })

    expect(result!.teams).toHaveLength(3)
    expect(result!.teams.reduce((sum, t) => sum + t.players.length, 0)).toBe(10)
  })

  it('non produce squadre vuote', () => {
    const result = searchBalancedTeams(roster(), { playersPerTeam: 4, seed: 8 })
    for (const team of result!.teams) {
      expect(team.players.length).toBeGreaterThan(0)
    }
  })
})

describe('getTeamVote', () => {
  it('arrotonda a un decimale dopo ogni inserimento, come Team.addPlayer', () => {
    // 3 giocatori da 1.5: 1.5 → 3 → 4.5
    const team = { key: 't1', bracket: '', players: [1.5, 1.5, 1.5].map((v, i) => makePlayer(`p${i}`, v)) }
    expect(getTeamVote(team)).toBe(4.5)
  })
})
