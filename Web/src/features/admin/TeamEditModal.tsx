import { useEffect, useMemo, useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import type { Player, Team, Tournament } from '@/domain/models'
import { byNameAsc } from '@/domain/player'
import { addTeamToTournament, updateTeamPlayers } from '@/data/tournamentsRepo'

/**
 * Composizione di una squadra del torneo.
 * Porta TournamentActivityPopUpEditTeam: cinque scelte, la prima vuota, e un
 * giocatore non può stare in due squadre dello stesso torneo.
 */

const SLOTS = 5

export function TeamEditModal({
  tournament,
  team,
  players,
  open,
  onClose,
}: {
  tournament: Tournament
  /** `null` per creare una nuova squadra. */
  team: Team | null
  players: Player[]
  open: boolean
  onClose: () => void
}) {
  const [chosen, setChosen] = useState<(string | null)[]>(Array(SLOTS).fill(null))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    const initial: (string | null)[] = Array(SLOTS).fill(null)
    team?.players.forEach((p, i) => {
      if (i < SLOTS) initial[i] = p.key
    })
    setChosen(initial)
    setError(null)
  }, [open, team])

  /** Giocatori già impegnati in ALTRE squadre di questo torneo. */
  const takenElsewhere = useMemo(() => {
    const taken = new Set<string>()
    for (const other of tournament.teams) {
      if (other.key === team?.key) continue
      for (const p of other.players) taken.add(p.key)
    }
    return taken
  }, [tournament, team])

  const selectable = useMemo(
    () => players.filter((p) => p.isActive || team?.players.some((tp) => tp.key === p.key)).sort(byNameAsc),
    [players, team],
  )

  const byKey = useMemo(() => new Map(players.map((p) => [p.key, p])), [players])

  const picked = chosen.filter((k): k is string => k !== null)
  const duplicated = new Set(picked).size !== picked.length
  const valid = picked.length > 0 && !duplicated

  const save = async () => {
    setBusy(true)
    setError(null)
    try {
      const chosenPlayers = picked.flatMap((k) => {
        const p = byKey.get(k)
        return p === undefined ? [] : [p]
      })

      if (team === null) await addTeamToTournament(tournament.key, chosenPlayers)
      else await updateTeamPlayers(tournament.key, team, chosenPlayers)

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
        <h2 className="app-title text-lg">{team === null ? 'Nuova squadra' : 'Modifica squadra'}</h2>
      }
    >
      <ul className="flex flex-col gap-2">
        {chosen.map((value, i) => (
          <li key={i}>
            <select
              value={value ?? ''}
              aria-label={`Giocatore ${i + 1}`}
              onChange={(e) =>
                setChosen(chosen.map((v, j) => (j === i ? (e.target.value || null) : v)))
              }
              className="w-full rounded-lg border border-list-card-border bg-list-card px-3 py-2
                         text-list-text focus:border-brand-blue focus:outline-none"
            >
              <option value="">— giocatore {i + 1} —</option>
              {selectable.map((p) => (
                <option
                  key={p.key}
                  value={p.key}
                  disabled={takenElsewhere.has(p.key) && p.key !== value}
                >
                  {p.name} {p.surname}
                  {takenElsewhere.has(p.key) ? ' (già in un’altra squadra)' : ''}
                  {p.isActive ? '' : ' (archiviato)'}
                </option>
              ))}
            </select>
          </li>
        ))}
      </ul>

      {duplicated && (
        <p className="mt-3 text-sm text-action-danger">
          Lo stesso giocatore è stato scelto più volte.
        </p>
      )}
      {picked.length === 0 && (
        <p className="mt-3 text-sm text-list-text-muted">Scegli almeno un giocatore.</p>
      )}
      {error !== null && <p className="mt-3 text-sm text-action-danger">{error}</p>}

      {tournament.matches.length > 0 && team !== null && (
        <p className="mt-3 text-sm text-action-warning">
          Il calendario è già stato generato: cambiare la squadra non cambia le partite già
          programmate.
        </p>
      )}

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
