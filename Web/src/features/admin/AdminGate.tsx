import { useState, type ReactNode } from 'react'
import { Button } from '@/components/ui/Button'

/**
 * Sbarramento all'area di amministrazione.
 *
 * ⚠️ QUESTO NON È UNA MISURA DI SICUREZZA. È una comodità, per non aprire per
 * sbaglio l'area di gestione: la password viaggia nel bundle JavaScript, che
 * chiunque può leggere. Vale anche per l'app Android, dove la password è
 * scritta in chiaro dentro Constants.java.
 *
 * L'unica protezione reale del database sono le REGOLE del Realtime Database,
 * che vanno configurate nella console Firebase (vedi database.rules.json nella
 * radice del progetto). Finché le regole sono aperte, chiunque conosca l'URL
 * del database può scrivere, con o senza questa schermata.
 *
 * La password si imposta con VITE_ADMIN_PASSWORD in `.env.local`. Se non è
 * impostata l'area resta aperta, comodo in sviluppo.
 */

const ADMIN_PASSWORD = (import.meta.env['VITE_ADMIN_PASSWORD'] as string | undefined) ?? ''

/** Sopravvive ai cambi di pagina, non alla chiusura della scheda. */
const STORAGE_KEY = 'teammaker.admin'

export function AdminGate({ children }: { children: ReactNode }) {
  const [unlocked, setUnlocked] = useState(
    () => ADMIN_PASSWORD.length === 0 || sessionStorage.getItem(STORAGE_KEY) === 'ok',
  )
  const [attempt, setAttempt] = useState('')
  const [wrong, setWrong] = useState(false)

  if (unlocked) return <>{children}</>

  const submit = () => {
    if (attempt === ADMIN_PASSWORD) {
      sessionStorage.setItem(STORAGE_KEY, 'ok')
      setUnlocked(true)
      return
    }
    setWrong(true)
  }

  return (
    <div className="mx-auto max-w-sm p-6">
      <h1 className="mb-4 text-xl font-bold tracking-wide">ACCESSO GESTIONE</h1>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          submit()
        }}
      >
        <input
          type="password"
          value={attempt}
          onChange={(e) => {
            setAttempt(e.target.value)
            setWrong(false)
          }}
          placeholder="Password"
          autoFocus
          className="w-full rounded-lg border border-list-card-border bg-list-card px-4 py-2
                     text-list-text placeholder:text-list-text-muted
                     focus:border-list-highlight-text focus:outline-none"
        />

        {wrong && <p className="mt-2 text-sm text-action-danger">Password errata.</p>}

        <Button type="submit" className="mt-4 w-full">
          ENTRA
        </Button>
      </form>
    </div>
  )
}
