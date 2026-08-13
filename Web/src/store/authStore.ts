import { create } from 'zustand'
import { subscribeAdminPassword } from '@/data/adminRepo'

/**
 * Stato dell'accesso all'area di gestione.
 *
 * Sostituisce `Constants.logged`. Come nell'app Android l'accesso vale per
 * l'intera sessione; qui sopravvive anche a un cambio di pagina, ma non alla
 * chiusura della scheda.
 *
 * La password vive su Firebase al nodo `admin-pw`: cambiarla dalla console
 * ha effetto immediato senza ripubblicare il sito. Finché non è stata caricata
 * (o se manca) il login viene rifiutato.
 *
 * ⚠️ Non è una misura di sicurezza: il valore transita in chiaro nel traffico
 * Firebase, chiunque lo può leggere con gli strumenti di rete. La protezione
 * vera del database sono le sue regole (vedi database.rules.json).
 */

const STORAGE_KEY = 'teammaker.admin'

interface AuthState {
  logged: boolean
  /**
   * Password letta dal DB. `null` = non ancora caricata; stringa vuota = nodo
   * `admin-pw` presente ma vuoto (per prudenza non concediamo l'accesso).
   */
  adminPassword: string | null
  /** `true` se la password era giusta. */
  login: (password: string) => boolean
  logout: () => void
}

export const useAuthStore = create<AuthState>((set, get) => ({
  // Se in questa scheda si era già entrati, si resta dentro — la password
  // era stata validata al momento del login e vale per la sessione.
  logged: sessionStorage.getItem(STORAGE_KEY) === 'ok',
  adminPassword: null,

  login: (password) => {
    const current = get().adminPassword
    // In caricamento o non impostata: rifiuta, così non si entra con una
    // finestra momentanea di "password vuota" mentre Firebase risponde.
    if (current === null || current.length === 0) return false
    if (password !== current) return false
    sessionStorage.setItem(STORAGE_KEY, 'ok')
    set({ logged: true })
    return true
  },

  logout: () => {
    sessionStorage.removeItem(STORAGE_KEY)
    set({ logged: false })
  },
}))

/**
 * Aggancia lo store al nodo `admin-pw` in tempo reale. Da chiamare una volta
 * sola all'avvio dell'app.
 */
export function initAdminPassword(): () => void {
  return subscribeAdminPassword(
    (password) => useAuthStore.setState({ adminPassword: password }),
    (error) => console.error('Lettura admin-pw fallita:', error),
  )
}
