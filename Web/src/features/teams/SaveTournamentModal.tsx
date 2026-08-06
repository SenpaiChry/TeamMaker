import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import type { Team } from '@/domain/models'
import { createTournament } from '@/data/tournamentsRepo'
import { useTournaments } from '@/hooks/useTournaments'

/**
 * Salva le squadre appena generate come nuovo torneo, che diventa quello attivo.
 * Porta ActivityPopUpSaveNewTournament.
 */
export function SaveTournamentModal({
  teams,
  open,
  onClose,
}: {
  teams: Team[]
  open: boolean
  onClose: () => void
}) {
  const navigate = useNavigate()
  const { tournaments } = useTournaments()
  const [name, setName] = useState('')
  const [date, setDate] = useState(todayInputValue)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const active = tournaments.find((t) => t.isValid)

  const save = async () => {
    setBusy(true)
    setError(null)
    try {
      await createTournament(tournaments, name.trim(), new Date(date), teams)
      onClose()
      navigate('/torneo')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Salvataggio non riuscito.')
      setBusy(false)
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={<h2 className="text-lg font-bold tracking-wide">SALVA COME TORNEO</h2>}
    >
      <label className="flex flex-col gap-1">
        <span className="text-xs uppercase tracking-wide text-list-text-muted">Nome *</span>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Torneo di settembre"
          className="rounded-lg border border-list-card-border bg-list-card px-3 py-2
                     focus:border-list-highlight-text focus:outline-none"
        />
      </label>

      <label className="mt-3 flex flex-col gap-1">
        <span className="text-xs uppercase tracking-wide text-list-text-muted">Data</span>
        <input
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          className="rounded-lg border border-list-card-border bg-list-card px-3 py-2
                     focus:border-list-highlight-text focus:outline-none"
        />
      </label>

      <p className="mt-3 text-sm text-list-text-muted">
        {teams.length} squadre. Il torneo diventerà quello attivo
        {active !== undefined && (
          <>
            , al posto di{' '}
            <b>{active.name.length > 0 ? active.name : 'quello senza nome'}</b>
          </>
        )}
        . Il calendario si genera dopo, dalla gestione tornei.
      </p>

      {error !== null && <p className="mt-2 text-sm text-action-danger">{error}</p>}

      <div className="mt-4 flex gap-2">
        <Button variant="ghost" onClick={onClose} className="grow">
          ANNULLA
        </Button>
        <Button
          variant="confirm"
          onClick={save}
          disabled={busy || name.trim().length === 0}
          className="grow"
        >
          {busy ? 'SALVATAGGIO…' : 'SALVA'}
        </Button>
      </div>
    </Modal>
  )
}

function todayInputValue(): string {
  const d = new Date()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}
