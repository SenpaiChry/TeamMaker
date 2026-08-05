import { useMemo, useState } from 'react'
import { usePlayers } from '@/hooks/usePlayers'
import { byNameAsc, getVote, matchesQuery } from '@/domain/player'
import { DB_ROOT } from '@/data/firebase'

/**
 * Prima schermata della migrazione: elenco dei giocatori in sola lettura.
 *
 * Serve a dimostrare che la catena Firebase → mapper → dominio → UI funziona
 * end to end. La schermata definitiva (con selezione, filtri e statistiche)
 * arriva nella fase 2.
 */
export function PlayersList() {
  const { players, loading, error } = usePlayers()
  const [query, setQuery] = useState('')

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
      <header className="mb-4">
        <h1 className="text-2xl font-bold tracking-wide">GIOCATORI</h1>
        <p className="text-sm text-list-text-muted">
          {visible.length} attivi su {players.length} · database{' '}
          <code className="text-list-highlight-text">{DB_ROOT}</code>
        </p>
      </header>

      <input
        type="search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Cerca giocatore…"
        className="mb-4 w-full rounded-lg border border-list-card-border bg-list-card px-4 py-2
                   text-list-text placeholder:text-list-text-muted
                   focus:border-list-highlight-text focus:outline-none"
      />

      {visible.length === 0 ? (
        <p className="text-list-text-muted">Nessun giocatore trovato.</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {visible.map((player) => (
            <li
              key={player.key}
              className="flex items-center justify-between rounded-lg border
                         border-list-card-border bg-list-card px-4 py-3"
            >
              <span>
                <span
                  className={
                    player.gender === 'F' ? 'font-semibold text-women' : 'font-semibold text-men-dark'
                  }
                >
                  {player.name}
                </span>{' '}
                <span className="text-list-text-secondary">{player.surname}</span>
                {player.nickname.length > 0 && (
                  <span className="ml-2 text-sm text-list-text-muted">«{player.nickname}»</span>
                )}
              </span>
              <span className="rounded-full bg-score-panel px-3 py-1 text-sm font-bold tabular-nums">
                {getVote(player)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
