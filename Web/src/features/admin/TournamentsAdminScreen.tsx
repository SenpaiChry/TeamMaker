import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTournaments } from '@/hooks/useTournaments'
import type { Tournament } from '@/domain/models'
import {
  deactivateAllTournaments,
  deleteTournament,
  setActiveTournament,
  updateTournamentDetails,
} from '@/data/tournamentsRepo'
import { deleteAllMatches } from '@/data/matchesRepo'
import { ScreenHeader } from '@/components/ui/ScreenHeader'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Modal } from '@/components/ui/Modal'
import { CalendarModal } from './CalendarModal'

/**
 * Gestione dei tornei: attivazione, rinomina, calendario, eliminazione.
 * Porta TournamentActivityManageTournaments + ActivityPopUpManageTournament.
 */
export function TournamentsAdminScreen() {
  const navigate = useNavigate()
  const { tournaments, loading } = useTournaments()

  const [editing, setEditing] = useState<Tournament | null>(null)
  const [calendarFor, setCalendarFor] = useState<Tournament | null>(null)
  const [toDelete, setToDelete] = useState<Tournament | null>(null)
  const [toClearCalendar, setToClearCalendar] = useState<Tournament | null>(null)

  if (loading) return <p className="p-6 text-list-text-secondary">Caricamento tornei…</p>

  const sorted = [...tournaments].sort((a, b) => {
    if (a.isValid !== b.isValid) return a.isValid ? -1 : 1
    return (b.date?.getTime() ?? 0) - (a.date?.getTime() ?? 0)
  })

  return (
    <div className="mx-auto max-w-2xl p-4">
      <ScreenHeader title="TORNEI" onBack={() => navigate('/admin')} />

      {sorted.length === 0 ? (
        <p className="text-list-text-muted">
          Nessun torneo. Creane uno generando delle squadre e salvandole.
        </p>
      ) : (
        <ul className="flex flex-col gap-3">
          {sorted.map((tournament) => (
            <li
              key={tournament.key}
              className={`rounded-lg border p-3 ${
                tournament.isValid
                  ? 'border-list-card-highlight-border bg-list-card-highlight'
                  : 'border-list-card-border bg-list-card'
              }`}
            >
              <div className="mb-2 flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <h2 className="truncate font-bold">
                    {tournament.name.length > 0 ? tournament.name : 'Senza nome'}
                  </h2>
                  <p className="text-sm text-list-text-muted">
                    {tournament.date?.toLocaleDateString('it') ?? 'senza data'} ·{' '}
                    {tournament.teams.length} squadre · {tournament.matches.length} partite
                  </p>
                </div>
                {tournament.isValid && (
                  <span className="shrink-0 rounded bg-bracket-header px-2 py-0.5 text-xs font-bold text-bracket-header-text">
                    ATTIVO
                  </span>
                )}
              </div>

              <div className="flex flex-wrap gap-2">
                {tournament.isValid ? (
                  <SmallButton onClick={() => void deactivateAllTournaments(tournaments)}>
                    disattiva
                  </SmallButton>
                ) : (
                  <SmallButton onClick={() => void setActiveTournament(tournaments, tournament.key)}>
                    attiva
                  </SmallButton>
                )}
                <SmallButton onClick={() => setEditing(tournament)}>rinomina</SmallButton>
                <SmallButton onClick={() => navigate(`/admin/tornei/${tournament.key}`)}>
                  squadre e partite
                </SmallButton>
                <SmallButton onClick={() => setCalendarFor(tournament)}>calendario</SmallButton>
                {tournament.matches.length > 0 && (
                  <SmallButton onClick={() => setToClearCalendar(tournament)}>
                    azzera calendario
                  </SmallButton>
                )}
                <SmallButton onClick={() => setToDelete(tournament)} danger>
                  elimina
                </SmallButton>
              </div>
            </li>
          ))}
        </ul>
      )}

      {editing !== null && (
        <RenameModal tournament={editing} onClose={() => setEditing(null)} />
      )}

      {calendarFor !== null && (
        <CalendarModal
          tournament={calendarFor}
          open
          onClose={() => setCalendarFor(null)}
        />
      )}

      <ConfirmDialog
        open={toClearCalendar !== null}
        title="Azzerare il calendario?"
        message="Tutte le partite e i gironi verranno cancellati. Le squadre restano."
        confirmLabel="AZZERA"
        onConfirm={() => {
          if (toClearCalendar !== null) {
            void deleteAllMatches(
              toClearCalendar.key,
              toClearCalendar.teams.map((t) => t.key),
            )
          }
          setToClearCalendar(null)
        }}
        onCancel={() => setToClearCalendar(null)}
      />

      <ConfirmDialog
        open={toDelete !== null}
        title={`Eliminare "${toDelete?.name ?? ''}"?`}
        message="Squadre, calendario e risultati verranno cancellati definitivamente."
        confirmLabel="ELIMINA"
        onConfirm={() => {
          if (toDelete !== null) void deleteTournament(toDelete.key)
          setToDelete(null)
        }}
        onCancel={() => setToDelete(null)}
      />
    </div>
  )
}

function RenameModal({ tournament, onClose }: { tournament: Tournament; onClose: () => void }) {
  const [name, setName] = useState(tournament.name)
  const [date, setDate] = useState(toInputDate(tournament.date))
  const [busy, setBusy] = useState(false)

  const save = async () => {
    setBusy(true)
    try {
      await updateTournamentDetails(tournament.key, name.trim(), new Date(date))
      onClose()
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={<h2 className="text-lg font-bold tracking-wide">RINOMINA TORNEO</h2>}
    >
      <label className="flex flex-col gap-1">
        <span className="text-xs uppercase tracking-wide text-list-text-muted">Nome</span>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
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
          SALVA
        </Button>
      </div>
    </Modal>
  )
}

/** Data nel formato richiesto da <input type="date">. */
function toInputDate(date: Date | null): string {
  const d = date ?? new Date()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

function SmallButton({
  onClick,
  danger = false,
  children,
}: {
  onClick: () => void
  danger?: boolean
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-lg bg-icon-action px-3 py-1.5 text-sm hover:brightness-150 ${
        danger ? 'text-action-delete' : 'text-list-text'
      }`}
    >
      {children}
    </button>
  )
}
