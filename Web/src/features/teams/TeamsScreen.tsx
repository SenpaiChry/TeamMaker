import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { usePlayers } from '@/hooks/usePlayers'
import { useTeamGenerator } from '@/hooks/useTeamGenerator'
import { resolveSelected, useSelectionStore } from '@/store/selectionStore'
import { getTeamVote } from '@/domain/team'
import { Button } from '@/components/ui/Button'
import { TeamCard } from '@/components/ui/TeamCard'
import { ScreenHeader } from '@/components/ui/ScreenHeader'
import { SaveTournamentModal } from './SaveTournamentModal'

/**
 * Squadre generate, con la possibilità di rigenerare.
 * Porta ActivityTeams + PlayerTeamsAdapter.
 */
export function TeamsScreen() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const playersPerTeam = Number(params.get('perSquadra') ?? 0)
  // Flag di contesto: la generazione parte dall'admin («Nuovo torneo») o da
  // «Crea squadre» in home? Scarto, voti e tasto di salvataggio compaiono solo
  // nel primo caso — nel secondo servono squadre da giocare, non da amministrare.
  const perTorneo = params.get('contesto') === 'torneo'

  const { players } = usePlayers()
  const selectedKeys = useSelectionStore((s) => s.selectedKeys)
  const teams = useSelectionStore((s) => s.teams)
  const setTeams = useSelectionStore((s) => s.setTeams)

  const { running, result, retries, error, generate, workerCount } = useTeamGenerator()
  const [savingTournament, setSavingTournament] = useState(false)

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
          {/*
            Scarto e voti solo per chi gestisce. In ActivityTeams il riquadro
            della differenza e il tasto di salvataggio sono `View.GONE` quando
            la generazione non è per un torneo, e PlayerTeamsAdapter nasconde
            il voto negli stessi casi: sono numeri che servono a chi compone le
            squadre, non a chi le gioca.
          */}
          {perTorneo && (
            <p className="mb-3 text-sm text-list-text-muted">
              Scarto fra la squadra più forte e la più debole:{' '}
              <span className="font-bold text-list-highlight-text tabular-nums">
                {String(spread).replace('.', ',')}
              </span>
              {result !== null && <> · trovata in {result.retries.toLocaleString('it')} tentativi</>}
            </p>
          )}

          <ul className="flex flex-col gap-2">
            {teams.map((team, i) => (
              <li key={team.key}>
                <TeamCard team={team} teamNumber={i + 1} showVote={perTorneo} showBracket={false} />
              </li>
            ))}
          </ul>
        </>
      )}

      <div
        className="fixed inset-x-0 bottom-0 border-t border-list-card-border
                   bg-score-bg-bottom/95 p-4 backdrop-blur"
      >
        <div className="mx-auto flex max-w-2xl gap-2">
          <Button
            variant="ghost"
            disabled={running}
            onClick={() => generate(selected, playersPerTeam)}
            className="grow"
          >
            RIGENERA
          </Button>
          {/* Salvare come torneo è un'azione di gestione: in Android il tasto
              esiste solo sul percorso che parte dalla gestione tornei. */}
          {perTorneo && (
            <Button
              variant="confirm"
              disabled={running || teams.length === 0}
              onClick={() => setSavingTournament(true)}
              className="grow"
            >
              Salva come torneo
            </Button>
          )}
        </div>
      </div>

      <SaveTournamentModal
        teams={teams}
        open={savingTournament}
        onClose={() => setSavingTournament(false)}
      />
    </div>
  )
}
