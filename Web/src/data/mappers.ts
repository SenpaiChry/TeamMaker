import { STAT_KEYS } from '@/domain/constants'
import type { Gender, LiveMatch, Match, Player, Stats, Team, Tournament } from '@/domain/models'
import { compareByDayAndTime } from '@/domain/time'

/**
 * Conversione fra le strutture di dominio e il formato del Realtime Database.
 *
 * ⚠️ È l'unico punto dell'applicazione che conosce le stranezze dello schema
 * scritto dall'app Android. Se qualcosa qui non è fedele, l'app Android smette
 * di leggere correttamente i dati salvati dal web.
 *
 * Le stranezze sono:
 *   - `day`, `points1`, `points2` sono salvati come STRINGHE
 *   - `is_valid` è la stringa "true" / "false", non un booleano
 *   - le date sono nel formato italiano "dd/MM/yyyy"
 *   - i giocatori di una squadra sono chiavi numerate `player1`…`playerN`
 */

/** Nodo grezzo letto da Firebase: le chiavi ci sono, i tipi no. */
type RawNode = Record<string, unknown>

// ---------------------------------------------------------------------------
// Conversioni di base
// ---------------------------------------------------------------------------

/** Numero da un valore che il database può avere salvato come stringa. */
function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number') return Number.isFinite(value) ? value : fallback
  if (typeof value === 'string') {
    const parsed = Number(value.trim())
    return Number.isFinite(parsed) ? parsed : fallback
  }
  return fallback
}

function toString(value: unknown, fallback = ''): string {
  return typeof value === 'string' ? value : fallback
}

/** Booleano da `true`, dalla stringa "true", o da qualunque altra cosa (→ false). */
function toBoolean(value: unknown, fallback = false): boolean {
  if (typeof value === 'boolean') return value
  if (typeof value === 'string') return value.toLowerCase() === 'true'
  return fallback
}

/** Data dal formato italiano "dd/MM/yyyy". `null` se assente o non valida. */
export function parseDate(value: unknown): Date | null {
  const text = toString(value).trim()
  if (text.length === 0) return null

  const parts = text.split('/')
  if (parts.length !== 3) return null

  const day = Number(parts[0])
  const month = Number(parts[1])
  const year = Number(parts[2])
  if (!Number.isInteger(day) || !Number.isInteger(month) || !Number.isInteger(year)) return null

  const date = new Date(year, month - 1, day)
  // Rifiuta le date che JavaScript "corregge" da solo, tipo 31/02/2025.
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return null
  }
  return date
}

/** Data nel formato "dd/MM/yyyy" atteso dall'app Android. */
export function formatDate(date: Date): string {
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  return `${day}/${month}/${date.getFullYear()}`
}

// ---------------------------------------------------------------------------
// Giocatori
// ---------------------------------------------------------------------------

function parseStats(raw: unknown): Stats {
  const node = (raw ?? {}) as RawNode
  const stats = {} as Stats
  for (const key of STAT_KEYS) {
    stats[key] = toNumber(node[key])
  }
  return stats
}

export function parsePlayer(key: string, raw: unknown): Player {
  const node = (raw ?? {}) as RawNode
  const gender = toString(node['gender']).toUpperCase() === 'F' ? 'F' : 'M'

  return {
    key,
    name: toString(node['name']),
    surname: toString(node['surname']),
    nickname: toString(node['nickname']),
    gender: gender as Gender,
    isActive: toBoolean(node['is_active'], true),
    stats: parseStats(node['stats']),
  }
}

/** Tutti i giocatori dal nodo `players/`, indicizzati per chiave. */
export function parsePlayers(raw: unknown): Map<string, Player> {
  const node = (raw ?? {}) as RawNode
  const players = new Map<string, Player>()
  for (const [key, value] of Object.entries(node)) {
    players.set(key, parsePlayer(key, value))
  }
  return players
}

/** Giocatore nel formato del database. Il campo `key` non viene serializzato. */
export function serializePlayer(player: Player): RawNode {
  const stats: RawNode = {}
  for (const key of STAT_KEYS) {
    stats[key] = player.stats[key]
  }

  return {
    name: player.name,
    surname: player.surname,
    nickname: player.nickname,
    gender: player.gender,
    is_active: player.isActive,
    stats,
  }
}

// ---------------------------------------------------------------------------
// Partite
// ---------------------------------------------------------------------------

export function parseMatch(key: string, raw: unknown): Match {
  const node = (raw ?? {}) as RawNode

  return {
    key,
    keyTeam1: toString(node['team1']),
    keyTeam2: toString(node['team2']),
    day: toNumber(node['day']),
    time: toString(node['time'], '0:00'),
    points1: toNumber(node['points1']),
    points2: toNumber(node['points2']),
    type: toString(node['type']),
  }
}

