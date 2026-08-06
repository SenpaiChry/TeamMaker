import type { Team } from '@/domain/models'
import { countFemales, getTeamNumber, getTeamVote } from '@/domain/team'
import { getVote } from '@/domain/player'
import { PlayerName } from '@/components/ui/PlayerName'

/** Squadre del torneo. Porta TournamentTeamsAdapter. */
export function TeamsTab({
  teams,
  allTeams,
  query,
}: {
  teams: Team[]
  allTeams: Team[]
  query: string
}) {
  if (teams.length === 0) {
    return (
      <p className="text-list-text-muted">
        {query.trim().length > 0
          ? 'Nessuna squadra con un giocatore che corrisponde.'
          : 'Il torneo non ha ancora squadre.'}
      </p>
    )
  }

  return (
    <ul className="flex flex-col gap-3">
      {teams.map((team) => (
        <li key={team.key} className="rounded-lg border border-list-card-border bg-list-card p-3">
          <div className="mb-2 flex items-center justify-between gap-3">
            <h2 className="font-bold tracking-wide text-list-highlight-text">
              TEAM {getTeamNumber(allTeams, team.key)}
              {team.bracket.length > 0 && (
                <span className="ml-2 text-sm font-normal text-list-text-muted">
                  girone {team.bracket}
                </span>
              )}
            </h2>
            <div className="flex shrink-0 items-center gap-2">
              {countFemales(team) > 0 && (
                <span className="text-sm text-women-dark" title="giocatrici">
                  ♀&nbsp;{countFemales(team)}
                </span>
              )}
              <span className="rounded-full bg-score-panel px-3 py-1 text-sm font-bold tabular-nums">
                {String(getTeamVote(team)).replace('.', ',')}
              </span>
            </div>
          </div>

          <ul className="flex flex-col divide-y divide-list-card-border">
            {team.players.map((player) => (
              <li key={player.key} className="flex items-center justify-between py-1.5">
                <PlayerName player={player} showNickname={false} />
                <span className="text-sm tabular-nums text-list-text-muted">{getVote(player)}</span>
              </li>
            ))}
          </ul>
        </li>
      ))}
    </ul>
  )
}
