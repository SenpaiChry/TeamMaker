import type { BonusFlags, Gender, Match, Player, Stats, Team } from './models'
import type { StatDefinition } from './statCatalog'
import { TYPE_STARS } from './statCatalog'

/** Aiutanti per costruire dati di prova nei test del dominio. */

/**
 * Catalogo minimo per i test: un'unica stat STARS con la chiave `vote` e
 * `step = 1`, così caricare `stats.vote = N` dà direttamente un giocatore
 * con voto `N`. Le stat reali del progetto sono più ricche, ma per testare
 * la generazione basta un peso singolo.
 */
export function makeTestCatalog(): StatDefinition[] {
  return [
    {
      key: 'vote',
      label: 'Voto',
      type: TYPE_STARS,
      max: 10,
      step: 1,
      order: 0,
      allowBonus: false,
      values: [],
    },
  ]
}

export function makeStats(overrides: Stats = {}): Stats {
  return { ...overrides }
}

/**
 * Giocatore con voto complessivo `vote`, caricato sulla stat `vote` del
 * catalogo di test (vedi `makeTestCatalog`). Per usarlo insieme a `getVote`
 * passa il catalogo di test come secondo argomento.
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
    stats: { vote },
    bonus: {} as BonusFlags,
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

/**
 * Partita con tutti i campi valorizzati a un default sensato, così i test
 * possono passare solo le proprietà che li interessano.
 */
export function makeMatch(overrides: Partial<Match> = {}): Match {
  return {
    key: 'm1',
    keyTeam1: 't1',
    keyTeam2: 't2',
    day: 1,
    time: '9:00',
    points1: 0,
    points2: 0,
    detail: [],
    type: 'GROUP',
    source1Type: '',
    source1Ref: '',
    source2Type: '',
    source2Ref: '',
    ...overrides,
  }
}
