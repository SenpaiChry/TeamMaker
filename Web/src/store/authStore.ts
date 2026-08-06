import { create } from 'zustand'

/**
 * Stato dell'accesso all'area di gestione.
 *
 * Sostituisce `Constants.logged`. Come nell'app Android l'accesso vale per
 * l'intera sessione; qui sopravvive anche a un cambio di pagina, ma non alla
 * chiusura della scheda.
 *
 * ⚠️ Non è una misura di sicurezza: la password sta nel bundle JavaScript,
 * come nell'originale sta in chiaro dentro Constants.java. La protezione vera
 * del database sono le sue regole (vedi database.rules.json).
 */

const ADMIN_PASSWORD = (import.meta.env['VITE_ADMIN_PASSWORD'] as string | undefined) ?? ''
const STORAGE_KEY = 'teammaker.admin'

/** `true` se non è stata configurata nessuna password: l'area resta aperta. */
export const ADMIN_OPEN = ADMIN_PASSWORD.length === 0

interface AuthState {
  logged: boolean
  /** `true` se la password era giusta. */
  login: (password: string) => boolean
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  logged: ADMIN_OPEN || sessionStorage.getItem(STORAGE_KEY) === 'ok',

  login: (password) => {
    if (password !== ADMIN_PASSWORD) return false
    sessionStorage.setItem(STORAGE_KEY, 'ok')
    set({ logged: true })
    return true
  },

  logout: () => {
    sessionStorage.removeItem(STORAGE_KEY)
    set({ logged: ADMIN_OPEN })
  },
}))
