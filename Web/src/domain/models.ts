import type { StatKey } from './constants'

/**
 * Modelli di dominio, portati da Player/Team/Match/Tournament.java.
 *
 * Sono strutture dati immutabili e senza metodi: il comportamento sta nelle
 * funzioni pure dei moduli accanto. Nessun campo qui rispecchia le stranezze
 * di serializzazione del database (numeri come stringhe, ecc.): quelle vivono
 * solo in `src/data/mappers.ts`.
 */

export type Gender = 'M' | 'F'

export type Stats = Record<StatKey, number>

export interface Player {
  key: string
  name: string
  surname: string
  nickname: string
  gender: Gender
  isActive: boolean
  stats: Stats
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
  points1: number
  points2: number
  /** "GIRONE " per il girone all'italiana, "BRACKET A" ecc. per i gironi multipli. */
  type: string
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
