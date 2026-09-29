import { useEffect, useState } from 'react'
import type { LiveMatch } from '@/domain/models'
import { clearLiveMatch, subscribeToLiveMatch } from '@/data/liveMatchRepo'

export interface LiveMatchState {
  live: LiveMatch | null
  /** true solo se esiste un nodo live con `active` a true e non è stale. */
  isLive: boolean
  loading: boolean
}

/**
 * Soglia oltre la quale una diretta viene considerata "abbandonata".
 * Porta `LiveMatchTimer.STALE_THRESHOLD_MS` (Android 51a21fd): se il segnapunti
 * non aggiorna il timestamp da 5 minuti, chi era in campo probabilmente ha
 * chiuso l'app o perso la rete senza far scattare `onDisconnect`.
 */
const STALE_THRESHOLD_MS = 5 * 60 * 1000

function isStale(live: LiveMatch | null): boolean {
  if (live === null || !live.active) return false
  if (live.timestamp <= 0) return false
  return Date.now() - live.timestamp > STALE_THRESHOLD_MS
}

/** Partita in diretta, aggiornata in tempo reale. */
export function useLiveMatch(): LiveMatchState {
  const [live, setLive] = useState<LiveMatch | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const unsubscribe = subscribeToLiveMatch(
      (next) => {
        // Diretta stale: consideriamola già spenta in UI e chiediamo a
        // Firebase di allineare lo stato (`active=false`). Un crash del
        // segnapunti aveva già dovuto lasciare tutto pulito via `onDisconnect`,
        // ma quello non parte se la scheda muore prima del beacon: qui il
        // primo client che se ne accorge ripulisce il nodo per tutti.
        if (isStale(next)) {
          void clearLiveMatch()
          setLive(next === null ? null : { ...next, active: false })
        } else {
          setLive(next)
        }
        setLoading(false)
      },
      () => setLoading(false),
    )
    return unsubscribe
  }, [])

  return { live, isLive: live?.active === true, loading }
}
