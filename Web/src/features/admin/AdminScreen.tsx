import { Link, useNavigate } from 'react-router-dom'
import { ScreenHeader } from '@/components/ui/ScreenHeader'
import { DB_ROOT, IS_PRODUCTION_DATA } from '@/data/firebase'
import { useAuthStore } from '@/store/authStore'

/** Bivio dell'area di gestione. Porta TournamentActivityManage. */
export function AdminScreen() {
  const navigate = useNavigate()
  const logout = useAuthStore((s) => s.logout)

  const onLogout = async () => {
    await logout()
    navigate('/', { replace: true })
  }

  return (
    <div className="mx-auto max-w-2xl p-4">
      <ScreenHeader
        title="GESTIONE"
        onBack={() => navigate('/')}
        right={
          <button
            type="button"
            onClick={() => void onLogout()}
            className="app-title text-xs text-list-text-muted transition hover:text-list-text"
          >
            ESCI
          </button>
        }
      />

      <p className="mb-4 text-sm text-list-text-muted">
        Stai scrivendo su <code className="text-list-highlight-text">{DB_ROOT}</code>
        {IS_PRODUCTION_DATA && (
          <span className="ml-2 font-bold text-action-warning">— dati reali, condivisi con l’app Android</span>
        )}
      </p>

      <nav className="flex flex-col gap-3">
        <AdminLink to="/admin/giocatori" label="GIOCATORI" hint="anagrafica, statistiche, archivio" />
        <AdminLink to="/admin/tornei" label="TORNEI" hint="attivazione, calendario, eliminazione" />
      </nav>
    </div>
  )
}

function AdminLink({ to, label, hint }: { to: string; label: string; hint: string }) {
  return (
    <Link
      to={to}
      className="rounded-xl border border-list-card-border bg-list-card px-5 py-4
                 transition hover:border-brand-blue hover:bg-score-panel"
    >
      <span className="block text-lg font-bold tracking-wide">{label}</span>
      <span className="block text-sm text-list-text-muted">{hint}</span>
    </Link>
  )
}
