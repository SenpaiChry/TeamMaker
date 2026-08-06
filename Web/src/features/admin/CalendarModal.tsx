import { useMemo, useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import type { Tournament } from '@/domain/models'
import {
  countItalianBracketMatches,
  countMultipleBracketMatches,
  generateItalianBracket,
  generateMultipleBrackets,
  NotEnoughTimeError,
} from '@/domain/scheduler'
import { countAvailableSlots, isValidTime, type TimeSlot } from '@/domain/time'
import { replaceMatches } from '@/data/matchesRepo'
import { saveTeamBrackets, updateBracketCount } from '@/data/tournamentsRepo'

/**
 * Generazione del calendario.
 * Porta ActivityPopUpGenerateBracket, usando lo scheduler già portato nel dominio.
 *
 * Il calendario viene composto in memoria e scritto in una sola operazione:
 * o c'è tutto, o non cambia niente.
 */

type Mode = 'italiana' | 'gironi'

const EMPTY_SLOT: TimeSlot = { day: 1, start: '9:00', end: '13:00' }

export function CalendarModal({
  tournament,
  open,
  onClose,
}: {
  tournament: Tournament
  open: boolean
  onClose: () => void
}) {
  const [mode, setMode] = useState<Mode>('italiana')
  const [bracketCount, setBracketCount] = useState(2)
  const [minutesPerMatch, setMinutesPerMatch] = useState(15)
  const [slots, setSlots] = useState<TimeSlot[]>([EMPTY_SLOT])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const teamCount = tournament.teams.length
  /** Con più di teamCount/2 gironi resterebbero squadre senza avversari. */
  const maxBrackets = Math.max(1, Math.floor(teamCount / 2))

  const matchCount = useMemo(
    () =>
      mode === 'italiana'
        ? countItalianBracketMatches(teamCount)
        : countMultipleBracketMatches(teamCount, bracketCount),
    [mode, teamCount, bracketCount],
  )

  const capacity = useMemo(
    () => countAvailableSlots(slots, minutesPerMatch),
    [slots, minutesPerMatch],
  )

  const slotsValid = slots.every((s) => isValidTime(s.start) && isValidTime(s.end))
  const enoughTime = capacity >= matchCount
  const canGenerate =
    teamCount >= 2 && minutesPerMatch > 0 && slotsValid && enoughTime && matchCount > 0

  const generate = async () => {
    setBusy(true)
    setError(null)

    try {
      const schedule =
        mode === 'italiana'
          ? generateItalianBracket(tournament.teams, slots, minutesPerMatch)
          : generateMultipleBrackets(tournament.teams, slots, minutesPerMatch, bracketCount)

      await replaceMatches(tournament.key, schedule.matches)
      await saveTeamBrackets(tournament.key, schedule.bracketByTeam)
      await updateBracketCount(tournament.key, schedule.bracketCount)

      onClose()
    } catch (e) {
      setError(
        e instanceof NotEnoughTimeError
          ? e.message
          : e instanceof Error
            ? e.message
            : 'Generazione non riuscita.',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={<h2 className="text-lg font-bold tracking-wide">GENERA CALENDARIO</h2>}
    >
      <div className="grid grid-cols-2 gap-1 rounded-lg border border-list-card-border bg-list-card p-1">
        <ModeButton active={mode === 'italiana'} onClick={() => setMode('italiana')}>
          ALL’ITALIANA
        </ModeButton>
        <ModeButton active={mode === 'gironi'} onClick={() => setMode('gironi')}>
          A GIRONI
        </ModeButton>
      </div>

      {mode === 'gironi' && (
        <label className="mt-3 flex items-center justify-between gap-3">
          <span className="text-sm text-list-text-secondary">Numero di gironi</span>
          <input
            type="number"
            min={1}
            max={maxBrackets}
            value={bracketCount}
            onChange={(e) =>
              setBracketCount(Math.min(Math.max(Number(e.target.value) || 1, 1), maxBrackets))
            }
            className="w-20 rounded border border-list-card-border bg-list-card px-2 py-1 text-right"
          />
        </label>
      )}

      <label className="mt-2 flex items-center justify-between gap-3">
        <span className="text-sm text-list-text-secondary">Minuti per partita</span>
        <input
          type="number"
          min={1}
          value={minutesPerMatch}
          onChange={(e) => setMinutesPerMatch(Math.max(Number(e.target.value) || 0, 0))}
          className="w-20 rounded border border-list-card-border bg-list-card px-2 py-1 text-right"
        />
      </label>

      <div className="mt-4">
        <div className="mb-2 flex items-center justify-between">
          <span className="text-sm font-bold tracking-wide">FASCE ORARIE</span>
          <button
            type="button"
            onClick={() => setSlots([...slots, { ...EMPTY_SLOT, day: slots.length + 1 }])}
            className="rounded bg-icon-action px-2 py-1 text-sm hover:brightness-150"
          >
            + aggiungi
          </button>
        </div>

        <ul className="flex flex-col gap-2">
          {slots.map((slot, i) => (
            <li key={i} className="flex items-center gap-2">
              <input
                type="number"
                min={1}
                value={slot.day}
                aria-label={`Giornata della fascia ${i + 1}`}
                onChange={(e) => patchSlot(setSlots, slots, i, { day: Number(e.target.value) || 1 })}
                className="w-14 rounded border border-list-card-border bg-list-card px-2 py-1 text-center"
              />
              <TimeInput
                value={slot.start}
                label={`Inizio fascia ${i + 1}`}
                onChange={(start) => patchSlot(setSlots, slots, i, { start })}
              />
              <span className="text-list-text-muted">→</span>
              <TimeInput
                value={slot.end}
                label={`Fine fascia ${i + 1}`}
                onChange={(end) => patchSlot(setSlots, slots, i, { end })}
              />
              {slots.length > 1 && (
                <button
                  type="button"
                  onClick={() => setSlots(slots.filter((_, j) => j !== i))}
                  aria-label={`Rimuovi fascia ${i + 1}`}
                  className="grid size-7 shrink-0 place-items-center rounded bg-icon-action text-action-delete"
                >
                  ✕
                </button>
              )}
            </li>
          ))}
        </ul>
      </div>

      <p className="mt-4 text-sm text-list-text-muted">
        {matchCount} partite da generare · le fasce ne ospitano{' '}
        <span className={enoughTime ? 'text-action-confirm' : 'text-action-danger'}>{capacity}</span>
      </p>

      {tournament.matches.length > 0 && (
        <p className="mt-2 text-sm text-action-warning">
          Il calendario esistente ({tournament.matches.length} partite) verrà sostituito.
        </p>
      )}

      {error !== null && <p className="mt-2 text-sm text-action-danger">{error}</p>}

      <div className="mt-4 flex gap-2">
        <Button variant="ghost" onClick={onClose} className="grow">
          ANNULLA
        </Button>
        <Button variant="confirm" onClick={generate} disabled={!canGenerate || busy} className="grow">
          {busy ? 'GENERAZIONE…' : 'GENERA'}
        </Button>
      </div>
    </Modal>
  )
}

function patchSlot(
  setSlots: (slots: TimeSlot[]) => void,
  slots: TimeSlot[],
  index: number,
  patch: Partial<TimeSlot>,
) {
  setSlots(slots.map((slot, i) => (i === index ? { ...slot, ...patch } : slot)))
}

function TimeInput({
  value,
  label,
  onChange,
}: {
  value: string
  label: string
  onChange: (value: string) => void
}) {
  const valid = isValidTime(value)
  return (
    <input
      value={value}
      aria-label={label}
      placeholder="9:00"
      onChange={(e) => onChange(e.target.value)}
      className={`w-20 rounded border bg-list-card px-2 py-1 text-center ${
        valid ? 'border-list-card-border' : 'border-action-danger'
      }`}
    />
  )
}

function ModeButton({
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
