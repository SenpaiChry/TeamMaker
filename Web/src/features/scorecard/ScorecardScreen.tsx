import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useTournaments } from '@/hooks/useTournaments'
import {
  changePoints,
  changeSets,
  INITIAL_SCORE,
  pointsOnSide,
  resultToSave,
  setsOnSide,
  swapSides,
  teamOnSide,
  type ScoreState,
  type Side,
} from '@/domain/scorecard'
import { getNameAndSurname } from '@/domain/player'
import { getTeamNumber } from '@/domain/team'
import {
  cancelLiveShutdown,
  clearLiveMatch,
  reserveLiveShutdown,
  writeLiveMatch,
} from '@/data/liveMatchRepo'
import { saveMatchResult } from '@/data/matchesRepo'
import { Button } from '@/components/ui/Button'
import { ScreenHeader } from '@/components/ui/ScreenHeader'
import { RepeatButton } from '@/components/ui/RepeatButton'

/**
 * Segnapunti. Porta ActivityScorecard.
 *
 * Funziona in due modi:
 *   - libero (`/segnapunti`): due squadre generiche, non scrive nulla
 *   - di torneo (`/segnapunti?torneo=…&partita=…`): trasmette il punteggio in
 *     diretta e a fine partita lo salva sulla partita
 *
 * Il colore segue la SQUADRA, non il lato: dopo uno swap si sposta con lei.
 */

/** Ogni quanto ritrasmettere il punteggio anche senza tocchi, come nel Java. */
const LIVE_HEARTBEAT_MS = 30_000

export function ScorecardScreen() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const tournamentKey = params.get('torneo')
  const matchKey = params.get('partita')

  const { tournaments, loading } = useTournaments()
  const [score, setScore] = useState<ScoreState>(INITIAL_SCORE)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)

  const tournament = useMemo(
    () => tournaments.find((t) => t.key === tournamentKey) ?? null,
    [tournaments, tournamentKey],
  )
  const match = useMemo(
    () => tournament?.matches.find((m) => m.key === matchKey) ?? null,
    [tournament, matchKey],
  )
  const isTournamentMatch = tournament !== null && match !== null

  const teamA = tournament?.teams.find((t) => t.key === match?.keyTeam1)
  const teamB = tournament?.teams.find((t) => t.key === match?.keyTeam2)

  const titleA = isTournamentMatch ? `TEAM ${getTeamNumber(tournament.teams, match.keyTeam1)}` : 'TEAM 1'
  const titleB = isTournamentMatch ? `TEAM ${getTeamNumber(tournament.teams, match.keyTeam2)}` : 'TEAM 2'
  const playersA = useMemo(() => (teamA?.players ?? []).map((p) => getNameAndSurname(p, 20)), [teamA])
  const playersB = useMemo(() => (teamB?.players ?? []).map((p) => getNameAndSurname(p, 20)), [teamB])

  // Lo stato più recente, per la trasmissione periodica senza rilanciare il timer.
  const scoreRef = useRef(score)
  scoreRef.current = score

  const broadcast = useCallback(
    (state: ScoreState) => {
      if (!isTournamentMatch) return
      void writeLiveMatch({
        active: true,
        tournamentKey: tournament.key,
        matchPosition: tournament.matches.indexOf(match),
        team1Name: titleA,
        team2Name: titleB,
        team1Players: playersA.join(', '),
        team2Players: playersB.join(', '),
        points1: state.points1,
        points2: state.points2,
        sets1: state.sets1,
        sets2: state.sets2,
      })
    },
    [isTournamentMatch, tournament, match, titleA, titleB, playersA, playersB],
  )

  // `broadcast` cambia identità a ogni aggiornamento che arriva da Firebase,
  // perché dipende dagli oggetti del torneo che vengono ricreati. Tenerlo in un
  // ref evita che l'effetto qui sotto si riavvii di continuo, spegnendo e
  // riaccendendo la diretta a ogni giro.
  const broadcastRef = useRef(broadcast)
  broadcastRef.current = broadcast

  // Trasmissione: subito all'apertura, poi ogni 30 secondi.
  // Alla chiusura la diretta si spegne; e per i casi in cui il nostro codice
  // non gira affatto (scheda chiusa, ricarica, rete caduta) la prenotiamo sul
  // server con onDisconnect, altrimenti la partita resterebbe live per sempre.
  useEffect(() => {
    if (tournamentKey === null || matchKey === null) return

    reserveLiveShutdown()
    const timer = setInterval(() => broadcastRef.current(scoreRef.current), LIVE_HEARTBEAT_MS)

    return () => {
      clearInterval(timer)
      cancelLiveShutdown()
      void clearLiveMatch()
    }
  }, [tournamentKey, matchKey])

  // Prima trasmissione appena la partita è stata risolta: all'apertura i tornei
  // non sono ancora arrivati da Firebase, quindi l'effetto qui sopra da solo
  // trasmetterebbe a vuoto e la diretta partirebbe solo al primo battito.
  useEffect(() => {
    if (isTournamentMatch) broadcastRef.current(scoreRef.current)
  }, [isTournamentMatch])

  const apply = (next: ScoreState) => {
    setScore(next)
    setSaved(false)
    broadcast(next)
  }

  const onSave = async () => {
    if (!isTournamentMatch) return
    setSaving(true)
    try {
      const [p1, p2] = resultToSave(score)
      await saveMatchResult(tournament.key, match.key, p1, p2)
      setSaved(true)
    } finally {
      setSaving(false)
    }
  }

  if (loading && tournamentKey !== null) {
    return <p className="p-6 text-list-text-secondary">Caricamento partita…</p>
  }

  if (tournamentKey !== null && !isTournamentMatch) {
    return (
      <div className="mx-auto max-w-2xl p-4">
        <ScreenHeader title="SEGNAPUNTI" onBack={() => navigate('/torneo')} />
        <p className="text-list-text-secondary">Partita non trovata.</p>
      </div>
    )
  }

  return (
    <div className="mx-auto flex min-h-dvh max-w-3xl flex-col p-4">
      <ScreenHeader
        title="SEGNAPUNTI"
        onBack={() => navigate(isTournamentMatch ? '/torneo' : '/')}
        right={
          <button
            type="button"
            onClick={() => apply(swapSides(score))}
            className="shrink-0 rounded-lg border border-list-card-border bg-list-card
                       px-3 py-2 text-sm font-bold hover:bg-score-panel"
          >
            ⇄ INVERTI
          </button>
        }
      />

      {isTournamentMatch && (
        <p className="mb-3 text-center text-sm text-list-text-muted">
          Giorno {match.day} · ore {match.time}
          {match.type.trim().length > 0 && <> · {match.type.trim()}</>}
          <span className="ml-2 rounded bg-action-danger px-2 py-0.5 text-xs font-bold text-white">
            IN DIRETTA
          </span>
        </p>
      )}

      <div className="grid grow grid-cols-2 gap-3">
        <SidePanel
          side={1}
          score={score}
          title={teamOnSide(score, 1) === 1 ? titleA : titleB}
          players={teamOnSide(score, 1) === 1 ? playersA : playersB}
          onPoints={(d) => apply(changePoints(score, 1, d))}
          onSets={(d) => apply(changeSets(score, 1, d))}
        />
        <SidePanel
          side={2}
          score={score}
          title={teamOnSide(score, 2) === 1 ? titleA : titleB}
          players={teamOnSide(score, 2) === 1 ? playersA : playersB}
          onPoints={(d) => apply(changePoints(score, 2, d))}
          onSets={(d) => apply(changeSets(score, 2, d))}
        />
      </div>

      {isTournamentMatch && (
        <div className="mt-4">
          <Button variant="confirm" onClick={onSave} disabled={saving} className="w-full">
            {saving ? 'SALVATAGGIO…' : saved ? 'SALVATO ✓' : 'SALVA RISULTATO'}
          </Button>
          <p className="mt-2 text-center text-xs text-list-text-muted">
            {(() => {
              const [p1, p2] = resultToSave(score)
              const bySets = score.sets1 !== 0 || score.sets2 !== 0
              return `Verrà salvato ${p1} – ${p2} (${bySets ? 'set' : 'punti'})`
            })()}
          </p>
        </div>
      )}
    </div>
  )
}

