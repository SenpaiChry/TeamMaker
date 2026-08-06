import { Link } from 'react-router-dom'
import { usePlayers } from '@/hooks/usePlayers'
import { useLiveMatch } from '@/hooks/useLiveMatch'

/**
 * Schermata iniziale. Porta MainActivity, compreso il tasto DIRETTA che
 * compare solo quando c'è davvero una partita trasmessa.
 */
export function HomeScreen() {
  const { players, loading } = usePlayers()
  const { isLive } = useLiveMatch()
  const activeCount = players.filter((p) => p.isActive).length

  return (
    <div className="mx-auto flex min-h-dvh max-w-2xl flex-col justify-center p-6">
      <h1 className="mb-2 text-center text-4xl font-black tracking-widest text-brand-orange">
        TEAM MAKER
      </h1>
      <p className="mb-8 text-center text-sm text-list-text-muted">
        {loading ? 'Caricamento…' : `${activeCount} giocatori attivi`}
      </p>

      <nav className="flex flex-col gap-3">
        {isLive && (
          <Link
            to="/diretta"
            className="flex animate-pulse items-center justify-between rounded-xl border
                       border-action-danger bg-action-danger/20 px-5 py-4 text-lg
                       font-bold tracking-wide text-white"
          >
            PARTITA IN DIRETTA
            <span aria-hidden>→</span>
          </Link>
        )}
        <HomeLink to="/genera" label="GENERA SQUADRE" />
        <HomeLink to="/giocatori" label="GIOCATORI" />
        <HomeLink to="/torneo" label="TORNEO" />
        <HomeLink to="/segnapunti" label="SEGNAPUNTI" />
        <HomeLink to="/admin" label="GESTIONE" />
      </nav>
    </div>
  )
}

function HomeLink({ to, label, hint }: { to?: string; label: string; hint?: string }) {
  const className =
    'flex items-center justify-between rounded-xl border border-list-card-border ' +
    'bg-list-card px-5 py-4 text-lg font-bold tracking-wide transition'

  if (to === undefined) {
    return (
      <span className={`${className} cursor-not-allowed opacity-35`}>
        {label}
        {hint !== undefined && <span className="text-xs font-normal">{hint}</span>}
      </span>
    )
  }

  return (
    <Link to={to} className={`${className} hover:border-brand-blue hover:bg-score-panel`}>
      {label}
      <span aria-hidden>→</span>
    </Link>
  )
}
