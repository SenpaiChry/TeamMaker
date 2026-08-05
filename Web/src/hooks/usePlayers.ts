import { useEffect, useState } from 'react'
import type { Player } from '@/domain/models'
import { subscribeToPlayers } from '@/data/playersRepo'

export interface PlayersState {
  players: Player[]
  loading: boolean
  error: Error | null
}

/**
 * Elenco dei giocatori, aggiornato in tempo reale.
 *
 * Sostituisce `Constants.players` + il listener di `PlayerUtility`: lo stato
 * vive nel componente invece che in una variabile statica globale, e l'ascolto
 * si chiude da solo quando il componente viene smontato.
 */
export function usePlayers(): PlayersState {
  const [state, setState] = useState<PlayersState>({
    players: [],
    loading: true,
    error: null,
  })

  useEffect(() => {
    const unsubscribe = subscribeToPlayers(
      (players) => setState({ players, loading: false, error: null }),
      (error) => setState({ players: [], loading: false, error }),
    )
    return unsubscribe
  }, [])

  return state
}
