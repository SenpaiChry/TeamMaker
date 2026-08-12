import { useNavigate } from 'react-router-dom'
import { useLiveMatch } from '@/hooks/useLiveMatch'
import { ScreenHeader } from '@/components/ui/ScreenHeader'

/**
 * Partita in diretta, sola lettura. Porta ActivityLiveMatch.
 * Non ha comandi: si aggiorna da sola quando il segnapunti scrive.
 *
 * Il layout ricalca il segnapunti — fascia colorata col nome, punteggio
 * grande in mezzo, riga set — così chi guarda in diretta ha la stessa
 * "forma" di chi tiene il conteggio a bordo campo. In portrait le squadre
 * sono impilate con la barra dei set in mezzo, in landscape affiancate.
 */
export function LiveScreen() {
  const navigate = useNavigate()
  const { live, isLive, loading } = useLiveMatch()

  if (loading) return <p className="p-6 text-list-text-secondary">Caricamento…</p>

  return (
    <div className="mx-auto flex min-h-dvh max-w-3xl flex-col p-3 sm:p-4">
      <ScreenHeader
        title="DIRETTA"
        onBack={() => navigate('/')}
        right={
          isLive ? (
            <span
              className="inline-flex items-center gap-1.5 rounded-full bg-action-danger
                         px-3 py-1 text-xs font-bold uppercase tracking-wide text-white"
            >
              <span className="size-2 animate-pulse rounded-full bg-white" aria-hidden />
              Live
            </span>
          ) : undefined
        }
      />

      {!isLive || live === null ? (
        <div className="mt-20 flex flex-col items-center gap-2 text-center">
          <span
            className="grid size-14 place-items-center rounded-full border border-list-card-border
                       bg-list-card text-2xl"
            aria-hidden
          >
            ●
          </span>
          <p className="text-list-text-secondary">Nessuna partita in diretta al momento.</p>
          <p className="text-xs text-list-text-muted">
            Comparirà qui appena qualcuno apre il segnapunti su una partita di torneo.
          </p>
        </div>
      ) : (
        <>
          <div className="flex grow flex-col gap-2 landscape:flex-row sm:gap-3">
            <LiveSide
              name={live.team1Name}
              players={live.team1Players}
              points={live.points1}
              sets={live.sets1}
              team="a"
            />
            <SetsBar setsA={live.sets1} setsB={live.sets2} />
            <LiveSide
              name={live.team2Name}
              players={live.team2Players}
              points={live.points2}
              sets={live.sets2}
              team="b"
            />
          </div>
          {/* Il timestamp «Ultimo aggiornamento» era ridondante: il badge LIVE
              pulsante e il fatto stesso che il punteggio arrivi via Realtime
              Database dicono già che i dati sono in tempo reale. */}
        </>
      )}
    </div>
  )
}

function LiveSide({
  name,
  players,
  points,
  sets,
  team,
}: {
  name: string
  players: string
  points: number
  sets: number
  team: 'a' | 'b'
}) {
  const accent = team === 'a' ? 'text-score-team-a' : 'text-score-team-b'
  const ribbon = team === 'a' ? 'bg-score-team-a' : 'bg-score-team-b'
  const border = team === 'a' ? 'border-score-team-a/60' : 'border-score-team-b/60'

  return (
    <section
      className={`flex grow basis-0 flex-col overflow-hidden rounded-2xl border-2 ${border}
                  bg-score-panel`}
    >
      <header className={`${ribbon} px-3 py-2`}>
        <h2 className="app-title truncate text-center text-base leading-none text-white sm:text-lg">
          {name}
        </h2>
      </header>

      {players.length > 0 && (
        <p
          className="border-b border-score-divider px-3 py-1.5 text-center text-[11px]
                     leading-tight text-score-player-name/80 sm:text-xs"
        >
          {players}
        </p>
      )}

      <div className="flex grow items-center justify-center px-2 py-3">
        <span
          className={`app-title tabular-nums leading-none ${accent}
                     text-[clamp(4rem,22vw,7.5rem)]`}
        >
          {points}
        </span>
      </div>

      {/* Set in fondo al pannello: solo in landscape. In portrait vive nella
          barra centrale, come nel segnapunti. */}
      <div
        className="hidden landscape:flex items-center justify-center gap-2 border-t
                   border-score-divider px-3 py-2 text-sm text-list-text-muted"
      >
        <span className="text-xs uppercase tracking-wide">Set</span>
        <b className={`text-2xl tabular-nums leading-none ${accent}`}>{sets}</b>
      </div>
    </section>
  )
}

/**
 * Barra dei set fra le due squadre (solo portrait). Niente nomi: le squadre
 * sono già identificate dal colore e dal pannello sopra/sotto, ripeterli qui
 * era rumore.
 */
function SetsBar({ setsA, setsB }: { setsA: number; setsB: number }) {
  return (
    <div
      className="grid grid-cols-2 divide-x divide-list-card-border overflow-hidden
                 rounded-2xl border border-list-card-border bg-list-card landscape:hidden"
    >
      <SetsHalf sets={setsA} team="a" />
      <SetsHalf sets={setsB} team="b" />
    </div>
  )
}

function SetsHalf({ sets, team }: { sets: number; team: 'a' | 'b' }) {
  const accent = team === 'a' ? 'text-score-team-a' : 'text-score-team-b'
  return (
    <div className="flex items-center justify-center gap-2 px-4 py-2">
      <span className="text-xs uppercase tracking-wide text-list-text-muted">Set</span>
      <b className={`text-3xl tabular-nums leading-none ${accent}`}>{sets}</b>
    </div>
  )
}
