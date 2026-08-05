import type { Player, Team } from './models'
import { getNameAndSurname, getVote } from './player'

/**
 * Funzioni pure su Team, portate da Team.java.
 */

/**
 * Voto totale della squadra.
 *
 * ⚠️ Riproduce fedelmente `Team.addPlayer()`, che arrotonda a un decimale
 * **dopo ogni singolo inserimento**. L'arrotondamento è quindi cumulativo e
 * dipende dall'ordine dei giocatori: sommare tutto e arrotondare alla fine
 * darebbe risultati leggermente diversi da quelli mostrati dall'app Android.
 */
export function getTeamVote(team: Team): number {
  let total = 0
  for (const player of team.players) {
    total = Math.round((total + getVote(player)) * 10) / 10
  }
  return total
}

/** Numero di giocatrici nella squadra: vincolo di bilanciamento del generatore. */
export function countFemales(team: Team): number {
  return team.players.filter((p) => p.gender === 'F').length
}

/** Solo i nomi: "Marco - Luca - Anna". Porta `toStringOnlyName()`. */
export function formatNames(team: Team): string {
  return team.players.map((p) => p.name).join(' - ')
}

/** Nome + iniziale del cognome: "Marco R - Luca B". Porta `toStringShortName()`. */
export function formatShortNames(team: Team): string {
  return team.players
    .map((p) => (p.surname.length > 0 ? `${p.name} ${p.surname[0]}` : p.name))
    .join(' - ')
}

/** Nome + cognome completo. Porta `toStringNameAndSurname()`. */
export function formatFullNames(team: Team): string {
  return team.players
    .map((p) => (p.surname.length > 0 ? `${p.name} ${p.surname}` : p.name))
    .join(' - ')
}

/** Nome + cognome troncato. Porta `toStringNameAndSurnameNChar(int)`. */
export function formatTruncatedNames(team: Team, maxSurnameChars: number): string {
  return team.players.map((p) => getNameAndSurname(p, maxSurnameChars)).join(' - ')
}

/**
 * Numero progressivo della squadra nel torneo (1-based), come mostrato in UI
 * ("TEAM 3"). Porta `Tournament.getNTeamByKey()`, che si basa sull'ordine
 * degli elementi nella lista. Restituisce 0 se la chiave non esiste.
 */
export function getTeamNumber(teams: Team[], teamKey: string): number {
  return teams.findIndex((t) => t.key === teamKey) + 1
}

/** Costruisce una squadra a partire dalle chiavi dei giocatori. */
export function buildTeam(key: string, bracket: string, players: Player[]): Team {
  return { key, bracket, players }
}
