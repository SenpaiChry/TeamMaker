import { useMemo, useState } from 'react'
import type { Match, Team } from '@/domain/models'
import { getTeamNumber, teamMatchesQuery } from '@/domain/team'
import { MatchDetailModal } from './MatchDetailModal'

/**
 * Calendario delle partite, raggruppato per giornata.
 * Porta TournamentBracketAdapter.
 *
 * L'app Android mostrava una riga piatta per ogni partita; qui le giornate
 * fanno da intestazione, perché il campo `day` è già lì e a colpo d'occhio
 * serve sapere "cosa si gioca oggi".
 */
export function MatchesTab({
  matches,
  teams,
  tournamentKey,
  query,
}: {
  matches: Match[]
  teams: Team[]
  tournamentKey: string
  query: string
}) {
  const [detail, setDetail] = useState<Match | null>(null)

  const teamsByKey = useMemo(() => new Map(teams.map((t) => [t.key, t])), [teams])

  const byDay = useMemo(() => {
    const groups = new Map<number, Match[]>()
    for (const match of matches) {
      const list = groups.get(match.day)
      if (list === undefined) groups.set(match.day, [match])
      else list.push(match)
    }
    return [...groups.entries()].sort((a, b) => a[0] - b[0])
  }, [matches])

  if (matches.length === 0) {
    return (
      <p className="text-list-text-muted">
        {query.trim().length > 0
          ? 'Nessuna partita con un giocatore che corrisponde.'
          : 'Il calendario non è ancora stato generato.'}
      </p>
    )
  }

  return (
    <>
      <div className="flex flex-col gap-4">
        {byDay.map(([day, dayMatches]) => (
          <section key={day}>
            <h2 className="mb-2 rounded bg-bracket-header px-3 py-1 text-sm font-bold tracking-wide text-bracket-header-text">
              GIORNO {day}
            </h2>
            <ul className="flex flex-col gap-2">
              {dayMatches.map((match) => (
                <li key={match.key}>
                  <button
                    type="button"
                    onClick={() => setDetail(match)}
                    aria-label={
                      `Partita delle ${match.time}: TEAM ${getTeamNumber(teams, match.keyTeam1)} ` +
                      `${match.points1} a ${match.points2} TEAM ${getTeamNumber(teams, match.keyTeam2)}`
                    }
                    className="flex w-full items-center gap-3 rounded-lg border border-list-card-border
                               bg-list-card px-3 py-2 text-left hover:border-brand-blue hover:bg-score-panel"
                  >
                    <span className="w-14 shrink-0 text-sm tabular-nums text-match-meta">
                      {match.time}
                    </span>

                    <span className="flex min-w-0 grow items-center justify-center gap-2">
                      <TeamChip
                        label={`TEAM ${getTeamNumber(teams, match.keyTeam1)}`}
                        highlighted={teamMatchesQuery(teamsByKey.get(match.keyTeam1), query)}
                      />
                      <span className="shrink-0 tabular-nums">
                        <b>{match.points1}</b>
                        <span className="mx-1 text-list-text-muted">–</span>
                        <b>{match.points2}</b>
                      </span>
                      <TeamChip
                        label={`TEAM ${getTeamNumber(teams, match.keyTeam2)}`}
                        highlighted={teamMatchesQuery(teamsByKey.get(match.keyTeam2), query)}
                      />
                    </span>

                    {match.type.trim().length > 0 && (
                      <span className="w-20 shrink-0 truncate text-right text-xs text-match-meta">
                        {match.type.trim()}
                      </span>
                    )}
                  </button>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>

      <MatchDetailModal
        match={detail}
        teams={teams}
        tournamentKey={tournamentKey}
        onClose={() => setDetail(null)}
      />
    </>
  )
}

function TeamChip({ label, highlighted }: { label: string; highlighted: boolean }) {
  return (
    <span
      className={`shrink-0 rounded-full px-2 py-0.5 text-sm ${
        highlighted
          ? 'bg-list-card-highlight font-bold text-list-highlight-text'
          : 'text-list-text'
      }`}
    >
      {label}
    </span>
  )
}
