import type { Match, Tournament } from '@/domain/models'
import { getTeamNumber } from '@/domain/team'
import { label as phaseLabel } from '@/domain/phases'
import { slotLabel, TO_DO_KEY } from '@/domain/finalStages'
import { formatSetDetail } from '@/domain/matchScore'

/**
 * Riga partita della gestione, portata da tournament_layout_manage_matches.xml:
 * in alto la fase con giornata e orario più i tre tasti da 30dp — gioca,
 * modifica, elimina — poi un divisore e le due squadre ai lati del punteggio.
 *
 * Il tasto "gioca" apre il segnapunti su quella partita: nell'app Android è
 * l'unico modo per arrivarci dalla gestione.
 */
export function MatchManageRow({
  match,
  tournament,
  onPlay,
  onEdit,
  onDelete,
}: {
  match: Match
  tournament: Tournament
  onPlay: () => void
  onEdit: () => void
  onDelete: () => void
}) {
  const label1 = teamOrPlaceholder(tournament, match, 1)
  const label2 = teamOrPlaceholder(tournament, match, 2)

  return (
    <div className="rounded-[14px] border border-list-card-border bg-list-card p-2.5">
      <div className="flex items-center gap-2">
        <div className="min-w-0 grow">
          <div className="app-title truncate text-[13px] text-match-meta">
            {phaseLabel(match.type)}
          </div>
          <div className="app-title flex gap-2 text-[13px] text-match-meta">
            <span>Giorno {match.day}</span>
            <span>{match.time}</span>
          </div>
        </div>

        <IconAction label={`Apri il segnapunti su ${label1} contro ${label2}`} onClick={onPlay}>
          ▶
        </IconAction>
        <IconAction label={`Modifica ${label1} contro ${label2}`} onClick={onEdit}>
          ✎
        </IconAction>
        <IconAction label={`Elimina ${label1} contro ${label2}`} onClick={onDelete}>
          🗑
        </IconAction>
      </div>

      <div className="my-2 h-px bg-list-divider" />

      <div className="flex items-center">
        <span className="app-title min-w-0 grow truncate px-1 text-[15px]">{label1}</span>

        <span className="min-w-[30px] text-center text-[19px] font-bold tabular-nums">
          {match.points1}
        </span>
        <span className="px-[3px] text-[17px] font-bold text-list-text-muted">-</span>
        <span className="min-w-[30px] text-center text-[19px] font-bold tabular-nums">
          {match.points2}
        </span>

        <span className="app-title min-w-0 grow truncate px-1 text-right text-[15px]">
          {label2}
        </span>
      </div>

      {formatSetDetail(match) !== null && (
        <div className="mt-1 text-center text-[11px] tabular-nums text-list-text-muted">
          {formatSetDetail(match)}
        </div>
      )}
    </div>
  )
}

/**
 * Etichetta della squadra: "TEAM n" se la squadra è nota, altrimenti il
 * placeholder della sorgente ("1ª GIR. A", "VINC. Q1", ...).
 */
export function teamOrPlaceholder(tournament: Tournament, match: Match, slot: 1 | 2): string {
  return slotLabel(tournament, match, slot, (key) => {
    if (key.length === 0 || key === TO_DO_KEY) return 'TEAM ?'
    const n = getTeamNumber(tournament.teams, key)
    return n > 0 ? `TEAM ${n}` : 'TEAM ?'
  })
}

function IconAction({
  label,
  onClick,
  children,
}: {
  label: string
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      title={label}
      className="mx-[3px] grid size-[30px] shrink-0 place-items-center rounded-lg
                 bg-icon-action text-sm leading-none hover:brightness-150"
    >
      {children}
    </button>
  )
}
