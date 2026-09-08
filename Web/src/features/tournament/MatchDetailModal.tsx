import { useNavigate } from 'react-router-dom'
import type { Match, Team, Tournament } from '@/domain/models'
import { getTeamNumber } from '@/domain/team'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { PlayerName } from '@/components/ui/PlayerName'
import { label as phaseLabel } from '@/domain/phases'
import { slotLabel, TO_DO_KEY } from '@/domain/finalStages'
import { formatSetDetail } from '@/domain/matchScore'
import { pointsForMatch } from '@/domain/standings'

/**
 * Dettaglio di una partita: formazioni e punti assegnati in classifica.
 * Porta ActivityPopUpInfoMatch.
 */
export function MatchDetailModal({
  match,
  tournament,
  onClose,
}: {
  match: Match | null
  tournament: Tournament
  onClose: () => void
}) {
  const navigate = useNavigate()
  const teams = tournament.teams
  const team1 = teams.find((t) => t.key === match?.keyTeam1)
  const team2 = teams.find((t) => t.key === match?.keyTeam2)
  const [awarded1, awarded2] = match === null ? [0, 0] : pointsForMatch(match)

  return (
    <Modal
      open={match !== null}
      onClose={onClose}
      title={
        match === null ? null : (
          <div>
            <div className="text-sm text-list-text-muted">
              Giorno {match.day} · ore {match.time}
              {match.type.trim().length > 0 && <> · {phaseLabel(match.type)}</>}
            </div>
            <div className="mt-1 flex items-baseline justify-center gap-3 text-3xl font-black tabular-nums">
              <span className="text-score-team-a">{match.points1}</span>
              <span className="text-lg text-list-text-muted">–</span>
              <span className="text-score-team-b">{match.points2}</span>
            </div>
            {formatSetDetail(match) !== null && (
              <div className="mt-1 text-center text-[12px] tabular-nums text-list-text-muted">
                {formatSetDetail(match)}
              </div>
            )}
          </div>
        )
      }
    >
      {match !== null && (
        <>
          <div className="grid grid-cols-2 gap-3">
            <TeamColumn
              label={teamOrPlaceholder(tournament, match, 1)}
              team={team1}
              awarded={awarded1}
              accent="text-score-team-a"
            />
            <TeamColumn
              label={teamOrPlaceholder(tournament, match, 2)}
              team={team2}
              awarded={awarded2}
              accent="text-score-team-b"
            />
          </div>

          <div className="mt-5 flex gap-2">
            <Button variant="ghost" onClick={onClose} className="grow">
              CHIUDI
            </Button>
            <Button
              onClick={() => navigate(`/segnapunti?torneo=${tournament.key}&partita=${match.key}`)}
              className="grow"
            >
              SEGNAPUNTI
            </Button>
          </div>
        </>
      )}
    </Modal>
  )
}

function teamOrPlaceholder(tournament: Tournament, match: Match, slot: 1 | 2): string {
  return slotLabel(tournament, match, slot, (key) => {
    if (key.length === 0 || key === TO_DO_KEY) return 'TEAM ?'
    const n = getTeamNumber(tournament.teams, key)
    return n > 0 ? `TEAM ${n}` : 'TEAM ?'
  })
}

function TeamColumn({
  label,
  team,
  awarded,
  accent,
}: {
  label: string
  team: Team | undefined
  awarded: number
  accent: string
}) {
  return (
    <div className="rounded-lg border border-list-card-border bg-list-card p-2">
      <div className={`mb-1 text-sm font-bold ${accent}`}>{label}</div>
      {team === undefined ? (
        <p className="text-sm text-list-text-muted">Squadra non trovata</p>
      ) : (
        <ul className="flex flex-col gap-1">
          {team.players.map((player) => (
            <li key={player.key} className="truncate text-sm">
              <PlayerName player={player} showNickname={false} />
            </li>
          ))}
        </ul>
      )}
      <div className="mt-2 border-t border-list-card-border pt-1 text-xs text-list-text-muted">
        {awarded} {awarded === 1 ? 'punto' : 'punti'} in classifica
      </div>
    </div>
  )
}