/** ⚠️ `day`, `points1` e `points2` tornano stringhe: è ciò che l'app Android legge. */
export function serializeMatch(match: Omit<Match, 'key'>): RawNode {
  return {
    day: String(match.day),
    time: match.time,
    team1: match.keyTeam1,
    team2: match.keyTeam2,
    points1: String(match.points1),
    points2: String(match.points2),
    type: match.type,
  }
}

// ---------------------------------------------------------------------------
// Squadre
// ---------------------------------------------------------------------------

/**
 * Chiavi dei giocatori di una squadra, in ordine di `playerN`.
 *
 * Differenza voluta rispetto al Java: `TournamentUtility` iterava da 1 fino a
 * `getChildrenCount()`, che conta anche il nodo `bracket`. Se `bracket` mancava
 * l'ultimo giocatore veniva perso in silenzio. Qui si raccolgono tutte e sole
 * le chiavi che corrispondono davvero al formato `playerN`.
 */
export function parseTeamPlayerKeys(raw: unknown): string[] {
  const node = (raw ?? {}) as RawNode

  return Object.entries(node)
    .flatMap(([key, value]) => {
      const match = /^player(\d+)$/.exec(key)
      if (match === null) return []
      const playerKey = toString(value)
      if (playerKey.length === 0) return []
      return [{ index: Number(match[1]), playerKey }]
    })
    .sort((a, b) => a.index - b.index)
    .map((entry) => entry.playerKey)
}

export function parseTeam(key: string, raw: unknown, players: Map<string, Player>): Team {
  const node = (raw ?? {}) as RawNode

  return {
    key,
    bracket: toString(node['bracket']),
    // I giocatori cancellati dall'anagrafica restano referenziati nei tornei
    // passati: vengono semplicemente saltati.
    players: parseTeamPlayerKeys(node).flatMap((k) => {
      const player = players.get(k)
      return player === undefined ? [] : [player]
    }),
  }
}

/** Squadra nel formato del database: `bracket` più le chiavi `player1`…`playerN`. */
export function serializeTeam(team: Omit<Team, 'key'>): RawNode {
  const node: RawNode = { bracket: team.bracket }
  team.players.forEach((player, i) => {
    node[`player${i + 1}`] = player.key
  })
  return node
}

// ---------------------------------------------------------------------------
// Tornei
// ---------------------------------------------------------------------------

export function parseTournament(
  key: string,
  raw: unknown,
  players: Map<string, Player>,
): Tournament {
  const node = (raw ?? {}) as RawNode

  const teams = Object.entries((node['teams'] ?? {}) as RawNode).map(([teamKey, value]) =>
    parseTeam(teamKey, value, players),
  )

  const matches = Object.entries((node['matches'] ?? {}) as RawNode)
    .map(([matchKey, value]) => parseMatch(matchKey, value))
    .sort(compareByDayAndTime)

  return {
    key,
    name: toString(node['name']),
    nBracket: toNumber(node['nBracket']),
    date: parseDate(node['date']),
    isValid: toBoolean(node['is_valid']),
    teams,
    matches,
  }
}

export function parseTournaments(raw: unknown, players: Map<string, Player>): Tournament[] {
  const node = (raw ?? {}) as RawNode
  return Object.entries(node).map(([key, value]) => parseTournament(key, value, players))
}

// ---------------------------------------------------------------------------
// Partita live
// ---------------------------------------------------------------------------

export function parseLiveMatch(raw: unknown): LiveMatch | null {
  if (raw === null || raw === undefined) return null
  const node = raw as RawNode

  return {
    active: toBoolean(node['active']),
    tournamentKey: toString(node['tournament_key']),
    matchPosition: toNumber(node['match_position']),
    team1Name: toString(node['team1_name']),
    team2Name: toString(node['team2_name']),
    team1Players: toString(node['team1_players']),
    team2Players: toString(node['team2_players']),
    points1: toNumber(node['points1']),
    points2: toNumber(node['points2']),
    sets1: toNumber(node['sets1']),
    sets2: toNumber(node['sets2']),
    timestamp: toNumber(node['timestamp']),
  }
}

export function serializeLiveMatch(live: Omit<LiveMatch, 'timestamp'>): RawNode {
  return {
    active: live.active,
    tournament_key: live.tournamentKey,
    match_position: live.matchPosition,
    team1_name: live.team1Name,
    team2_name: live.team2Name,
    team1_players: live.team1Players,
    team2_players: live.team2Players,
    points1: live.points1,
    points2: live.points2,
    sets1: live.sets1,
    sets2: live.sets2,
    timestamp: Date.now(),
  }
}
