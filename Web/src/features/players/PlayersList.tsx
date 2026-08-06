import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { usePlayers } from '@/hooks/usePlayers'
import { byNameAsc, getVote, matchesQuery } from '@/domain/player'
import type { Player } from '@/domain/models'
import { DB_ROOT } from '@/data/firebase'
import { SearchField } from '@/components/ui/SearchField'
import { ScreenHeader } from '@/components/ui/ScreenHeader'
import { PlayerName } from '@/components/ui/PlayerName'
import { PlayerStatsModal } from './PlayerStatsModal'

/**
 * Prima schermata della migrazione: elenco dei giocatori in sola lettura.
 *
 * Serve a dimostrare che la catena Firebase → mapper → dominio → UI funziona
 * end to end. La schermata definitiva (con selezione, filtri e statistiche)
 * arriva nella fase 2.
 */
export function PlayersList() {
  const navigate = useNavigate()
  const { players, loading, error } = usePlayers()
  const [query, setQuery] = useState('')
  const [inspecting, setInspecting] = useState<Player | null>(null)

  const visible = useMemo(() => {
    const active = players.filter((p) => p.isActive)
    const filtered = query.trim().length > 0 ? active.filter((p) => matchesQuery(p, query)) : active
    return [...filtered].sort(byNameAsc)
  }, [players, query])

  if (loading) {
    return <p className="p-6 text-list-text-secondary">Caricamento giocatori…</p>
  }

  if (error) {
    return (
      <div className="m-6 rounded-lg border border-action-danger/50 bg-action-danger/10 p-4">
        <p className="font-semibold text-action-danger">Errore di connessione</p>
        <p className="mt-1 text-sm text-list-text-secondary">{error.message}</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl p-4">
      <ScreenHeader title="GIOCATORI" onBack={() => navigate('/')} />

      <p className="mb-3 text-sm text-list-text-muted">
        {visible.length} attivi su {players.length} · database{' '}
        <code className="text-list-highlight-text">{DB_ROOT}</code>
      </p>

      <div className="mb-4">
        <SearchField value={query} onChange={setQuery} />
      </div>

      {visible.length === 0 ? (
        <p className="text-list-text-muted">Nessun giocatore trovato.</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {visible.map((player) => (
            <li key={player.key}>
              <button
                type="button"
                onClick={() => setInspecting(player)}
                className="flex w-full items-center justify-between gap-3 rounded-lg border
                           border-list-card-border bg-list-card px-4 py-3 text-left
                           hover:border-brand-blue hover:bg-score-panel"
              >
                <PlayerName player={player} />
                <span className="shrink-0 rounded-full bg-score-panel px-3 py-1 text-sm font-bold tabular-nums">
                  {getVote(player)}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}

      <PlayerStatsModal player={inspecting} onClose={() => setInspecting(null)} />
    </div>
  )
}
