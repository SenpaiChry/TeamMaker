import { STAT_KEYS } from './constants'
import type { Gender, Player, Stats, Team } from './models'

/** Aiutanti per costruire dati di prova nei test del dominio. */

export function makeStats(overrides: Partial<Stats> = {}): Stats {
  const stats = {} as Stats
  for (const key of STAT_KEYS) {
    stats[key] = 0
  }
  return { ...stats, ...overrides }
}

/**
 * Giocatore con un voto complessivo pari a `vote`, ottenuto caricando la
 * statistica `bonus`. Comodo per i test del generatore, dove conta solo il voto.
 */
export function makePlayer(
  key: string,
  vote: number,
  gender: Gender = 'M',
  overrides: Partial<Player> = {},
): Player {
  return {
    key,
    name: `Nome${key}`,
    surname: `Cognome${key}`,
    nickname: '',
    gender,
    isActive: true,
    stats: makeStats({ bonus: vote }),
    ...overrides,
  }
}

export function makeTeam(key: string, players: Player[] = [], bracket = ''): Team {
  return { key, bracket, players }
}

/** N squadre vuote con chiavi "t1", "t2", … */
export function makeTeams(count: number): Team[] {
  return Array.from({ length: count }, (_, i) => makeTeam(`t${i + 1}`))
}