function SidePanel({
  side,
  score,
  title,
  players,
  onPoints,
  onSets,
}: {
  side: Side
  score: ScoreState
  title: string
  players: string[]
  onPoints: (delta: number) => void
  onSets: (delta: number) => void
}) {
  // Il colore segue la squadra: dopo lo swap si sposta con lei.
  const isTeamA = teamOnSide(score, side) === 1
  const accent = isTeamA ? 'text-score-team-a' : 'text-score-team-b'
  const border = isTeamA ? 'border-score-team-a/40' : 'border-score-team-b/40'

  return (
    <section className={`flex flex-col rounded-xl border-2 ${border} bg-score-panel p-3`}>
      <h2 className={`text-center text-lg font-black tracking-wide ${accent}`}>{title}</h2>

      {players.length > 0 && (
        <ul className="mb-2 text-center text-xs text-score-player-name/80">
          {players.map((name) => (
            <li key={name} className="truncate">
              {name}
            </li>
          ))}
        </ul>
      )}

      <button
        type="button"
        onClick={() => onPoints(+1)}
        className={`my-2 text-7xl font-black tabular-nums sm:text-8xl ${accent}`}
        aria-label={`Aggiungi un punto a ${title}`}
      >
        {pointsOnSide(score, side)}
      </button>

      <div className="flex items-center justify-center gap-2">
        <RepeatButton onTrigger={() => onPoints(-1)} label="−" />
        <RepeatButton onTrigger={() => onPoints(+1)} label="+" />
      </div>

      <div className="mt-4 flex items-center justify-center gap-3 border-t border-score-divider pt-3">
        <button
          type="button"
          onClick={() => onSets(-1)}
          className="size-8 rounded-lg border border-score-panel-border text-lg leading-none"
          aria-label={`Togli un set a ${title}`}
        >
          −
        </button>
        <span className="text-sm text-list-text-muted">
          set <b className={`text-xl tabular-nums ${accent}`}>{setsOnSide(score, side)}</b>
        </span>
        <button
          type="button"
          onClick={() => onSets(+1)}
          className="size-8 rounded-lg border border-score-panel-border text-lg leading-none"
          aria-label={`Aggiungi un set a ${title}`}
        >
          +
        </button>
      </div>
    </section>
  )
}
