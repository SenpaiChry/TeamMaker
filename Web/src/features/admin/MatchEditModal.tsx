import { useEffect, useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import type { Match, Tournament } from '@/domain/models'
import { getTeamNumber } from '@/domain/team'
import { formatFullNames } from '@/domain/team'
import { availablePhases, formatPhase } from '@/domain/phases'
import { isValidTime } from '@/domain/time'
import { addMatch, updateMatch } from '@/data/matchesRepo'

/**
 * Creazione e modifica di una partita.
 * Porta TournamentActivityEditMatch: giornata, orario, le due squadre, la
 * fase e — solo in modifica — il punteggio.
 */
export function MatchEditModal({
  tournament,
  match,
  open,
  onClose,
}: {
  tournament: Tournament
  /** `null` per creare una nuova partita. */
  match: Match | null
  open: boolean
  onClose: () => void
}) {
  const [day, setDay] = useState(1)
  const [time, setTime] = useState('9:00')
  const [team1, setTeam1] = useState('')
  const [team2, setTeam2] = useState('')
  const [phase, setPhase] = useState('')
  const [points1, setPoints1] = useState(0)
  const [points2, setPoints2] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const phases = availablePhases(tournament.nBracket)

  useEffect(() => {
    if (!open) return
    setDay(match?.day ?? 1)
    setTime(match?.time ?? '9:00')
    setTeam1(match?.keyTeam1 ?? '')
    setTeam2(match?.keyTeam2 ?? '')
    setPhase(match?.type ?? '')
    setPoints1(match?.points1 ?? 0)
    setPoints2(match?.points2 ?? 0)
    setError(null)
  }, [open, match])

  const sameTeam = team1 !== '' && team1 === team2
  const valid =
    isValidTime(time) && team1 !== '' && team2 !== '' && !sameTeam && phase !== '' && day >= 1

  const save = async () => {
    setBusy(true)
    setError(null)
    try {
      const payload = {
        keyTeam1: team1,
        keyTeam2: team2,
        day,
        time,
        points1,
        points2,
        type: phase,
      }

      if (match === null) await addMatch(tournament.key, payload)
      else await updateMatch(tournament.key, { ...payload, key: match.key })

      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Salvataggio non riuscito.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={
        <h2 className="app-title text-lg">{match === null ? 'Nuova partita' : 'Modifica partita'}</h2>
      }
    >
      <div className="grid grid-cols-2 gap-2">
        <Field label="Giornata">
          <input
            type="number"
            min={1}
            value={day}
            onChange={(e) => setDay(Math.max(Number(e.target.value) || 1, 1))}
            className="w-full rounded-lg border border-list-card-border bg-list-card px-3 py-2"
          />
        </Field>

        <Field label="Orario">
          <input
            value={time}
            placeholder="9:30"
            onChange={(e) => setTime(e.target.value)}
            className={`w-full rounded-lg border bg-list-card px-3 py-2 ${
              isValidTime(time) ? 'border-list-card-border' : 'border-action-danger'
            }`}
          />
        </Field>
      </div>

      <Field label="Fase">
        <select
          value={phase}
          onChange={(e) => setPhase(e.target.value)}
          className="w-full rounded-lg border border-list-card-border bg-list-card px-3 py-2
                     text-list-text focus:border-brand-blue focus:outline-none"
        >
          <option value="">— scegli —</option>
          {phases.map((p) => (
            <option key={p} value={p}>
              {formatPhase(p)}
            </option>
          ))}
        </select>
      </Field>

      <Field label="Squadra 1">
        <TeamSelect tournament={tournament} value={team1} onChange={setTeam1} />
      </Field>

      <Field label="Squadra 2">
        <TeamSelect tournament={tournament} value={team2} onChange={setTeam2} />
      </Field>

      {match !== null && (
        <div className="grid grid-cols-2 gap-2">
          <Field label="Punti 1">
            <input
              type="number"
              min={0}
              value={points1}
              onChange={(e) => setPoints1(Math.max(Number(e.target.value) || 0, 0))}
              className="w-full rounded-lg border border-list-card-border bg-list-card px-3 py-2"
            />
          </Field>
          <Field label="Punti 2">
            <input
              type="number"
              min={0}
              value={points2}
              onChange={(e) => setPoints2(Math.max(Number(e.target.value) || 0, 0))}
              className="w-full rounded-lg border border-list-card-border bg-list-card px-3 py-2"
            />
          </Field>
        </div>
      )}

      {sameTeam && (
        <p className="mt-3 text-sm text-action-danger">Una squadra non può giocare contro sé stessa.</p>
      )}
      {!isValidTime(time) && (
        <p className="mt-3 text-sm text-action-danger">Orario non valido: usa il formato 9:30.</p>
      )}
      {error !== null && <p className="mt-3 text-sm text-action-danger">{error}</p>}

      <div className="mt-4 flex gap-2">
        <Button variant="ghost" onClick={onClose} className="grow">
          Annulla
        </Button>
        <Button variant="confirm" onClick={save} disabled={!valid || busy} className="grow">
          {busy ? 'Salvataggio…' : 'Salva'}
        </Button>
      </div>
    </Modal>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="mt-2 flex flex-col gap-1">
      <span className="text-xs uppercase tracking-wide text-list-text-muted">{label}</span>
      {children}
    </label>
  )
}

function TeamSelect({
  tournament,
  value,
  onChange,
}: {
  tournament: Tournament
  value: string
  onChange: (value: string) => void
}) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="w-full rounded-lg border border-list-card-border bg-list-card px-3 py-2
                 text-list-text focus:border-brand-blue focus:outline-none"
    >
      <option value="">— scegli —</option>
      {tournament.teams.map((team) => (
        <option key={team.key} value={team.key}>
          Team {getTeamNumber(tournament.teams, team.key)} · {formatFullNames(team)}
        </option>
      ))}
    </select>
  )
}
