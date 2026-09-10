import type { Player } from './models'
import type { StatDefinition } from './statCatalog'

/**
 * Funzioni pure su Player, portate da Player.java.
 */

/**
 * Voto complessivo di un giocatore rispetto a un catalogo di stat.
 * Porta `Player.getVote()` dopo il passaggio al catalogo dinamico.
 *
 * Regola: somma dei valori per ogni stat del catalogo (mancanti = 0), più
 * `def.step` per ogni stat con `allowBonus` e `bonus[key] === true`. Valori
 * orfani (stat cancellate dal catalogo) non contribuiscono, così un catalogo
 * "pulito" non trascina i punti di stat rimosse.
 */
export function getVote(player: Player, catalog: StatDefinition[]): number {
  let total = 0
  for (const def of catalog) {
    total += player.stats[def.key] ?? 0
    if (def.allowBonus && player.bonus[def.key] === true) {
      total += def.step
    }
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
 * Comparator per voto decrescente, ricavato da un catalogo di stat.
 * Porta `Player.compareTo` (che restituisce `-Float.compare(...)`).
 *
 * Nota: prima era una funzione a due argomenti direttamente confrontabile.
 * Ora serve una factory perché il voto dipende dal catalogo — dopo la
 * closure il comparator ha la stessa forma di prima.
 */
export function byVoteDesc(catalog: StatDefinition[]): (a: Player, b: Player) => number {
  return (a, b) => getVote(b, catalog) - getVote(a, catalog)
}

/** Ordinamento alfabetico per nome, poi cognome — usato nella lista di selezione. */
export function byNameAsc(a: Player, b: Player): number {
  const byName = a.name.localeCompare(b.name, 'it', { sensitivity: 'base' })
  if (byName !== 0) return byName
  return a.surname.localeCompare(b.surname, 'it', { sensitivity: 'base' })
}
