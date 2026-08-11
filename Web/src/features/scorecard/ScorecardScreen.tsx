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
    <div className="mx-auto flex min-h-dvh max-w-3xl flex-col p-3 sm:p-4">
      <ScreenHeader
        title="SEGNAPUNTI"
        onBack={() => navigate(isTournamentMatch ? '/torneo' : '/')}
        right={
          <button
            type="button"
            onClick={() => apply(swapSides(score))}
            aria-label="Inverti i lati"
            title="Inverti i lati"
            className="grid size-[42px] shrink-0 place-items-center rounded-[11px]
                       border border-list-card-border bg-list-card text-lg text-list-text
                       transition hover:bg-score-panel"
          >
            {/* Icona swap orizzontale — due frecce contrapposte. */}
            <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor"
                 strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
              <path d="M8 7H21" />
              <path d="M17 3l4 4-4 4" />
              <path d="M16 17H3" />
              <path d="M7 13l-4 4 4 4" />
            </svg>
          </button>
        }
      />

      {isTournamentMatch && (
        <div className="mb-3 flex flex-wrap items-center justify-center gap-2 text-sm">
          <span className="rounded-full border border-list-card-border bg-list-card px-3 py-1
                           text-list-text-muted">
            Giorno <b className="text-list-text">{match.day}</b> · ore{' '}
            <b className="text-list-text">{match.time}</b>
            {match.type.trim().length > 0 && (
              <>
                {' · '}
                <b className="text-list-text">{match.type.trim()}</b>
              </>
            )}
          </span>
          <span className="inline-flex items-center gap-1.5 rounded-full bg-action-danger
                           px-3 py-1 text-xs font-bold uppercase tracking-wide text-white">
            <span className="size-2 animate-pulse rounded-full bg-white" aria-hidden />
            In diretta
          </span>
        </div>
      )}

      {/*
        Come nell'app Android: portrait = squadre sopra/sotto (il telefono in
        verticale sta più comodo così), landscape = sinistra/destra.
        In portrait la barra dei SET va in mezzo alle due squadre, con i due
        contatori affiancati; in landscape sparisce e ogni pannello riprende il
        suo stepper interno.
      */}
      <div className="flex grow flex-col gap-2 landscape:flex-row sm:gap-3">
        <SidePanel
          side={1}
          score={score}
          title={teamOnSide(score, 1) === 1 ? titleA : titleB}
          players={teamOnSide(score, 1) === 1 ? playersA : playersB}
          onPoints={(d) => apply(changePoints(score, 1, d))}
          onSets={(d) => apply(changeSets(score, 1, d))}
        />
        <SetsBar
          score={score}
          titleA={titleA}
          titleB={titleB}
          onSetsSide={(side, delta) => apply(changeSets(score, side, delta))}
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
  const ribbon = isTeamA ? 'bg-score-team-a' : 'bg-score-team-b'
  const border = isTeamA ? 'border-score-team-a/60' : 'border-score-team-b/60'

  const points = pointsOnSide(score, side)
  const sets = setsOnSide(score, side)

  return (
    <section
      className={`flex grow basis-0 flex-col overflow-hidden rounded-2xl border-2 ${border}
                  bg-score-panel`}
    >
      {/* Fascia colorata col nome squadra: la squadra si riconosce a colpo d'occhio. */}
      <header className={`${ribbon} px-3 py-2`}>
        <h2 className="app-title truncate text-center text-base leading-none text-white
                       sm:text-lg">
          {title}
        </h2>
      </header>

      {players.length > 0 && (
        <ul className="border-b border-score-divider px-2 py-1.5 text-center text-[11px]
                       leading-tight text-score-player-name/80 sm:text-xs">
          {players.map((name) => (
            <li key={name} className="truncate">
              {name}
            </li>
          ))}
        </ul>
      )}

      {/* Punteggio: solo display, per non aggiungere punti per sbaglio. */}
      <div className="flex grow items-center justify-center px-2 py-3">
        <span
          className={`app-title tabular-nums leading-none ${accent}
                     text-[clamp(4rem,22vw,7.5rem)]`}
        >
          {points}
        </span>
      </div>

      {/* +/- grandi: il tocco principale del segnapunti. Il + è a destra e più
          in evidenza perché è quello che si usa 9 volte su 10. */}
      <div className="grid grid-cols-[1fr_1.4fr] gap-1.5 px-2 pb-2">
        <RepeatButton onTrigger={() => onPoints(-1)} label="−" />
        <RepeatButton onTrigger={() => onPoints(+1)} label="+" primary />
      </div>

      {/* Riga set: solo in landscape. In portrait i set stanno nella barra
          centrale fra le due squadre, per non affastellare il pannello. */}
      <div className="hidden landscape:flex items-center justify-between gap-2
                      border-t border-score-divider px-3 py-2">
        <button
          type="button"
          onClick={() => onSets(-1)}
          aria-label={`Togli un set a ${title}`}
          className="grid size-9 place-items-center rounded-lg border border-score-panel-border
                     bg-score-panel text-lg leading-none hover:bg-score-panel-border"
        >
          −
        </button>
        <div className="flex items-baseline gap-1.5">
          <span className="text-xs uppercase tracking-wide text-list-text-muted">Set</span>
          <b className={`text-2xl tabular-nums leading-none ${accent}`}>{sets}</b>
        </div>
        <button
          type="button"
          onClick={() => onSets(+1)}
          aria-label={`Aggiungi un set a ${title}`}
          className="grid size-9 place-items-center rounded-lg border border-score-panel-border
                     bg-score-panel text-lg leading-none hover:bg-score-panel-border"
        >
          +
        </button>
      </div>
    </section>
  )
}

