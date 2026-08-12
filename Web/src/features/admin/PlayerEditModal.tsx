import { useEffect, useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import {
  HEIGHT_VALUES,
  STAT_KEYS,
  STAT_LABELS,
  STAT_MAX,
  STAT_STEP,
  type StatKey,
} from '@/domain/constants'
import type { Gender, Player, Stats } from '@/domain/models'
import { addPlayer, updatePlayer } from '@/data/playersRepo'

/**
 * Creazione e modifica di un giocatore.
 * Porta ActivityEditPlayer + StatsPlayerAdapter.
 *
 * Le statistiche si impostano a stelle: la stella di indice `i` vale
 * `i × passo`. L'altezza fa eccezione e usa un elenco di fasce.
 */

function emptyStats(): Stats {
  const stats = {} as Stats
  for (const key of STAT_KEYS) stats[key] = 0
  return stats
}

export function PlayerEditModal({
  player,
  open,
  onClose,
}: {
  /** `null` per creare un nuovo giocatore. */
  player: Player | null
  open: boolean
  onClose: () => void
}) {
  const [name, setName] = useState('')
  const [surname, setSurname] = useState('')
  const [nickname, setNickname] = useState('')
  const [gender, setGender] = useState<Gender | null>(null)
  const [stats, setStats] = useState<Stats>(emptyStats)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Ricarica i campi ogni volta che la modale si apre su un giocatore diverso.
  useEffect(() => {
    if (!open) return
    setName(player?.name ?? '')
    setSurname(player?.surname ?? '')
    setNickname(player?.nickname ?? '')
    setGender(player?.gender ?? null)
    setStats(player === null ? emptyStats() : { ...player.stats })
    setError(null)
  }, [open, player])

  const valid = name.trim().length > 0 && gender !== null

  const save = async () => {
    if (!valid || gender === null) return
    setSaving(true)
    setError(null)

    try {
      const payload = {
        name: name.trim(),
        surname: surname.trim(),
        nickname: nickname.trim(),
        gender,
        isActive: player?.isActive ?? true,
        stats,
      }

      if (player === null) await addPlayer(payload)
      else await updatePlayer({ ...payload, key: player.key })

      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Salvataggio non riuscito.')
    } finally {
      setSaving(false)
    }
  }

  const total = STAT_KEYS.reduce((sum, key) => sum + stats[key], 0)

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={
        <div className="flex items-baseline justify-between gap-3">
          <h2 className="text-lg font-bold tracking-wide">
            {player === null ? 'NUOVO GIOCATORE' : 'MODIFICA GIOCATORE'}
          </h2>
          <span className="text-2xl font-black tabular-nums text-list-highlight-text">{total}</span>
        </div>
      }
    >
      <div className="flex flex-col gap-2">
        <Field label="Nome" value={name} onChange={setName} required />
        <Field label="Cognome" value={surname} onChange={setSurname} />
        <Field label="Soprannome" value={nickname} onChange={setNickname} />

        <div className="mt-1 grid grid-cols-2 gap-2">
          <GenderButton label="UOMO" active={gender === 'M'} onClick={() => setGender('M')} tone="men" />
          <GenderButton label="DONNA" active={gender === 'F'} onClick={() => setGender('F')} tone="women" />
        </div>
      </div>

      <ul className="mt-4 flex max-h-72 flex-col divide-y divide-list-card-border overflow-y-auto">
        {STAT_KEYS.map((stat) => (
          <li key={stat} className="flex items-center justify-between gap-3 py-2">
            <span className="text-sm text-list-text-secondary">{STAT_LABELS[stat]}</span>
            {stat === 'height' ? (
              <select
                value={String(Math.trunc(stats.height))}
                onChange={(e) => setStats({ ...stats, height: Number(e.target.value) })}
                className="min-w-24 rounded-lg border border-list-card-border bg-list-card
                           px-3 py-2 text-sm text-list-text"
              >
                {HEIGHT_VALUES.map((label, i) => (
                  <option key={label} value={i}>
                    {label}
                  </option>
                ))}
              </select>
            ) : (
              <StarPicker
                stat={stat}
                value={stats[stat]}
                onChange={(value) => setStats({ ...stats, [stat]: value })}
              />
            )}
          </li>
        ))}
      </ul>

      {error !== null && <p className="mt-3 text-sm text-action-danger">{error}</p>}
      {!valid && <p className="mt-3 text-sm text-list-text-muted">Servono nome e sesso.</p>}

      <div className="mt-4 flex gap-2">
        <Button variant="ghost" onClick={onClose} className="grow">
          ANNULLA
        </Button>
        <Button variant="confirm" onClick={save} disabled={!valid || saving} className="grow">
          {saving ? 'SALVATAGGIO…' : 'SALVA'}
        </Button>
      </div>
    </Modal>
  )
}

function Field({
  label,
  value,
  onChange,
  required = false,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  required?: boolean
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-xs uppercase tracking-wide text-list-text-muted">
        {label}
        {required && ' *'}
      </span>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="rounded-lg border border-list-card-border bg-list-card px-3 py-2
                   text-list-text focus:border-list-highlight-text focus:outline-none"
      />
    </label>
  )
}

function GenderButton({
  label,
  active,
  onClick,
  tone,
}: {
  label: string
  active: boolean
  onClick: () => void
  tone: 'men' | 'women'
}) {
  const activeClass = tone === 'men' ? 'bg-men-dark text-black' : 'bg-women text-black'
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-lg border py-2 font-bold transition ${
        active ? `border-transparent ${activeClass}` : 'border-list-card-border bg-list-card'
      }`}
    >
      {label}
    </button>
  )
}

/**
 * Selettore a stelle. Cliccando la stella `i` la statistica vale `i × passo`,
 * quindi la prima stella corrisponde a zero ed è sempre accesa: è il
 * comportamento dell'app Android.
 */
function StarPicker({
  stat,
  value,
  onChange,
}: {
  stat: StatKey
  value: number
  onChange: (value: number) => void
}) {
  const step = STAT_STEP[stat]
  const total = Math.floor(STAT_MAX[stat] / step) + 1
  const level = Math.floor(value / step)

  return (
    <span className="flex gap-0.5">
      {Array.from({ length: total }, (_, i) => (
        <button
          key={i}
          type="button"
          onClick={() => onChange(i * step)}
          aria-label={`${STAT_LABELS[stat]}: ${i * step}`}
          className={`text-lg leading-none ${i <= level ? 'text-stars' : 'text-list-text-muted/40'}`}
        >
          ★
        </button>
      ))}
    </span>
  )
}
