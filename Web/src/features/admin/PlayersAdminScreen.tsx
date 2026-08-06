import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { usePlayers } from '@/hooks/usePlayers'
import { byNameAsc, getVote, matchesQuery } from '@/domain/player'
import type { Player } from '@/domain/models'
import { deletePlayer, setPlayerActive } from '@/data/playersRepo'
import { SearchField } from '@/components/ui/SearchField'
import { ScreenHeader } from '@/components/ui/ScreenHeader'
import { PlayerName } from '@/components/ui/PlayerName'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { PlayerEditModal } from './PlayerEditModal'

/**
 * Anagrafica giocatori: creazione, modifica, archiviazione, eliminazione.
 * Porta ActivityAdmin + PlayerAdminAdapter.
 */
export function PlayersAdminScreen() {
  const navigate = useNavigate()
  const { players, loading } = usePlayers()
  const [query, setQuery] = useState('')
  const [showArchived, setShowArchived] = useState(false)
  const [editing, setEditing] = useState<Player | null>(null)
  const [editorOpen, setEditorOpen] = useState(false)
  const [toDelete, setToDelete] = useState<Player | null>(null)

  const visible = useMemo(() => {
    const byState = players.filter((p) => p.isActive !== showArchived)
    const filtered = query.trim().length > 0 ? byState.filter((p) => matchesQuery(p, query)) : byState
    return [...filtered].sort(byNameAsc)
  }, [players, query, showArchived])

  const archivedCount = players.filter((p) => !p.isActive).length

  if (loading) return <p className="p-6 text-list-text-secondary">Caricamento…</p>

  return (
    <div className="mx-auto max-w-2xl p-4 pb-28">
      <ScreenHeader title="GIOCATORI" onBack={() => navigate('/admin')} />

      <div className="mb-3 grid grid-cols-2 gap-1 rounded-lg border border-list-card-border bg-list-card p-1">
        <TabButton active={!showArchived} onClick={() => setShowArchived(false)}>
          ATTIVI ({players.length - archivedCount})
        </TabButton>
        <TabButton active={showArchived} onClick={() => setShowArchived(true)}>
          ARCHIVIATI ({archivedCount})
        </TabButton>
      </div>

      <div className="mb-4">
        <SearchField value={query} onChange={setQuery} />
      </div>

      {visible.length === 0 ? (
        <p className="text-list-text-muted">Nessun giocatore.</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {visible.map((player) => (
            <li
              key={player.key}
              className={`flex items-center gap-2 rounded-lg border px-3 py-2 ${
                player.isActive
                  ? 'border-list-card-border bg-list-card'
                  : 'border-player-archived-border bg-player-archived'
              }`}
            >
              <span className="min-w-0 grow">
                <PlayerName player={player} />
              </span>

              <span className="shrink-0 rounded-full bg-score-panel px-2 py-1 text-sm font-bold tabular-nums">
                {getVote(player)}
              </span>

              <IconButton
                label={`Modifica ${player.name}`}
                onClick={() => {
                  setEditing(player)
                  setEditorOpen(true)
                }}
              >
                ✎
              </IconButton>

              <IconButton
                label={player.isActive ? `Archivia ${player.name}` : `Riattiva ${player.name}`}
                onClick={() => void setPlayerActive(player.key, !player.isActive)}
              >
                {player.isActive ? '↓' : '↑'}
              </IconButton>

              <IconButton label={`Elimina ${player.name}`} onClick={() => setToDelete(player)} danger>
                ✕
              </IconButton>
            </li>
          ))}
        </ul>
      )}

      <div className="fixed inset-x-0 bottom-0 border-t border-list-card-border bg-score-bg-bottom/95 p-4 backdrop-blur">
        <div className="mx-auto max-w-2xl">
          <Button
            onClick={() => {
              setEditing(null)
              setEditorOpen(true)
            }}
            className="w-full"
          >
            NUOVO GIOCATORE
          </Button>
        </div>
      </div>

      <PlayerEditModal player={editing} open={editorOpen} onClose={() => setEditorOpen(false)} />

      <ConfirmDialog
        open={toDelete !== null}
        title={`Eliminare ${toDelete?.name ?? ''}?`}
        message="L'eliminazione è definitiva. I tornei passati resteranno con una squadra incompleta: se il giocatore non gioca più, conviene archiviarlo."
        confirmLabel="ELIMINA"
        onConfirm={() => {
          if (toDelete !== null) void deletePlayer(toDelete.key)
          setToDelete(null)
        }}
        onCancel={() => setToDelete(null)}
      />
    </div>
  )
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded px-2 py-2 text-sm font-bold tracking-wide transition ${
        active ? 'bg-action-selected text-white' : 'text-list-text-secondary hover:text-list-text'
      }`}
    >
      {children}
    </button>
  )
}

function IconButton({
  label,
  onClick,
  danger = false,
  children,
}: {
  label: string
  onClick: () => void
  danger?: boolean
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      title={label}
      className={`grid size-8 shrink-0 place-items-center rounded-lg bg-icon-action
                  hover:brightness-150 ${danger ? 'text-action-delete' : 'text-list-text'}`}
    >
      {children}
    </button>
  )
}
