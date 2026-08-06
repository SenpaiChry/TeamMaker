import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTournaments } from '@/hooks/useTournaments'
import { teamMatchesQuery } from '@/domain/team'
import { SearchField } from '@/components/ui/SearchField'
import { ScreenHeader } from '@/components/ui/ScreenHeader'
import { TeamsTab } from './TeamsTab'
import { MatchesTab } from './MatchesTab'
import { StandingsTab } from './StandingsTab'

/**
 * Torneo attivo: squadre, calendario e classifica.
 * Porta TournamentActivityTeamsBracketsTable.
 *
 * La ricerca si comporta come nell'app Android: FILTRA squadre e partite, ma
 * nella classifica non toglie nessuno — evidenzia soltanto, perché una
 * classifica parziale non vorrebbe dire niente.
 */

type Tab = 'squadre' | 'partite' | 'classifica'

const TABS: { id: Tab; label: string }[] = [
  { id: 'squadre', label: 'SQUADRE' },
  { id: 'partite', label: 'PARTITE' },
  { id: 'classifica', label: 'CLASSIFICA' },
]

export function TournamentScreen() {
  const navigate = useNavigate()
  const { active, loading, error } = useTournaments()
  const [tab, setTab] = useState<Tab>('classifica')
  const [query, setQuery] = useState('')

  const teamsByKey = useMemo(
    () => new Map((active?.teams ?? []).map((t) => [t.key, t])),
    [active],
  )

  const visibleTeams = useMemo(() => {
    if (active === null) return []
    if (query.trim().length === 0) return active.teams
    return active.teams.filter((team) => teamMatchesQuery(team, query))
  }, [active, query])

  const visibleMatches = useMemo(() => {
    if (active === null) return []
    if (query.trim().length === 0) return active.matches
    return active.matches.filter(
      (match) =>
        teamMatchesQuery(teamsByKey.get(match.keyTeam1), query) ||
        teamMatchesQuery(teamsByKey.get(match.keyTeam2), query),
    )
  }, [active, query, teamsByKey])

  if (loading) return <p className="p-6 text-list-text-secondary">Caricamento torneo…</p>

  if (error !== null) {
    return (
      <div className="m-6 rounded-lg border border-action-danger/50 bg-action-danger/10 p-4">
        <p className="font-semibold text-action-danger">Errore di connessione</p>
        <p className="mt-1 text-sm text-list-text-secondary">{error.message}</p>
      </div>
    )
  }

  if (active === null) {
    return (
      <div className="mx-auto max-w-2xl p-4">
        <ScreenHeader title="TORNEO" onBack={() => navigate('/')} />
        <p className="text-list-text-secondary">
          Nessun torneo attivo. Attivane uno dall’app Android, oppure attendi la fase 6 per farlo
          da qui.
        </p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl p-4">
      <ScreenHeader
        title={active.name.length > 0 ? active.name : 'TORNEO'}
        onBack={() => navigate('/')}
      />

      <p className="mb-3 text-sm text-list-text-muted">
        {active.teams.length} squadre · {active.matches.length} partite
        {active.date !== null && <> · {active.date.toLocaleDateString('it')}</>}
      </p>

      <div className="mb-3 grid grid-cols-3 gap-1 rounded-lg border border-list-card-border bg-list-card p-1">
        {TABS.map(({ id, label }) => (
          <button
            key={id}
            type="button"
            onClick={() => setTab(id)}
            className={`rounded px-2 py-2 text-sm font-bold tracking-wide transition ${
              tab === id
                ? 'bg-action-selected text-white'
                : 'text-list-text-secondary hover:text-list-text'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="mb-4">
        <SearchField value={query} onChange={setQuery} />
      </div>

      {tab === 'squadre' && <TeamsTab teams={visibleTeams} allTeams={active.teams} query={query} />}
      {tab === 'partite' && (
        <MatchesTab matches={visibleMatches} teams={active.teams} query={query} />
      )}
      {tab === 'classifica' && <StandingsTab tournament={active} query={query} />}
    </div>
  )
}
