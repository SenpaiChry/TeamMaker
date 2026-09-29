import { useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useTournaments } from '@/hooks/useTournaments'
import { usePlayers } from '@/hooks/usePlayers'
import type { Match, Team } from '@/domain/models'
import { getTeamNumber } from '@/domain/team'
import { deleteTeam, setLocked } from '@/data/tournamentsRepo'
import { deleteAllMatches, deleteMatch } from '@/data/matchesRepo'
import { ScreenHeader } from '@/components/ui/ScreenHeader'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { TeamEditModal } from './TeamEditModal'
import { MatchEditModal } from './MatchEditModal'
import { CalendarModal } from './CalendarModal'
import { FinalStagesModal } from './FinalStagesModal'
import { TeamManageCard } from './TeamManageCard'
import { MatchManageRow } from './MatchManageRow'

/**
 * Gestione di un torneo: squadre e calendario.
 * Porta TournamentActivityManageTeams e TournamentActivityManageMatches, che
 * nell'app Android sono due schermate distinte raggiunte dalla stessa modale.
 *
 * Come nell'originale la generazione del calendario compare solo quando il
 * torneo non ne ha uno: per rifarlo bisogna prima azzerarlo, così non si
 * sovrascrivono per sbaglio partite con risultati già inseriti.
 */

type Tab = 'squadre' | 'partite'

