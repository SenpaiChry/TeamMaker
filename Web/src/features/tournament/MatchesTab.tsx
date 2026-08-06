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
            <h2 className="app-title mb-2 rounded-[10px] bg-bracket-header px-3 py-1.5 text-bracket-header-text">
              Giorno {day}
            </h2>
            {/* Struttura da tournament_layout_bracket.xml: intestazione con
                tipo e orario, divisore, poi le due squadre col punteggio. */}
            <ul className="flex flex-col gap-2">
              {dayMatches.map((match) => (
                <li
                  key={match.key}
                  className="rounded-[14px] border border-list-card-border bg-list-card p-2.5"
                >
                  <div className="flex items-center gap-2">
                    <div className="min-w-0 grow">
                      {match.type.trim().length > 0 && (
                        <div className="app-title truncate text-[13px] text-match-meta">
                          {match.type.trim()}
                        </div>
                      )}
                      <div className="app-title flex gap-2 text-[13px] text-match-meta">
                        <span>Giorno {match.day}</span>
                        <span>{match.time}</span>
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={() => setDetail(match)}
                      aria-label={
                        `Dettaglio partita delle ${match.time}: Team ${getTeamNumber(teams, match.keyTeam1)} ` +
                        `${match.points1} a ${match.points2} Team ${getTeamNumber(teams, match.keyTeam2)}`
                      }
                      className="app-title h-[34px] shrink-0 rounded-[11px] bg-brand-blue px-3.5
                                 text-sm text-white transition hover:bg-brand-blue-pressed"
                    >
                      Info
                    </button>
                  </div>

                  <div className="my-2 h-px bg-list-divider" />

                  <div className="flex items-center">
                    <span className="min-w-0 grow">
                      <TeamChip
                        label={`Team ${getTeamNumber(teams, match.keyTeam1)}`}
                        highlighted={teamMatchesQuery(teamsByKey.get(match.keyTeam1), query)}
                      />
                    </span>

                    <span className="app-title min-w-[30px] text-center text-[19px] not-italic">
                      {match.points1}
                    </span>
                    <span className="app-title px-[3px] text-[17px] not-italic text-list-text-muted">
                      -
                    </span>
                    <span className="app-title min-w-[30px] text-center text-[19px] not-italic">
                      {match.points2}
                    </span>

                    <span className="flex min-w-0 grow justify-end">
                      <TeamChip
                        label={`Team ${getTeamNumber(teams, match.keyTeam2)}`}
                        highlighted={teamMatchesQuery(teamsByKey.get(match.keyTeam2), query)}
                      />
                    </span>
                  </div>
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

/**
 * Nome squadra nella riga partita. Quando corrisponde alla ricerca prende la
 * pillola ciano di bg_team_chip, come fa applyTeamHighlight nell'adapter.
 */
function TeamChip({ label, highlighted }: { label: string; highlighted: boolean }) {
  return (
    <span
      className={`app-title inline-block max-w-full truncate px-1.5 py-[3px] text-[15px] ${
        highlighted
          ? 'rounded-full bg-list-card-highlight text-list-highlight-text'
          : 'text-list-text'
      }`}
    >
      {label}
    </span>
  )
}