/**
 * Barra dei set in mezzo alle due squadre (solo in portrait).
 *
 * Due stepper affiancati, uno per lato, colorati con la squadra che in quel
 * momento è sopra/sotto (i colori seguono lo swap come nei pannelli). Serve a
 * tenere il gesto naturale: il set del pannello di sopra sta sopra, quello del
 * pannello di sotto sta sotto — anche se qui la barra è orizzontale.
 */
function SetsBar({
  score,
  titleA,
  titleB,
  onSetsSide,
}: {
  score: ScoreState
  titleA: string
  titleB: string
  onSetsSide: (side: Side, delta: number) => void
}) {
  return (
    <div
      className="grid grid-cols-2 divide-x divide-list-card-border overflow-hidden
                 rounded-2xl border border-list-card-border bg-list-card landscape:hidden"
    >
      <SetsHalf
        side={1}
        score={score}
        title={teamOnSide(score, 1) === 1 ? titleA : titleB}
        onSets={(d) => onSetsSide(1, d)}
      />
      <SetsHalf
        side={2}
        score={score}
        title={teamOnSide(score, 2) === 1 ? titleA : titleB}
        onSets={(d) => onSetsSide(2, d)}
      />
    </div>
  )
}

function SetsHalf({
  side,
  score,
  title,
  onSets,
}: {
  side: Side
  score: ScoreState
  title: string
  onSets: (delta: number) => void
}) {
  const isTeamA = teamOnSide(score, side) === 1
  const accent = isTeamA ? 'text-score-team-a' : 'text-score-team-b'
  const sets = setsOnSide(score, side)

  return (
    <div className="flex items-center justify-between gap-2 px-3 py-2">
      <button
        type="button"
        onClick={() => onSets(-1)}
        aria-label={`Togli un set a ${title}`}
        className="grid size-10 place-items-center rounded-lg border border-score-panel-border
                   bg-score-panel text-xl leading-none hover:bg-score-panel-border"
      >
        −
      </button>
      <div className="flex items-baseline gap-1.5">
        <span className="text-[11px] uppercase tracking-wide text-list-text-muted">Set</span>
        <b className={`text-2xl tabular-nums leading-none ${accent}`}>{sets}</b>
      </div>
      <button
        type="button"
        onClick={() => onSets(+1)}
        aria-label={`Aggiungi un set a ${title}`}
        className="grid size-10 place-items-center rounded-lg border border-score-panel-border
                   bg-score-panel text-xl leading-none hover:bg-score-panel-border"
      >
        +
      </button>
    </div>
  )
}
