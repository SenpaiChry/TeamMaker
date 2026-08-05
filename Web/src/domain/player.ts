import { STAT_KEYS } from './constants'
import type { Player } from './models'

/**
 * Funzioni pure su Player, portate da Player.java.
 */

/**
 * Voto complessivo: somma delle 12 statistiche.
 * Porta `Player.getVote()`. Le statistiche mancanti valgono 0, come nel Java
 * dove il costruttore vuoto le inizializza a zero.
 */
export function getVote(player: Player): number {
  let total = 0
  for (const key of STAT_KEYS) {
    total += player.stats[key] ?? 0
  }
  return total
}

/** Porta `Player.getSurnameOrNickname()`. */
export function getSurnameOrNickname(player: Player): string {
  return player.surname.length > 0 ? player.surname : player.nickname
}

/**
 * Nome + cognome troncato a `maxSurnameChars` caratteri seguito da un punto.
 * Porta `Player.getNameAndSurname(int)`.
 */
export function getNameAndSurname(player: Player, maxSurnameChars: number): string {
  if (player.surname.length > maxSurnameChars) {
    return `${player.name} ${player.surname.slice(0, maxSurnameChars)}.`
  }
  if (player.surname.length > 0) {
    return `${player.name} ${player.surname}`
  }
  return player.name
}

/**
 * Ricerca testuale su nome, cognome e soprannome.
 * Porta `Player.containsString(String)`: il confronto è case-insensitive e
 * copre anche la forma "nome cognome" concatenata.
 */
export function matchesQuery(player: Player, query: string): boolean {
  const q = query.toLowerCase()
  if (q.length === 0) return false

  const full = `${player.name} ${player.surname}`.toLowerCase()
  return (
    player.name.toLowerCase().includes(q) ||
    player.surname.toLowerCase().includes(q) ||
    player.nickname.toLowerCase().includes(q) ||
    full.includes(q)
  )
}

/**
 * Ordinamento per voto decrescente.
 * Porta `Player.compareTo`, che restituisce `-Float.compare(...)`.
 */
export function byVoteDesc(a: Player, b: Player): number {
  return getVote(b) - getVote(a)
}

/** Ordinamento alfabetico per nome, poi cognome — usato nella lista di selezione. */
export function byNameAsc(a: Player, b: Player): number {
  const byName = a.name.localeCompare(b.name, 'it', { sensitivity: 'base' })
  if (byName !== 0) return byName
  return a.surname.localeCompare(b.surname, 'it', { sensitivity: 'base' })
}
