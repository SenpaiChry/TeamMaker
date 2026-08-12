import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

/**
 * Accesso all'area di gestione, ricalcato su activity_login.xml: tasto
 * indietro in alto a sinistra, etichetta PASSWORD, campo stondato centrato,
 * messaggio di errore in rosso e tasto LOGIN staccato più in basso.
 *
 * Porta ActivityLogin, compreso il fatto che il messaggio di errore compare
 * solo dopo un tentativo sbagliato e resta finché non si riprova.
 */
export function LoginScreen() {
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)
  const [password, setPassword] = useState('')
  const [wrong, setWrong] = useState(false)

  const submit = () => {
    if (login(password)) navigate('/admin', { replace: true })
    else setWrong(true)
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <div className="p-3">
        <button
          type="button"
          onClick={() => navigate('/')}
          aria-label="Indietro"
          className="grid size-11 place-items-center rounded-full text-list-text transition
                     hover:bg-list-card active:scale-95"
        >
          {/* Stessa freccia SVG di ScreenHeader per uniformità. */}
          <svg
            viewBox="0 0 24 24"
            className="size-7"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden
          >
            <path d="M15 6l-6 6 6 6" />
          </svg>
        </button>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          submit()
        }}
        className="flex grow flex-col items-center justify-center px-6"
      >
        <label htmlFor="password" className="app-title mb-2 text-xl text-list-text-secondary">
          Password
        </label>

        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => {
            setPassword(e.target.value)
            setWrong(false)
          }}
          autoFocus
          className="app-title w-60 max-w-full rounded-[20px] border border-search-box-border
                     bg-search-box px-4 py-3 text-center text-xl text-list-text
                     focus:border-brand-blue focus:outline-none"
        />

        <p className="app-title mt-4 h-6 text-base text-action-danger">
          {wrong && 'Password errata'}
        </p>

        <button
          type="submit"
          className="app-title mt-14 w-50 max-w-full rounded-[11px] bg-brand-blue py-3
                     text-lg text-white transition hover:bg-brand-blue-pressed"
        >
          Login
        </button>
      </form>
    </div>
  )
}
