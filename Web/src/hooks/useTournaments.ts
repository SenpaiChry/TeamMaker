import { useEffect, useMemo, useState } from 'react'
import type { Tournament } from '@/domain/models'
import { subscribeToTournaments } from '@/data/tournamentsRepo'
import { usePlayers } from './usePlayers'

export interface TournamentsState {
  tournaments: Tournament[]
  /** Il torneo con `is_valid` a true: ce n'è al massimo uno. */
  active: Tournament | null
  loading: boolean
  error: Error | null
}

/**
 * Tornei aggiornati in tempo reale, con i giocatori già risolti dentro le squadre.
 *
 * Sostituisce `Constants.tournaments` + il listener di `TournamentUtility`.
 */
export function useTournaments(): TournamentsState {
  const { players, loading: loadingPlayers, error: playersError } = usePlayers()
  const [tournaments, setTournaments] = useState<Tournament[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  // I tornei vanno riletti quando cambia l'anagrafica: le squadre contengono
  // i giocatori risolti, non le sole chiavi.
  const playersByKey = useMemo(() => new Map(players.map((p) => [p.key, p])), [players])

  useEffect(() => {
    if (loadingPlayers) return

    const unsubscribe = subscribeToTournaments(
      playersByKey,
      (next) => {
        setTournaments(next)
        setLoading(false)
        setError(null)
      },
      (err) => {
        setError(err)
        setLoading(false)
      },
    )
    return unsubscribe
  }, [playersByKey, loadingPlayers])

  const active = useMemo(() => tournaments.find((t) => t.isValid) ?? null, [tournaments])

  return {
    tournaments,
    active,
    loading: loading || loadingPlayers,
    error: error ?? playersError,
  }
}
