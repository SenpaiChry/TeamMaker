import type { Player } from '@/domain/models'
import { searchBalancedTeams, type GenerationResult } from '@/domain/teamGenerator'

/**
 * Esegue la ricerca delle squadre fuori dal thread principale.
 *
 * Sostituisce l'ExecutorService del Java: `useTeamGenerator` avvia più worker
 * con semi diversi e tiene il primo risultato che arriva. Qui dentro non c'è
 * logica: solo il collegamento fra i messaggi e la funzione pura del dominio.
 */

export interface GenerateRequest {
  players: Player[]
  playersPerTeam: number
  maxDifference: number
  maxRetries: number
  seed: number
  threadIndex: number
}

export type GenerateResponse =
  | { type: 'progress'; retries: number }
  | { type: 'done'; result: GenerationResult }
  | { type: 'exhausted' }
  | { type: 'error'; message: string }

self.onmessage = (event: MessageEvent<GenerateRequest>) => {
  const request = event.data

  try {
    const result = searchBalancedTeams(request.players, {
      playersPerTeam: request.playersPerTeam,
      maxDifference: request.maxDifference,
      maxRetries: request.maxRetries,
      seed: request.seed,
      threadIndex: request.threadIndex,
      onProgress: (retries) => {
        const message: GenerateResponse = { type: 'progress', retries }
        self.postMessage(message)
      },
    })

    const message: GenerateResponse =
      result === null ? { type: 'exhausted' } : { type: 'done', result }
    self.postMessage(message)
  } catch (error) {
    const message: GenerateResponse = {
      type: 'error',
      message: error instanceof Error ? error.message : String(error),
    }
    self.postMessage(message)
  }
}
