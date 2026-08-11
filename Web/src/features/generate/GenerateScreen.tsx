import { useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { usePlayers } from '@/hooks/usePlayers'
import { byNameAsc, matchesQuery } from '@/domain/player'
import { resolveSelected, useSelectionStore } from '@/store/selectionStore'
import { SearchField } from '@/components/ui/SearchField'
import { Button } from '@/components/ui/Button'
import { PlayerCard } from '@/components/ui/PlayerCard'
import { SelectAllButton } from './SelectAllButton'
import { GenerateOptionsModal } from './GenerateOptionsModal'
import { ScreenHeader } from '@/components/ui/ScreenHeader'

/**
 * Selezione dei giocatori per la generazione delle squadre.
 * Porta ActivityGenerate + PlayerGenerateAdapter.
 */
export function GenerateScreen() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  // Se la generazione è per un nuovo torneo, propaghiamo il flag a `/squadre`
  // così mostra scarto, voti e tasto di salvataggio. Su ogni altra provenienza
  // (Home) la vista resta minimale.
  const contesto = params.get('contesto')
  const { players, loading, error } = usePlayers()
  const [query, setQuery] = useState('')
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
        <SelectAllButton
          allSelected={allSelected}
          onClick={() => (allSelected ? clear() : selectAll(active.map((p) => p.key)))}
        />
      </div>

      <p className="mb-3 text-sm text-list-text-muted">
        <span className="font-bold text-list-highlight-text">{selectedCount}</span> selezionati su{' '}
        {active.length} attivi
      </p>

      {visible.length === 0 ? (
        <p className="text-list-text-muted">Nessun giocatore trovato.</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {visible.map((player) => (
            <li key={player.key}>
              <PlayerCard
                player={player}
                selected={selectedKeys.has(player.key)}
                onToggle={() => toggle(player.key)}
              />
            </li>
          ))}
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

      <GenerateOptionsModal
        open={choosingSize}
        selectedCount={selectedCount}
        onClose={() => setChoosingSize(false)}
        onConfirm={(playersPerTeam) => {
          setChoosingSize(false)
          const query = new URLSearchParams({ perSquadra: String(playersPerTeam) })
          if (contesto !== null) query.set('contesto', contesto)
          navigate(`/squadre?${query.toString()}`)
        }}
      />
    </div>
  )
}
