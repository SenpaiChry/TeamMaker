import { useEffect, useMemo, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { usePlayers } from '@/hooks/usePlayers'
import { useTeamGenerator } from '@/hooks/useTeamGenerator'
import { resolveSelected, useSelectionStore } from '@/store/selectionStore'
import { countFemales, getTeamVote } from '@/domain/team'
import { getVote } from '@/domain/player'
import { Button } from '@/components/ui/Button'
import { PlayerName } from '@/components/ui/PlayerName'
import { ScreenHeader } from '@/components/ui/ScreenHeader'

/**
 * Squadre generate, con la possibilità di rigenerare.
 * Porta ActivityTeams + PlayerTeamsAdapter.
 */
export function TeamsScreen() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const playersPerTeam = Number(params.get('perSquadra') ?? 0)

  const { players } = usePlayers()
  const selectedKeys = useSelectionStore((s) => s.selectedKeys)
  const teams = useSelectionStore((s) => s.teams)
  const setTeams = useSelectionStore((s) => s.setTeams)

  const { running, result, retries, error, generate, workerCount } = useTeamGenerator()

  const selected = useMemo(() => resolveSelected(players, selectedKeys), [players, selectedKeys])

  // Genera una volta all'arrivo sulla schermata, quando i giocatori sono pronti.
  const started = useRef(false)
  useEffect(() => {
    if (started.current || selected.length === 0 || playersPerTeam <= 0) return
    started.current = true
    generate(selected, playersPerTeam)
  }, [selected, playersPerTeam, generate])

  useEffect(() => {
    if (result !== null) setTeams(result.teams, playersPerTeam)
  }, [result, playersPerTeam, setTeams])

  const spread = useMemo(() => {
    if (teams.length === 0) return 0
    const votes = teams.map(getTeamVote)
    return Math.round((Math.max(...votes) - Math.min(...votes)) * 10) / 10
  }, [teams])

  if (playersPerTeam <= 0 || (selected.length === 0 && !running)) {
    return (
      <div className="mx-auto max-w-2xl p-4">
        <ScreenHeader title="SQUADRE" onBack={() => navigate('/genera')} />
        <p className="text-list-text-secondary">
          Nessuna selezione attiva. Torna indietro e scegli i giocatori.
        </p>
      </div>
    )
  }

  return (
    <div className="mx-auto flex min-h-dvh max-w-2xl flex-col p-4 pb-28">
      <ScreenHeader title="SQUADRE" onBack={() => navigate('/genera')} />

      {running && (
        <div className="rounded-lg border border-list-card-border bg-list-card p-6 text-center">
          <p className="font-bold">Cerco l’equilibrio migliore…</p>
          <p className="mt-2 text-sm text-list-text-muted">
            {retries === 0
              ? `${workerCount} ricerche in parallelo`
              : `${retries.toLocaleString('it')} tentativi`}
          </p>
          <div className="mx-auto mt-4 h-1 w-40 overflow-hidden rounded-full bg-score-panel">
            <div className="h-full w-1/3 animate-pulse rounded-full bg-brand-blue" />
          </div>
        </div>
      )}

      {error !== null && (
        <div className="rounded-lg border border-action-danger/50 bg-action-danger/10 p-4">
          <p className="font-semibold text-action-danger">Generazione non riuscita</p>
          <p className="mt-1 text-sm text-list-text-secondary">{error}</p>
        </div>
      )}

      {!running && teams.length > 0 && (
        <>
          <p className="mb-3 text-sm text-list-text-muted">
            Scarto fra la squadra più forte e la più debole:{' '}
            <span className="font-bold text-list-highlight-text tabular-nums">
              {String(spread).replace('.', ',')}
            </span>
            {result !== null && <> · trovata in {result.retries.toLocaleString('it')} tentativi</>}
          </p>

          <ul className="flex flex-col gap-3">
            {teams.map((team, i) => (
              <li
                key={team.key}
                className="rounded-lg border border-list-card-border bg-list-card p-3"
              >
                <div className="mb-2 flex items-center justify-between gap-3">
                  <h2 className="font-bold tracking-wide text-list-highlight-text">
                    SQUADRA {i + 1}
                  </h2>
                  <div className="flex shrink-0 items-center gap-2">
                    {countFemales(team) > 0 && (
                      <span className="text-sm text-women-dark" title="giocatrici">
                        ♀&nbsp;{countFemales(team)}
                      </span>
                    )}
                    <span
                      className="rounded-full bg-score-panel px-3 py-1 text-sm font-bold tabular-nums"
                      title="voto totale della squadra"
                    >
                      {String(getTeamVote(team)).replace('.', ',')}
                    </span>
                  </div>
                </div>
                <ul className="flex flex-col divide-y divide-list-card-border">
                  {team.players.map((player) => (
                    <li key={player.key} className="flex items-center justify-between py-1.5">
                      <PlayerName player={player} showNickname={false} />
                      <span className="text-sm tabular-nums text-list-text-muted">
                        {getVote(player)}
                      </span>
                    </li>
                  ))}
                </ul>
              </li>
            ))}
          </ul>
        </>
      )}

      <div
        className="fixed inset-x-0 bottom-0 border-t border-list-card-border
                   bg-score-bg-bottom/95 p-4 backdrop-blur"
      >
        <div className="mx-auto max-w-2xl">
          <Button
            variant="ghost"
            disabled={running}
            onClick={() => generate(selected, playersPerTeam)}
            className="w-full"
          >
            RIGENERA
          </Button>
        </div>
      </div>
    </div>
  )
}
