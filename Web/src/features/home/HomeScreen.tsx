import { Link } from 'react-router-dom'
import { useLiveMatch } from '@/hooks/useLiveMatch'
import { useAuthStore } from '@/store/authStore'

/**
 * Schermata iniziale, ricalcata su activity_main.xml: logo, colonna di tasti
 * blu larghi 250dp e il tasto LIVE che compare pulsando solo quando c'è
 * davvero una partita in corso.
 *
 * Come nell'originale l'ultimo tasto porta all'accesso finché non si è entrati,
 * e alla gestione una volta dentro.
 */
export function HomeScreen() {
  const { isLive } = useLiveMatch()
  const logged = useAuthStore((s) => s.logged)

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center px-9 py-10">
      {/*
        Logo con sfondo trasparente: si posa sullo sfondo scuro dell'app senza
        il riquadro quadrato dell'icona PWA. L'altezza fa da vincolo perché il
        logo è più largo che alto (rapporto ~5:4).
      */}
      <img
        src="/logo.png"
        alt="La Foce Beach Volley Cup 2025"
        className="mb-10 h-[220px] max-w-[80vw] object-contain"
      />

      {isLive && (
        <Link
          to="/diretta"
          className="app-title mb-5 w-[250px] max-w-full animate-pulse rounded-[20px]
                     bg-action-danger py-2.5 text-center text-lg text-white"
        >
          ● Live
        </Link>
      )}

      <nav className="flex w-[250px] max-w-full flex-col gap-5">
        <HomeButton to="/genera" label="Crea squadre" />
        <HomeButton to="/torneo" label="Torneo" />
        <HomeButton to="/segnapunti" label="Segnapunti" />
        <HomeButton to={logged ? '/admin' : '/login'} label="Admin" />
      </nav>
    </div>
  )
}

function HomeButton({ to, label }: { to: string; label: string }) {
  return (
    <Link
      to={to}
      className="app-title rounded-[11px] bg-brand-blue py-2.5 text-center text-lg
                 text-white transition hover:bg-brand-blue-pressed active:bg-brand-blue-pressed"
    >
      {label}
    </Link>
  )
}
