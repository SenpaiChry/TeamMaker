import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { usePlayers } from '@/hooks/usePlayers'
import { byNameAsc, getVote, matchesQuery } from '@/domain/player'
import type { Player } from '@/domain/models'
import { resolveSelected, useSelectionStore } from '@/store/selectionStore'
import { SearchField } from '@/components/ui/SearchField'
import { Button } from '@/components/ui/Button'
import { PlayerName } from '@/components/ui/PlayerName'
import { PlayerStatsModal } from '@/features/players/PlayerStatsModal'
import { GenerateOptionsModal } from './GenerateOptionsModal'
import { ScreenHeader } from '@/components/ui/ScreenHeader'

/**
 * Selezione dei giocatori per la generazione delle squadre.
 * Porta ActivityGenerate + PlayerGenerateAdapter.
 */
export function GenerateScreen() {
  const navigate = useNavigate()
  const { players, loading, error } = usePlayers()
  const [query, setQuery] = useState('')
  const [inspecting, setInspecting] = useState<Player | null>(null)
  const [choosingSize, setChoosingSize] = useState(false)

  const selectedKeys = useSelectionStore((s) => s.selectedKeys)
  const toggle = useSelectionStore((s) => s.toggle)
  const selectAll = useSelectionStore((s) => s.selectAll)
  const clear = useSelectionStore((s) => s.clear)

  const active = useMemo(() => players.filter((p) => p.isActive).sort(byNameAsc), [players])

  const visible = useMemo(
    () => (query.trim().length > 0 ? active.filter((p) => matchesQuery(p, query)) : active),
    [active, query],
  )

  const selectedCount = useMemo(
    () => resolveSelected(active, selectedKeys).length,
    [active, selectedKeys],
  )
  const allSelected = selectedCount > 0 && selectedCount === active.length

  if (loading) return <p className="p-6 text-list-text-secondary">Caricamento giocatori…</p>
  if (error !== null) {
    return (
      <div className="m-6 rounded-lg border border-action-danger/50 bg-action-danger/10 p-4">
        <p className="font-semibold text-action-danger">Errore di connessione</p>
        <p className="mt-1 text-sm text-list-text-secondary">{error.message}</p>
      </div>
    )
  }

  return (
    <div className="mx-auto flex min-h-dvh max-w-2xl flex-col p-4 pb-28">
      <ScreenHeader title="GENERA SQUADRE" onBack={() => navigate('/')} />

      <div className="mb-3 flex items-center gap-2">
        <div className="grow">
          <SearchField value={query} onChange={setQuery} />
        </div>
        <Button
          variant="ghost"
          onClick={() => (allSelected ? clear() : selectAll(active.map((p) => p.key)))}
          className="shrink-0 whitespace-nowrap px-3 py-2 text-sm"
        >
          {allSelected ? 'DESELEZIONA' : 'SELEZIONA'} TUTTI
        </Button>
      </div>

      <p className="mb-3 text-sm text-list-text-muted">
        <span className="font-bold text-list-highlight-text">{selectedCount}</span> selezionati su{' '}
        {active.length} attivi
      </p>

      {visible.length === 0 ? (
        <p className="text-list-text-muted">Nessun giocatore trovato.</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {visible.map((player) => {
            const selected = selectedKeys.has(player.key)
            return (
              <li key={player.key}>
                <div
                  className={`flex items-center gap-3 rounded-lg border px-3 py-2 transition ${
                    selected
                      ? 'border-player-selected-border bg-player-selected/30'
                      : 'border-list-card-border bg-list-card'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => toggle(player.key)}
                    aria-pressed={selected}
                    className="flex min-w-0 grow items-center gap-3 text-left"
                  >
                    <span
                      className={`grid size-8 shrink-0 place-items-center rounded-full text-sm font-bold ${
                        player.gender === 'F'
                          ? 'bg-women text-black'
                          : 'bg-men-icon text-black'
                      }`}
                      aria-hidden
                    >
                      {player.gender}
                    </span>
                    <PlayerName player={player} />
                  </button>

                  <button
                    type="button"
                    onClick={() => setInspecting(player)}
                    aria-label={`Statistiche di ${player.name}`}
                    className="shrink-0 rounded-full bg-score-panel px-3 py-1 text-sm
                               font-bold tabular-nums hover:bg-score-panel-border"
                  >
                    {getVote(player)}
                  </button>
                </div>
              </li>
            )
          })}
        </ul>
      )}

      <div
        className="fixed inset-x-0 bottom-0 border-t border-list-card-border
                   bg-score-bg-bottom/95 p-4 backdrop-blur"
      >
        <div className="mx-auto max-w-2xl">
          <Button
            onClick={() => setChoosingSize(true)}
            disabled={selectedCount < 3}
            className="w-full"
          >
            CREA SQUADRE
          </Button>
        </div>
      </div>

      <PlayerStatsModal player={inspecting} onClose={() => setInspecting(null)} />
      <GenerateOptionsModal
        open={choosingSize}
        selectedCount={selectedCount}
        onClose={() => setChoosingSize(false)}
        onConfirm={(playersPerTeam) => {
          setChoosingSize(false)
          navigate(`/squadre?perSquadra=${playersPerTeam}`)
        }}
      />
    </div>
  )
}
