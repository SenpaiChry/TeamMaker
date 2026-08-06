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
          {/* bg_bracket_header: pieno ciano, angoli da 10dp */}
          <h2 className="app-title mb-2 rounded-[10px] bg-bracket-header px-3 py-1.5 text-bracket-header-text">
            {group.bracket.length > 0 ? `Girone ${group.bracket}` : 'Calendario non generato'}
          </h2>

          {/* Misure da tournament_layout_table.xml */}
          <ul className="flex flex-col gap-2">
            {group.rows.map((row) => {
              const highlighted = teamMatchesQuery(row.team, query)
              return (
                <li
                  key={row.team.key}
                  className={`flex items-center rounded-[14px] p-2.5 ${
                    highlighted
                      ? 'border-2 border-list-card-highlight-border bg-list-card-highlight'
                      : 'border border-list-card-border bg-list-card'
                  }`}
                >
                  <span
                    className={`app-title w-[26px] shrink-0 text-center text-[17px] ${
                      highlighted ? 'text-list-highlight-text' : 'text-list-text-muted'
                    }`}
                  >
                    {row.position}
                  </span>

                  <span className="mx-2.5 min-w-0 grow">
                    <span
                      className={`app-title block truncate text-base ${
                        highlighted ? 'text-list-highlight-text' : 'text-list-text'
                      }`}
                    >
                      Team {getTeamNumber(tournament.teams, row.team.key)}
                    </span>
                    <span
                      className={`mt-px block text-[13px] italic leading-[1.05] ${
                        highlighted ? 'text-list-highlight-text' : 'text-list-text-secondary'
                      }`}
                    >
                      {formatFullNames(row.team)}
                    </span>
                  </span>

                  <span
                    className={`min-w-[46px] shrink-0 rounded-xl bg-points-pill px-2.5 py-1.5
                                text-center text-xl font-bold ${
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
