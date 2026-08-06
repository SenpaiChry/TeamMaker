import type { Team } from '@/domain/models'
import { getTeamNumber } from '@/domain/team'
import { TeamCard } from '@/components/ui/TeamCard'

/**
 * Squadre del torneo, viste da chi gioca. Porta TournamentTeamsAdapter.
 *
 * ⚠️ Nessun voto, né di squadra né dei singoli: nell'adapter Android il campo
 * del voto viene esplicitamente nascosto. I valori si vedono solo nella
 * gestione, dove servono a chi compone le squadre.
 */
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
    <ul className="flex flex-col gap-2">
      {teams.map((team) => (
        <li key={team.key}>
          <TeamCard team={team} teamNumber={getTeamNumber(allTeams, team.key)} />
        </li>
      ))}
    </ul>
  )
}
