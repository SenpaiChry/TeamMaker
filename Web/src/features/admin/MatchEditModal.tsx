import { useEffect, useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import type { Match, Tournament } from '@/domain/models'
import { getTeamNumber } from '@/domain/team'
import { formatFullNames } from '@/domain/team'
import { availablePhases, label as phaseLabel, normalize as normalizePhase } from '@/domain/phases'
import { isValidTime } from '@/domain/time'
import { addMatch, resolveFinalStages, updateMatch } from '@/data/matchesRepo'

/**
 * Creazione e modifica di una partita — porta TournamentActivityEditMatch.
 *
 * Il risultato si compila un set alla volta: le righe con entrambi i punti
 * lasciati vuoti vengono ignorate al salvataggio, quelle senza vincitore
 * (`p1 === p2`) invalidano il form. Il "punteggio" della partita
 * (`points1`/`points2`) è il conteggio dei set vinti, calcolato dal dettaglio;
 * il dettaglio viene scritto sul database esattamente come lo scrive il
 * segnapunti, così classifica e app Android leggono lo stesso formato.
 *
 * Retrocompat legacy: se apri una partita salvata senza `detail` ma con
 * `points1`/`points2` diversi da zero (formato vecchio a set unico), il
 * conteggio parte da quei valori. Se non aggiungi righe, il salvataggio li
 * lascia intatti — così NON perdi il vecchio risultato solo per aver aperto
 * l'editor.
 */

/** Riga dell'editor: `null` = campo vuoto (differente da 0). */
type SetRow = { p1: number | null; p2: number | null }

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
  const [sets, setSets] = useState<SetRow[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const phases = availablePhases(tournament.nBracket)

  useEffect(() => {
    if (!open) return
    setDay(match?.day ?? 1)
    setTime(match?.time ?? '9:00')
    setTeam1(match?.keyTeam1 ?? '')
    setTeam2(match?.keyTeam2 ?? '')
    setPhase(normalizePhase(match?.type ?? ''))
    setSets((match?.detail ?? []).map(([p1, p2]) => ({ p1: p1 ?? 0, p2: p2 ?? 0 })))
    setError(null)
  }, [open, match])

  const sameTeam = team1 !== '' && team1 === team2

  // Righe considerate "in gioco": entrambe vuote → riga scartata; almeno una
  // riempita → richiede un vincitore. Nell'Android il controllo è lo stesso.
  const activeRows = sets.filter((s) => s.p1 !== null || s.p2 !== null)
  const invalidSet = activeRows.some((s) => (s.p1 ?? 0) === (s.p2 ?? 0))

  const valid =
    isValidTime(time) &&
    team1 !== '' &&
    team2 !== '' &&
    !sameTeam &&
    phase !== '' &&
    day >= 1 &&
    !invalidSet

  // Conteggio live "2 - 1" mostrato accanto al titolo dell'elenco set.
  const liveSets1 = activeRows.reduce((n, s) => ((s.p1 ?? 0) > (s.p2 ?? 0) ? n + 1 : n), 0)
  const liveSets2 = activeRows.reduce((n, s) => ((s.p2 ?? 0) > (s.p1 ?? 0) ? n + 1 : n), 0)

  const addSet = () => setSets([...sets, { p1: null, p2: null }])
  const removeSet = (i: number) => setSets(sets.filter((_, k) => k !== i))
  const patchSet = (i: number, patch: Partial<SetRow>) =>
    setSets(sets.map((s, k) => (k === i ? { ...s, ...patch } : s)))

  const save = async () => {
    setBusy(true)
    setError(null)
    try {
      // Costruisce il detail dai set validi (righe entrambe vuote scartate).
      const detail = activeRows.map((s) => [s.p1 ?? 0, s.p2 ?? 0])

      // Regole `points1`/`points2` (identiche a Android):
      //  - se ci sono set validi → conteggio dei set vinti
      //  - se non ci sono set validi ma la partita originale ne aveva → azzera
      //  - se legacy senza detail → mantieni i vecchi points1/points2
      let points1 = 0
      let points2 = 0
      if (detail.length > 0) {
        for (const set of detail) {
          if (set[0]! > set[1]!) points1++
          else points2++
        }
      } else if (match !== null && match.detail.length === 0) {
        points1 = match.points1
        points2 = match.points2
      }

      const payload = {
        keyTeam1: team1,
        keyTeam2: team2,
        day,
        time,
        points1,
        points2,
        detail,
        type: phase,
        // Le sorgenti delle fasi finali si preservano: l'editor non le tocca
        // (le assegna solo il generatore di finali).
        source1Type: match?.source1Type ?? '',
        source1Ref: match?.source1Ref ?? '',
        source2Type: match?.source2Type ?? '',
        source2Ref: match?.source2Ref ?? '',
      }

      if (match === null) {
        await addMatch(tournament.key, payload)
      } else {
        const updated = { ...payload, key: match.key }
        await updateMatch(tournament.key, updated)
        // Propaga la modifica alle fasi finali che dipendono da questa partita.
        const updatedTournament = {
          ...tournament,
          matches: tournament.matches.map((m) => (m.key === match.key ? updated : m)),
        }
        await resolveFinalStages(updatedTournament)
      }

      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Salvataggio non riuscito.')
    } finally {
      setBusy(false)
    }
  }

  // Se apro una partita legacy senza detail ma con un risultato salvato,
  // mostro il conteggio ereditato invece di "0 - 0", così l'utente vede
  // subito da dove parte e non si spaventa se non aggiunge righe.
  const showsLegacyScore =
    match !== null &&
    match.detail.length === 0 &&
    (match.points1 !== 0 || match.points2 !== 0) &&
    activeRows.length === 0

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
              {phaseLabel(p)}
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
        <div className="mt-3">
          <div className="mb-2 flex items-center justify-between">
            <span className="text-xs uppercase tracking-wide text-list-text-muted">Risultato</span>
            <span className="text-sm tabular-nums text-list-text">
              <b className="text-list-text">{showsLegacyScore ? match.points1 : liveSets1}</b>
              {' - '}
              <b className="text-list-text">{showsLegacyScore ? match.points2 : liveSets2}</b>
              {showsLegacyScore && (
                <span className="ml-2 text-xs text-list-text-muted">(punti, formato vecchio)</span>
              )}
            </span>
          </div>

          <ul className="flex flex-col gap-1.5">
            {sets.map((s, i) => (
              <li key={i} className="flex items-center gap-2">
                <span className="w-[46px] shrink-0 text-xs uppercase tracking-wide text-list-text-muted">
                  Set {i + 1}
                </span>
                <SetInput
                  value={s.p1}
                  ariaLabel={`Punti squadra 1, set ${i + 1}`}
                  onChange={(p1) => patchSet(i, { p1 })}
                />
                <span className="text-list-text-muted">-</span>
                <SetInput
                  value={s.p2}
                  ariaLabel={`Punti squadra 2, set ${i + 1}`}
                  onChange={(p2) => patchSet(i, { p2 })}
                />
                <button
                  type="button"
                  onClick={() => removeSet(i)}
                  aria-label={`Rimuovi set ${i + 1}`}
                  className="grid size-8 shrink-0 place-items-center rounded-lg bg-icon-action
                             text-action-delete hover:brightness-150"
                >
                  ✕
                </button>
              </li>
            ))}
          </ul>

          <button
            type="button"
            onClick={addSet}
            className="mt-2 rounded-lg bg-icon-action px-3 py-1.5 text-sm text-list-text hover:brightness-150"
          >
            + Aggiungi set
          </button>

          {invalidSet && (
            <p className="mt-2 text-xs text-action-danger">
              Un set non può finire in pareggio: correggi i punteggi.
            </p>
          )}
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

/**
 * Input numerico per un punteggio di set. Il valore `null` corrisponde al
 * campo vuoto: distinguerlo da `0` è essenziale, perché una riga con entrambi
 * vuoti viene scartata mentre una con `0-25` è un set valido.
 */
function SetInput({
  value,
  ariaLabel,
  onChange,
}: {
  value: number | null
  ariaLabel: string
  onChange: (value: number | null) => void
}) {
  return (
    <input
      type="number"
      min={0}
      inputMode="numeric"
      aria-label={ariaLabel}
      value={value === null ? '' : String(value)}
      onChange={(e) => {
        const raw = e.target.value
        if (raw === '') onChange(null)
        else onChange(Math.max(Number(raw) || 0, 0))
      }}
      className="w-16 rounded-lg border border-list-card-border bg-list-card px-2 py-1.5 text-center
                 tabular-nums focus:border-brand-blue focus:outline-none"
    />
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
