import { useEffect, useState } from 'react'
import type { LiveMatch } from '@/domain/models'
import { subscribeToLiveMatch } from '@/data/liveMatchRepo'

export interface LiveMatchState {
  live: LiveMatch | null
  /** true solo se esiste un nodo live con `active` a true. */
  isLive: boolean
  loading: boolean
}

/** Partita in diretta, aggiornata in tempo reale. */
export function useLiveMatch(): LiveMatchState {
  const [live, setLive] = useState<LiveMatch | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const unsubscribe = subscribeToLiveMatch(
      (next) => {
        setLive(next)
        setLoading(false)
      },
      () => setLoading(false),
    )
    return unsubscribe
  }, [])

  return { live, isLive: live?.active === true, loading }
}