export function TournamentDetailScreen() {
  const navigate = useNavigate()
  const { key = '' } = useParams()
  const { tournaments, loading } = useTournaments()
  const { players } = usePlayers()

  const [tab, setTab] = useState<Tab>('squadre')
  const [editingTeam, setEditingTeam] = useState<Team | null>(null)
  const [teamModalOpen, setTeamModalOpen] = useState(false)
  const [editingMatch, setEditingMatch] = useState<Match | null>(null)
  const [matchModalOpen, setMatchModalOpen] = useState(false)
  const [calendarOpen, setCalendarOpen] = useState(false)
  const [finalsOpen, setFinalsOpen] = useState(false)
  const [teamToDelete, setTeamToDelete] = useState<Team | null>(null)
  const [matchToDelete, setMatchToDelete] = useState<Match | null>(null)
  const [clearingCalendar, setClearingCalendar] = useState(false)

  const tournament = useMemo(
    () => tournaments.find((t) => t.key === key) ?? null,
    [tournaments, key],
  )

  if (loading) return <p className="p-6 text-list-text-secondary">Caricamento…</p>

  if (tournament === null) {
    return (
      <div className="mx-auto max-w-2xl p-4">
        <ScreenHeader title="Torneo" onBack={() => navigate('/admin/tornei')} />
        <p className="text-list-text-secondary">Torneo non trovato.</p>
      </div>
    )
  }

  const hasCalendar = tournament.matches.length > 0
  const locked = tournament.locked

  return (
    <div className="mx-auto max-w-2xl p-4 pb-28">
      <ScreenHeader
        title={tournament.name.length > 0 ? tournament.name : 'Torneo'}
        onBack={() => navigate('/admin/tornei')}
        right={
          <button
            type="button"
            onClick={() => void setLocked(tournament.key, !locked)}
            className={`app-title rounded-lg px-3 py-1.5 text-xs transition
                        ${locked
                          ? 'bg-action-warning/20 text-action-warning hover:bg-action-warning/30'
                          : 'bg-icon-action text-list-text hover:brightness-150'}`}
          >
            {locked ? 'SBLOCCA' : 'CONCLUDI'}
          </button>
        }
      />

      {locked && (
        <p className="mb-3 rounded-[10px] border border-action-warning/40 bg-action-warning/10
                      px-3 py-2 text-sm text-action-warning">
          Torneo concluso: sola lettura. Sblocca per modificarlo.
        </p>
      )}

      <div className="mb-3 grid grid-cols-2 gap-1 rounded-lg border border-list-card-border bg-list-card p-1">
        <TabButton active={tab === 'squadre'} onClick={() => setTab('squadre')}>
          Squadre ({tournament.teams.length})
        </TabButton>
        <TabButton active={tab === 'partite'} onClick={() => setTab('partite')}>
          Partite ({tournament.matches.length})
        </TabButton>
      </div>

      {tab === 'squadre' && (
        <>
          {tournament.teams.length === 0 ? (
            <p className="text-list-text-muted">Il torneo non ha ancora squadre.</p>
          ) : (
            <ul className="flex flex-col gap-2">
              {tournament.teams.map((team) => (
                <li key={team.key}>
                  <TeamManageCard
                    team={team}
                    teamNumber={getTeamNumber(tournament.teams, team.key)}
                    // Su torneo bloccato le azioni sulle righe spariscono:
                    // rimane la sola visualizzazione, come su Android.
                    onEdit={locked ? undefined : () => {
                      setEditingTeam(team)
                      setTeamModalOpen(true)
                    }}
                    onDelete={locked ? undefined : () => setTeamToDelete(team)}
                  />
                </li>
              ))}
            </ul>
          )}
        </>
      )}

      {tab === 'partite' && (
        <>
          {!locked && (
            <div className="mb-3 flex flex-wrap gap-2">
              {hasCalendar ? (
                <>
                  <SmallButton onClick={() => setFinalsOpen(true)}>genera finali</SmallButton>
                  <SmallButton onClick={() => setClearingCalendar(true)} danger>
                    azzera calendario
                  </SmallButton>
                </>
              ) : (
                <SmallButton onClick={() => setCalendarOpen(true)}>genera calendario</SmallButton>
              )}
            </div>
          )}

          {!hasCalendar ? (
            <p className="text-list-text-muted">
              {locked
                ? 'Nessuna partita in questo torneo.'
                : 'Nessuna partita. Genera il calendario, oppure aggiungi le partite una a una.'}
            </p>
          ) : (
            <ul className="flex flex-col gap-2">
              {tournament.matches.map((match) => (
                <li key={match.key}>
                  <MatchManageRow
                    match={match}
                    tournament={tournament}
                    // Su torneo bloccato le icone play/edit/delete spariscono:
                    // resta solo la riga con punteggio e squadre.
                    onPlay={locked ? undefined : () =>
                      navigate(`/segnapunti?torneo=${tournament.key}&partita=${match.key}`)
                    }
                    onEdit={locked ? undefined : () => {
                      setEditingMatch(match)
                      setMatchModalOpen(true)
                    }}
                    onDelete={locked ? undefined : () => setMatchToDelete(match)}
                  />
                </li>
              ))}
            </ul>
          )}
        </>
      )}

      {!locked && (
        <div className="fixed inset-x-0 bottom-0 border-t border-list-card-border bg-score-bg-bottom/95 p-4 backdrop-blur">
          <div className="mx-auto max-w-2xl">
            <Button
              onClick={() => {
                if (tab === 'squadre') {
                  setEditingTeam(null)
                  setTeamModalOpen(true)
                } else {
                  setEditingMatch(null)
                  setMatchModalOpen(true)
                }
              }}
              className="w-full"
            >
              {tab === 'squadre' ? 'Nuova squadra' : 'Nuova partita'}
            </Button>
          </div>
        </div>
      )}

      <TeamEditModal
        tournament={tournament}
        team={editingTeam}
        players={players}
        open={teamModalOpen}
        onClose={() => setTeamModalOpen(false)}
      />

      <MatchEditModal
        tournament={tournament}
        match={editingMatch}
        open={matchModalOpen}
        onClose={() => setMatchModalOpen(false)}
      />

      <CalendarModal
        tournament={tournament}
        open={calendarOpen}
        onClose={() => setCalendarOpen(false)}
      />

      <FinalStagesModal
        tournament={tournament}
        open={finalsOpen}
        onClose={() => setFinalsOpen(false)}
      />

      <ConfirmDialog
        open={teamToDelete !== null}
        title={`Eliminare Team ${teamToDelete === null ? '' : getTeamNumber(tournament.teams, teamToDelete.key)}?`}
        message="Le partite che la citano resteranno in calendario, ma senza avversario riconoscibile."
        confirmLabel="Elimina"
        onConfirm={() => {
          if (teamToDelete !== null) void deleteTeam(tournament.key, teamToDelete.key)
          setTeamToDelete(null)
        }}
        onCancel={() => setTeamToDelete(null)}
      />

      <ConfirmDialog
        open={matchToDelete !== null}
        title="Eliminare la partita?"
        message="Il risultato verrà perso e la classifica si aggiorna di conseguenza."
        confirmLabel="Elimina"
        onConfirm={() => {
          if (matchToDelete !== null) void deleteMatch(tournament.key, matchToDelete.key)
          setMatchToDelete(null)
        }}
        onCancel={() => setMatchToDelete(null)}
      />

      <ConfirmDialog
        open={clearingCalendar}
        title="Azzerare il calendario?"
        message="Tutte le partite e i gironi verranno cancellati, risultati compresi. Le squadre restano."
        confirmLabel="Azzera"
        onConfirm={() => {
          void deleteAllMatches(
            tournament.key,
            tournament.teams.map((t) => t.key),
          )
          setClearingCalendar(false)
        }}
        onCancel={() => setClearingCalendar(false)}
      />
    </div>
  )
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`app-title rounded px-2 py-2 text-sm transition ${
        active ? 'bg-action-selected text-white' : 'text-list-text-secondary hover:text-list-text'
      }`}
    >
      {children}
    </button>
  )
}

function SmallButton({
  onClick,
  danger = false,
  children,
}: {
  onClick: () => void
  danger?: boolean
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`shrink-0 rounded-lg bg-icon-action px-3 py-1.5 text-sm hover:brightness-150 ${
        danger ? 'text-action-delete' : 'text-list-text'
      }`}
    >
      {children}
    </button>
  )
}
