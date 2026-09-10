/**
 * Modelli di dominio, portati da Player/Team/Match/Tournament.java.
 *
 * Sono strutture dati immutabili e senza metodi: il comportamento sta nelle
 * funzioni pure dei moduli accanto. Nessun campo qui rispecchia le stranezze
 * di serializzazione del database (numeri come stringhe, ecc.): quelle vivono
 * solo in `src/data/mappers.ts`.
 */

export type Gender = 'M' | 'F'

/**
 * Valori delle statistiche di un giocatore, indicizzati per key opaca della
 * stat (push-key di `teammaker/stats`, es. `-Nxyz…`). Le chiavi non sono più
 * hardcoded: prima erano `agility`, `height`, ecc.; ora arrivano dal catalogo
 * dinamico letto da Firebase. I valori mancanti valgono 0.
 */
export type Stats = Record<string, number>

/**
 * Flag di bonus per-stat, indicizzato come `stats`. `true` se il giocatore ha
 * il bonus attivo per quella stat (visibile solo se la stat ha `allowBonus`).
 * Per compattezza sul DB si scrivono SOLO le chiavi con valore `true`.
 */
export type BonusFlags = Record<string, boolean>

export interface Player {
  key: string
  name: string
  surname: string
  nickname: string
  gender: Gender
  isActive: boolean
  stats: Stats
  bonus: BonusFlags
}

export interface Team {
  key: string
  /** Lettera del girone: "A", "B", … Stringa vuota se il calendario non è ancora stato generato. */
  bracket: string
  players: Player[]
}

export interface Match {
  key: string
  keyTeam1: string
  keyTeam2: string
  day: number
  /** Formato "H:mm" — l'ora NON è zero-padded, per compatibilità con l'app Android. */
  time: string
  /**
   * Set vinti dalla squadra 1 (quando la partita è a set) o punti del set unico
   * (formato legacy). L'interpretazione dipende da `detail`: se pieno → set,
   * altrimenti euristica per grandezza (`points ≤ 5` = set).
   */
  points1: number
  /** Come `points1`, per la squadra 2. */
  points2: number
  /**
   * Punti dei singoli set, ciascuno come `[puntiT1, puntiT2]`.
   * Vuoto per le partite salvate prima del formato set + detail.
   */
  detail: number[][]
  /**
   * Fase della partita, come codice canonico: GROUP, GROUP_A, QUARTER, SEMIFINAL,
   * FINAL, THIRD. I dati legacy (GIRONE, BRACKET A, FINALE…) vengono normalizzati
   * in lettura da `phases.normalize()`.
   */
  type: string
  /**
   * Sorgente dello slot squadra 1 per le fasi finali (stringa vuota = squadra
   * già nota in `keyTeam1`). Valori: `STANDING` (posizione classifica generale),
   * `GROUP_STANDING` (posizione nel girone), `WINNER`/`LOSER` (esito di una
   * partita sorgente).
   */
  source1Type: string
  /**
   * Riferimento della sorgente 1. Contenuto dipendente da `source1Type`:
   * - `STANDING`: la posizione ("1", "2"…)
   * - `GROUP_STANDING`: girone + posizione ("A1", "B2"…)
   * - `WINNER`/`LOSER`: la key della partita sorgente
   */
  source1Ref: string
  /** Come `source1Type`, per lo slot squadra 2. */
  source2Type: string
  /** Come `source1Ref`, per lo slot squadra 2. */
  source2Ref: string
}

export interface Tournament {
  key: string
  name: string
  nBracket: number
  /** Data del torneo; `null` se assente o non parsabile. */
  date: Date | null
  isValid: boolean
  teams: Team[]
  matches: Match[]
}

export interface LiveMatch {
  active: boolean
  tournamentKey: string
  matchPosition: number
  team1Name: string
  team2Name: string
  team1Players: string
  team2Players: string
  points1: number
  points2: number
  sets1: number
  sets2: number
  timestamp: number
}
