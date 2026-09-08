import { useMemo } from 'react'
import type { Tournament } from '@/domain/models'
import { computeStandings, type StandingRow } from '@/domain/standings'
import { formatFullNames, getTeamNumber, teamMatchesQuery } from '@/domain/team'

/**
 * Classifica raggruppata per girone — porta TournamentTableAdapter dopo il
 * refactor con StandingsUtility.
 *
 * Colonne (a destra del nome squadra): V (vittorie), QS (quoziente set),
 * QP (quoziente punti) e pillola dei punti classifica. Le squadre pari su
 * tutti i criteri (chain + scontro diretto) condividono lo stesso rank; chi è
 * separata SOLO dallo scontro diretto ha un piccolo "SD" ciano accanto al nome.
 *
 * ⚠️ La ricerca NON filtra: la classifica resta completa e le squadre
 * corrispondenti vengono solo evidenziate. Una classifica con metà delle
 * squadre nascoste darebbe posizioni prive di senso.
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

          <ul className="flex flex-col gap-2">
            {group.rows.map((row) => (
              <StandingRowView
                key={row.team.key}
                row={row}
                teamNumber={getTeamNumber(tournament.teams, row.team.key)}
                highlighted={teamMatchesQuery(row.team, query)}
              />
            ))}
          </ul>
        </section>
      ))}
    </div>
  )
}

function StandingRowView({
  row,
  teamNumber,
  highlighted,
}: {
  row: StandingRow
  teamNumber: number
  highlighted: boolean
}) {
  return (
    <li
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
        {row.rank}
      </span>

      <span className="mx-2.5 min-w-0 grow">
        <span className="flex items-baseline gap-1.5">
          <span
            className={`app-title truncate text-base ${
              highlighted ? 'text-list-highlight-text' : 'text-list-text'
            }`}
          >
            Team {teamNumber}
          </span>
          {row.directClash && (
            <span
              className="app-title shrink-0 text-[11px] text-list-highlight-text"
              title="Ordinata rispetto a una pari grazie allo scontro diretto"
            >
              SD
            </span>
          )}
        </span>
        <span
          className={`mt-px block text-[13px] italic leading-[1.05] ${
            highlighted ? 'text-list-highlight-text' : 'text-list-text-secondary'
          }`}
        >
          {formatFullNames(row.team)}
        </span>
      </span>

      <StatColumn label="V" value={String(row.wins)} minWidth="min-w-[30px]" />
      <StatColumn
        label="QS"
        value={formatQuotient(row.setsWon, row.setsLost)}
        minWidth="min-w-[42px]"
      />
      <StatColumn
        label="QP"
        value={formatQuotient(row.pointsFor, row.pointsAgainst)}
        minWidth="min-w-[42px]"
        extraGap
      />

      <span
        className={`app-title min-w-[46px] shrink-0 rounded-xl bg-points-pill px-2.5 py-1.5
                    text-center text-xl ${
                      highlighted ? 'text-list-highlight-text' : 'text-list-text'
                    }`}
      >
        {row.classificaPoints}
      </span>
    </li>
  )
}

function StatColumn({
  label,
  value,
  minWidth,
  extraGap = false,
}: {
  label: string
  value: string
  minWidth: string
  extraGap?: boolean
}) {
  return (
    <span
      className={`app-title flex shrink-0 flex-col items-center leading-tight
                  text-list-text-secondary ${minWidth} ${extraGap ? 'mr-2' : 'mr-1'}`}
    >
      <span className="text-[10px] text-list-text-muted">{label}</span>
      <span className="text-[14px] tabular-nums">{value}</span>
    </span>
  )
}

/**
 * Quoziente vinti/persi a 3 decimali, come nell'app Android (rende
 * distinguibili le pari apparenti). `-` quando il denominatore è zero e neppure
 * il numeratore c'è (squadra senza dati). Se solo il denominatore è zero e la
 * squadra ha comunque vinto qualcosa mostriamo `∞` invece di un numero enorme:
 * è più chiaro di "999.000".
 */
function formatQuotient(won: number, lost: number): string {
  if (won === 0 && lost === 0) return '-'
  if (lost === 0) return '∞'
  return (won / lost).toFixed(3).replace('.', ',')
}
