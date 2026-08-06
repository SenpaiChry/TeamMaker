import type { Player, Team } from '@/domain/models'
import { getSurnameOrNickname } from '@/domain/player'
import { getTeamVote } from '@/domain/team'

/**
 * Scheda squadra della gestione, portata da tournament_layout_manage_teams.xml:
 * titolo "TEAM N" centrato in ciano con i tasti modifica ed elimina, divisore,
 * poi i giocatori su DUE colonne — 1 e 2, 3 e 4, il quinto a tutta larghezza —
 * con il nome sopra e il cognome sotto.
 */
export function TeamManageCard({
  teamNumber,
  team,
  onEdit,
  onDelete,
}: {
  teamNumber: number
  team: Team
  onEdit: () => void
  onDelete: () => void
}) {
  const [p1, p2, p3, p4, p5] = team.players

  return (
    <div className="rounded-[14px] border border-list-card-border bg-list-card p-2.5">
      <div className="flex items-center gap-2">
        <span className="w-1/3 text-center text-xl text-list-text-secondary app-title">
          {String(getTeamVote(team)).replace('.', ',')}
        </span>

        <h3 className="app-title grow truncate text-center text-xl text-list-highlight-text">
          Team {teamNumber}
          {team.bracket.length > 0 && (
            <span className="ml-2 text-sm text-list-text-muted">girone {team.bracket}</span>
          )}
        </h3>

        <span className="flex shrink-0 gap-1.5">
          <IconAction label={`Modifica team ${teamNumber}`} onClick={onEdit}>
            ✎
          </IconAction>
          <IconAction label={`Elimina team ${teamNumber}`} onClick={onDelete}>
            🗑
          </IconAction>
        </span>
      </div>

      <div className="my-2 h-px bg-list-divider" />

      {team.players.length === 0 ? (
        <p className="text-center text-sm text-list-text-muted">Squadra vuota</p>
      ) : (
        <>
          <div className="flex">
            <PlayerCell player={p1} />
            <PlayerCell player={p2} />
          </div>
          {(p3 !== undefined || p4 !== undefined) && (
            <div className="mt-1.5 flex">
              <PlayerCell player={p3} />
              <PlayerCell player={p4} />
            </div>
          )}
          {p5 !== undefined && (
            <div className="mt-1.5">
              <PlayerCell player={p5} />
            </div>
          )}
        </>
      )}
    </div>
  )
}

/** Nome sopra, cognome (o soprannome) sotto, centrati. */
function PlayerCell({ player }: { player: Player | undefined }) {
  if (player === undefined) return <div className="flex-1" />

  return (
    <div className="min-w-0 flex-1 text-center">
      <div className="app-title truncate text-base text-list-text">{player.name}</div>
      <div className="truncate text-base italic text-list-text-secondary">
        {getSurnameOrNickname(player)}
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
      className="grid size-[30px] shrink-0 place-items-center rounded-lg bg-icon-action
                 text-sm leading-none hover:brightness-150"
    >
      {children}
    </button>
  )
}
