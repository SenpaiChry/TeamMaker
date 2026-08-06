import { useNavigate } from 'react-router-dom'
import { useLiveMatch } from '@/hooks/useLiveMatch'
import { ScreenHeader } from '@/components/ui/ScreenHeader'

/**
 * Partita in diretta, sola lettura. Porta ActivityLiveMatch.
 * Non ha comandi: si aggiorna da sola quando il segnapunti scrive.
 */
export function LiveScreen() {
  const navigate = useNavigate()
  const { live, isLive, loading } = useLiveMatch()

  if (loading) return <p className="p-6 text-list-text-secondary">Caricamento…</p>

  return (
    <div className="mx-auto max-w-3xl p-4">
      <ScreenHeader
        title="DIRETTA"
        onBack={() => navigate('/')}
        right={
          isLive ? (
            <span className="animate-pulse rounded bg-action-danger px-2 py-1 text-xs font-bold text-white">
              LIVE
            </span>
          ) : undefined
        }
      />

      {!isLive || live === null ? (
        <p className="mt-10 text-center text-list-text-secondary">
          Nessuna partita in diretta al momento.
        </p>
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3">
            <LiveSide
              name={live.team1Name}
              players={live.team1Players}
              points={live.points1}
              sets={live.sets1}
              accent="text-score-team-a"
              border="border-score-team-a/40"
            />
            <LiveSide
              name={live.team2Name}
              players={live.team2Players}
              points={live.points2}
              sets={live.sets2}
              accent="text-score-team-b"
              border="border-score-team-b/40"
            />
          </div>

          <p className="mt-4 text-center text-xs text-list-text-muted">
            Ultimo aggiornamento:{' '}
            {new Date(live.timestamp).toLocaleTimeString('it', {
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit',
            })}
          </p>
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
  accent,
  border,
}: {
  name: string
  players: string
  points: number
  sets: number
  accent: string
  border: string
}) {
  return (
    <section className={`rounded-xl border-2 ${border} bg-score-panel p-4 text-center`}>
      <h2 className={`text-lg font-black tracking-wide ${accent}`}>{name}</h2>
      {players.length > 0 && (
        <p className="mt-1 text-xs text-score-player-name/80">{players}</p>
      )}
      <div className={`my-3 text-7xl font-black tabular-nums sm:text-8xl ${accent}`}>{points}</div>
      <div className="border-t border-score-divider pt-2 text-sm text-list-text-muted">
        set <b className={`text-xl tabular-nums ${accent}`}>{sets}</b>
      </div>
    </section>
  )
}
