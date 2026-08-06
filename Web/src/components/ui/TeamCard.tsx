import type { Player, Team } from '@/domain/models'
import { getSurnameOrNickname } from '@/domain/player'
import { getTeamVote } from '@/domain/team'

/**
 * Scheda squadra, da layout_player_teams.xml e tournament_layout_manage_teams.xml:
 * titolo "TEAM N" centrato in ciano, divisore, giocatori su DUE colonne col
 * nome sopra e il cognome (o soprannome) sotto.
 *
 * ⚠️ Il voto si mostra solo passando `showVote`. Nell'app Android è nascosto
 * in tutte le schermate pubbliche — `TournamentTeamsAdapter` fa esplicitamente
 * `txtValueTeam.setVisibility(GONE)` e `PlayerTeamsAdapter` lo mostra solo
 * quando si generano squadre per un torneo, cioè dal percorso di gestione.
 * È un'informazione che riguarda chi organizza, non chi gioca.
 */
export function TeamCard({
  team,
  teamNumber,
  showVote = false,
  showBracket = true,
  actions,
}: {
  team: Team
  teamNumber: number
  showVote?: boolean
  showBracket?: boolean
  actions?: React.ReactNode
}) {
  const [p1, p2, p3, p4, p5] = team.players

  return (
    <div className="rounded-[14px] border border-list-card-border bg-list-card p-2.5">
      <div className="flex items-center gap-2">
        {showVote && (
          <span className="app-title w-1/3 text-center text-xl text-list-text-secondary">
            {String(getTeamVote(team)).replace('.', ',')}
          </span>
        )}

        <h3 className="app-title grow truncate text-center text-xl text-list-highlight-text">
          Team {teamNumber}
          {showBracket && team.bracket.length > 0 && (
            <span className="ml-2 text-sm text-list-text-muted">girone {team.bracket}</span>
          )}
        </h3>

        {actions !== undefined && <span className="flex shrink-0 gap-1.5">{actions}</span>}
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
