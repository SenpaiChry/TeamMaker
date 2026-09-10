import { useEffect, useMemo, useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import type { BonusFlags, Gender, Player, Stats } from '@/domain/models'
import { getVote } from '@/domain/player'
import type { StatDefinition } from '@/domain/statCatalog'
import { TYPE_RANGE } from '@/domain/statCatalog'
import { useStatCatalog } from '@/hooks/useStatCatalog'
import { addPlayer, updatePlayer } from '@/data/playersRepo'

/**
 * Creazione e modifica di un giocatore — porta ActivityEditPlayer +
 * StatsPlayerAdapter dopo il passaggio al catalogo dinamico.
 *
 * Le statistiche di tipo STARS si scelgono cliccando la stella: la stella `i`
 * dà valore `i × step`. Le RANGE (l'altezza è l'esempio classico) mostrano
 * una tendina con le fasce configurate. Se una stat ammette il bonus, accanto
 * alle stelle compare un pulsante-stella per accenderlo/spegnerlo.
 */

function emptyStats(catalog: StatDefinition[]): Stats {
  const stats: Stats = {}
  for (const def of catalog) stats[def.key] = 0
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
  const { catalog } = useStatCatalog()
  const [name, setName] = useState('')
  const [surname, setSurname] = useState('')
  const [nickname, setNickname] = useState('')
  const [gender, setGender] = useState<Gender | null>(null)
  const [stats, setStats] = useState<Stats>({})
  const [bonus, setBonus] = useState<BonusFlags>({})
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Ricarica i campi ogni volta che la modale si apre su un giocatore diverso.
  // Nel caso di un nuovo giocatore i valori partono a zero per tutte le stat
  // del catalogo corrente, così una nuova stat aggiunta di recente entra
  // subito in scena.
  useEffect(() => {
    if (!open) return
    setName(player?.name ?? '')
    setSurname(player?.surname ?? '')
    setNickname(player?.nickname ?? '')
    setGender(player?.gender ?? null)
    setStats(player === null ? emptyStats(catalog) : { ...player.stats })
    setBonus(player === null ? {} : { ...player.bonus })
    setError(null)
  }, [open, player, catalog])

  const valid = name.trim().length > 0 && gender !== null
  const total = useMemo(
    () => getVote({ ...(player ?? emptyPlayer()), stats, bonus }, catalog),
    [player, stats, bonus, catalog],
  )

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
        bonus,
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
        {catalog.map((stat) => (
          <li key={stat.key} className="flex items-center justify-between gap-3 py-2">
            <span className="text-sm text-list-text-secondary">{stat.label}</span>
            <span className="flex items-center gap-2">
              {stat.type === TYPE_RANGE ? (
                <RangeSelect
                  stat={stat}
                  value={stats[stat.key] ?? 0}
                  onChange={(value) => setStats({ ...stats, [stat.key]: value })}
                />
              ) : (
                <StarPicker
                  stat={stat}
                  value={stats[stat.key] ?? 0}
                  onChange={(value) => setStats({ ...stats, [stat.key]: value })}
                />
              )}
              {stat.allowBonus && (
                <BonusToggle
                  active={bonus[stat.key] === true}
                  onToggle={() =>
                    setBonus((prev) => ({ ...prev, [stat.key]: !(prev[stat.key] === true) }))
                  }
                  statLabel={stat.label}
                />
              )}
            </span>
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

/** Player "vuoto" per riusare `getVote` sul totale live senza casi speciali. */
function emptyPlayer(): Player {
  return {
    key: '',
    name: '',
    surname: '',
    nickname: '',
    gender: 'M',
    isActive: true,
    stats: {},
    bonus: {},
  }
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
 * Selettore a stelle. Cliccando la stella `i` la statistica vale `i × step`,
 * quindi la prima stella corrisponde a zero ed è sempre accesa: è il
 * comportamento dell'app Android.
 */
function StarPicker({
  stat,
  value,
  onChange,
}: {
  stat: StatDefinition
  value: number
  onChange: (value: number) => void
}) {
  const step = stat.step > 0 ? stat.step : 1
  const total = Math.floor(stat.max / step) + 1
  const level = Math.floor(value / step)

  return (
    <span className="flex gap-0.5">
      {Array.from({ length: total }, (_, i) => (
        <button
          key={i}
          type="button"
          onClick={() => onChange(i * step)}
          aria-label={`${stat.label}: ${i * step}`}
          className={`text-lg leading-none ${i <= level ? 'text-stars' : 'text-list-text-muted/40'}`}
        >
          ★
        </button>
      ))}
    </span>
  )
}

/**
 * Tendina di fasce per le stat di tipo RANGE. Il valore memorizzato è
 * `index × step`, per lasciare `step` a decidere quanto pesa una fascia sul
 * voto (l'altezza tipicamente vale 1 per fascia).
 */
function RangeSelect({
  stat,
  value,
  onChange,
}: {
  stat: StatDefinition
  value: number
  onChange: (value: number) => void
}) {
  const step = stat.step > 0 ? stat.step : 1
  const index = Math.min(Math.max(Math.trunc(value / step), 0), Math.max(stat.values.length - 1, 0))

  if (stat.values.length === 0) {
    return <span className="text-sm text-list-text-muted">—</span>
  }

  return (
    <select
      value={String(index)}
      onChange={(e) => onChange(Number(e.target.value) * step)}
      aria-label={stat.label}
      className="min-w-24 rounded-lg border border-list-card-border bg-list-card
                 px-3 py-2 text-sm text-list-text"
    >
      {stat.values.map((label, i) => (
        <option key={`${i}-${label}`} value={i}>
          {label}
        </option>
      ))}
    </select>
  )
}

/**
 * Interruttore a stella per il bonus per-stat. Ciano acceso quando attivo,
 * mutedo quando spento; il tooltip spiega il peso sul voto.
 */
function BonusToggle({
  active,
  onToggle,
  statLabel,
}: {
  active: boolean
  onToggle: () => void
  statLabel: string
}) {
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-pressed={active}
      title={`Bonus ${statLabel}`}
      className={`text-lg leading-none transition ${
        active ? 'text-bracket-header' : 'text-list-text-muted/40'
      }`}
    >
      ★
    </button>
  )
}
