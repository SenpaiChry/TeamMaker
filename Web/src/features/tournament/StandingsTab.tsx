import { useMemo } from 'react'
import type { Tournament } from '@/domain/models'
import { computeStandings } from '@/domain/standings'
import { formatFullNames, getTeamNumber, teamMatchesQuery } from '@/domain/team'

/**
 * Classifica raggruppata per girone.
 * Porta TournamentTableAdapter.
 *
 * ⚠️ A differenza delle altre schede la ricerca NON filtra: la classifica resta
 * completa e le squadre corrispondenti vengono solo evidenziate. Una classifica
 * con metà delle squadre nascoste darebbe posizioni prive di senso.
 */
export function StandingsTab({ tournament, query }: { tournament: Tournament; query: string }) {
  const standings = useMemo(() => computeStandings(tournament), [tournament])

  if (tournament.teams.length === 0) {
    return <p className="text-list-text-muted">Il torneo non ha ancora squadre.</p>
  }

  return (
    <div className="flex flex-col gap-4">
      {standings.map((group) => (
        <section key={group.bracket}>
          <h2 className="mb-2 rounded bg-bracket-header px-3 py-1 text-sm font-bold tracking-wide text-bracket-header-text">
            {group.bracket.length > 0 ? `GIRONE ${group.bracket}` : 'CALENDARIO NON GENERATO'}
          </h2>

          <ul className="flex flex-col gap-2">
            {group.rows.map((row) => {
              const highlighted = teamMatchesQuery(row.team, query)
              return (
                <li
                  key={row.team.key}
                  className={`flex items-center gap-3 rounded-lg border px-3 py-2 ${
                    highlighted
                      ? 'border-list-card-highlight-border bg-list-card-highlight'
                      : 'border-list-card-border bg-list-card'
                  }`}
                >
                  <span
                    className={`w-6 shrink-0 text-center text-sm tabular-nums ${
                      highlighted ? 'text-list-highlight-text' : 'text-list-text-muted'
                    }`}
                  >
                    {row.position}
                  </span>

                  <span className="min-w-0 grow">
                    <span
                      className={`block font-bold ${
                        highlighted ? 'text-list-highlight-text' : 'text-list-text'
                      }`}
                    >
                      TEAM {getTeamNumber(tournament.teams, row.team.key)}
                    </span>
                    <span
                      className={`block truncate text-sm ${
                        highlighted ? 'text-list-highlight-text' : 'text-list-text-secondary'
                      }`}
                    >
                      {formatFullNames(row.team)}
                    </span>
                  </span>

                  <span
                    className={`shrink-0 rounded-full bg-points-pill px-3 py-1 font-bold tabular-nums ${
                      highlighted ? 'text-list-highlight-text' : 'text-list-text'
                    }`}
                  >
                    {row.points}
                  </span>
                </li>
              )
            })}
          </ul>
        </section>
      ))}
    </div>
  )
}
