import type { Match, Team } from '@/domain/models'
import { getTeamNumber } from '@/domain/team'
import { formatPhase } from '@/domain/phases'

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
  teams,
  onPlay,
  onEdit,
  onDelete,
}: {
  match: Match
  teams: Team[]
  onPlay: () => void
  onEdit: () => void
  onDelete: () => void
}) {
  const n1 = getTeamNumber(teams, match.keyTeam1)
  const n2 = getTeamNumber(teams, match.keyTeam2)

  return (
    <div className="rounded-[14px] border border-list-card-border bg-list-card p-2.5">
      <div className="flex items-center gap-2">
        <div className="min-w-0 grow">
          <div className="app-title truncate text-[13px] text-match-meta">
            {formatPhase(match.type)}
          </div>
          <div className="app-title flex gap-2 text-[13px] text-match-meta">
            <span>Giorno {match.day}</span>
            <span>{match.time}</span>
          </div>
        </div>

        <IconAction label={`Apri il segnapunti su Team ${n1} contro Team ${n2}`} onClick={onPlay}>
          ▶
        </IconAction>
        <IconAction label={`Modifica Team ${n1} contro Team ${n2}`} onClick={onEdit}>
          ✎
        </IconAction>
        <IconAction label={`Elimina Team ${n1} contro Team ${n2}`} onClick={onDelete}>
          🗑
        </IconAction>
      </div>

      <div className="my-2 h-px bg-list-divider" />

      <div className="flex items-center">
        <span className="app-title min-w-0 grow truncate px-1 text-[15px]">Team {n1}</span>

        <span className="min-w-[30px] text-center text-[19px] font-bold tabular-nums">
          {match.points1}
        </span>
        <span className="px-[3px] text-[17px] font-bold text-list-text-muted">-</span>
        <span className="min-w-[30px] text-center text-[19px] font-bold tabular-nums">
          {match.points2}
        </span>

        <span className="app-title min-w-0 grow truncate px-1 text-right text-[15px]">
          Team {n2}
        </span>
      </div>
    </div>
  )
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
