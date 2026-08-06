import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { usePlayers } from '@/hooks/usePlayers'
import { getVote, matchesQuery } from '@/domain/player'
import type { Player } from '@/domain/models'
import { deletePlayer, setPlayerActive } from '@/data/playersRepo'
import { SearchField } from '@/components/ui/SearchField'
import { ScreenHeader } from '@/components/ui/ScreenHeader'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { PlayerStatsModal } from '@/features/players/PlayerStatsModal'
import { PlayerEditModal } from './PlayerEditModal'
import { PlayerAdminRow } from './PlayerAdminRow'

/**
 * Anagrafica giocatori. Porta ActivityAdmin + PlayerAdminAdapter.
 *
 * Come nell'originale attivi e archiviati stanno in UNA sola lista ordinata
 * per voto decrescente: gli archiviati si riconoscono perché sono spenti.
 * Serviva a vedere subito dove si colloca un giocatore rispetto agli altri,
 * cosa che due schede separate rendevano impossibile.
 */
export function PlayersAdminScreen() {
  const navigate = useNavigate()
  const { players, loading } = usePlayers()
  const [query, setQuery] = useState('')

  const [editing, setEditing] = useState<Player | null>(null)
  const [editorOpen, setEditorOpen] = useState(false)
  const [inspecting, setInspecting] = useState<Player | null>(null)
  const [toDelete, setToDelete] = useState<Player | null>(null)
  const [toArchive, setToArchive] = useState<Player | null>(null)

  const visible = useMemo(() => {
    const filtered = query.trim().length > 0 ? players.filter((p) => matchesQuery(p, query)) : players
    return [...filtered].sort((a, b) => getVote(b) - getVote(a))
  }, [players, query])

  const archivedCount = players.filter((p) => !p.isActive).length

  if (loading) return <p className="p-6 text-list-text-secondary">Caricamento…</p>

  return (
    <div className="mx-auto max-w-2xl p-4 pb-28">
      <ScreenHeader title="Giocatori" onBack={() => navigate('/admin')} />

      <div className="mb-3">
        <SearchField value={query} onChange={setQuery} />
      </div>

      <p className="mb-3 text-sm text-list-text-muted">
        {players.length} giocatori · {archivedCount} archiviati · ordinati per voto
      </p>

      {visible.length === 0 ? (
        <p className="text-list-text-muted">Nessun giocatore.</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {visible.map((player) => (
            <li key={player.key}>
              <PlayerAdminRow
                player={player}
                onEdit={() => {
                  setEditing(player)
                  setEditorOpen(true)
                }}
                onToggleActive={() => setToArchive(player)}
                onDelete={() => setToDelete(player)}
                onInfo={() => setInspecting(player)}
              />
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
            Nuovo giocatore
          </Button>
        </div>
      </div>

      <PlayerEditModal player={editing} open={editorOpen} onClose={() => setEditorOpen(false)} />
      <PlayerStatsModal player={inspecting} onClose={() => setInspecting(null)} />

      <ConfirmDialog
        open={toArchive !== null}
        title={
          toArchive?.isActive === true
            ? `Archiviare ${toArchive.name}?`
            : `Riattivare ${toArchive?.name ?? ''}?`
        }
        message={
          toArchive?.isActive === true
            ? 'Non comparirà più fra i giocatori selezionabili, ma resta nei tornei passati.'
            : 'Tornerà selezionabile per la generazione delle squadre.'
        }
        confirmLabel={toArchive?.isActive === true ? 'Archivia' : 'Riattiva'}
        onConfirm={() => {
          if (toArchive !== null) void setPlayerActive(toArchive.key, !toArchive.isActive)
          setToArchive(null)
        }}
        onCancel={() => setToArchive(null)}
      />

      <ConfirmDialog
        open={toDelete !== null}
        title={`Eliminare ${toDelete?.name ?? ''}?`}
        message="L'eliminazione è definitiva. I tornei passati resteranno con una squadra incompleta: se il giocatore non gioca più, conviene archiviarlo."
        confirmLabel="Elimina"
        onConfirm={() => {
          if (toDelete !== null) void deletePlayer(toDelete.key)
          setToDelete(null)
        }}
        onCancel={() => setToDelete(null)}
      />
    </div>
  )
}
