import { useCallback, useEffect, useRef, useState } from 'react'
import type { Player } from '@/domain/models'
import type { GenerationResult } from '@/domain/teamGenerator'
import type { GenerateRequest, GenerateResponse } from '@/workers/teamGenerator.worker'

/**
 * Lancia la ricerca delle squadre su più Web Worker e tiene il primo risultato.
 *
 * Sostituisce l'ExecutorService del Java: lì N thread condividevano un
 * AtomicBoolean, qui N worker indipendenti corrono con semi diversi e il primo
 * che finisce vince; gli altri vengono terminati.
 */

const MAX_RETRIES = 50_000

/** Un worker per core, entro limiti ragionevoli. */
function workerCount(): number {
  const cores = navigator.hardwareConcurrency
  return Math.min(Math.max(typeof cores === 'number' ? cores : 4, 2), 8)
}

export interface GeneratorState {
  running: boolean
  result: GenerationResult | null
  /** Tentativi del worker più avanti: dà un progresso reale invece di uno spinner. */
  retries: number
  error: string | null
}

const IDLE: GeneratorState = { running: false, result: null, retries: 0, error: null }

export function useTeamGenerator() {
  const [state, setState] = useState<GeneratorState>(IDLE)
  const workersRef = useRef<Worker[]>([])

  const terminateAll = useCallback(() => {
    for (const worker of workersRef.current) worker.terminate()
    workersRef.current = []
  }, [])

  useEffect(() => terminateAll, [terminateAll])

  const generate = useCallback(
    (players: Player[], playersPerTeam: number) => {
      terminateAll()
      setState({ running: true, result: null, retries: 0, error: null })

      const count = workerCount()
      let finished = 0

      for (let i = 0; i < count; i++) {
        const worker = new Worker(new URL('../workers/teamGenerator.worker.ts', import.meta.url), {
          type: 'module',
        })

        worker.onmessage = (event: MessageEvent<GenerateResponse>) => {
          const message = event.data

          if (message.type === 'progress') {
            // Mostra il worker più avanti, non l'ultimo che ha parlato.
            setState((s) =>
              s.running && message.retries > s.retries ? { ...s, retries: message.retries } : s,
            )
            return
          }

          if (message.type === 'done') {
            terminateAll()
            setState({ running: false, result: message.result, retries: message.result.retries, error: null })
            return
          }

          if (message.type === 'error') {
            terminateAll()
            setState({ running: false, result: null, retries: 0, error: message.message })
            return
          }

          // 'exhausted': si arrende solo se si sono arresi tutti.
          finished++
          if (finished === count) {
            terminateAll()
            setState({
              running: false,
              result: null,
              retries: 0,
              error:
                'Nessuna combinazione trovata: prova a cambiare il numero di giocatori per squadra.',
            })
          }
        }

        const request: GenerateRequest = {
          players,
          playersPerTeam,
          // L'app Android forza la precisione massima: la tolleranza parte
          // comunque dalla deviazione standard e si allarga a ogni tentativo.
          maxDifference: 0,
          maxRetries: MAX_RETRIES,
          seed: Date.now() + i * 7919,
          threadIndex: i,
        }
        worker.postMessage(request)
        workersRef.current.push(worker)
      }
    },
    [terminateAll],
  )

  const reset = useCallback(() => {
    terminateAll()
    setState(IDLE)
  }, [terminateAll])

  return { ...state, generate, reset, workerCount: workerCount() }
}
