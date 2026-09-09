import { onAuthStateChanged, signInWithEmailAndPassword, signOut } from 'firebase/auth'
import { create } from 'zustand'
import { auth } from '@/data/firebase'

/**
 * Stato dell'accesso all'area di gestione — porta AdminUtility dopo il passaggio
 * a Firebase Authentication.
 *
 * Prima la password admin era un nodo `admin-pw` in chiaro nel DB e il confronto
 * avveniva client-side; ora l'admin è un utente Firebase Auth vero con email
 * fissa (ADMIN_EMAIL) e il confronto avviene server-side. Le regole del
 * database accettano scritture solo se `auth.uid` è registrato in
 * `teammaker/admins`, quindi l'auth è la vera protezione.
 *
 * UX invariata: l'utente digita SOLO la password. L'email è nascosta.
 *
 * La sessione persiste in `localStorage` grazie alla persistenza nativa di
 * Firebase Auth: chi era loggato prima di chiudere il tab rientra
 * automaticamente al riapertura, esattamente come su Android.
 */

/** Email fissa dell'account admin, hardcoded — deve combaciare con l'Android. */
const ADMIN_EMAIL = 'admin@teammaker.local'

interface AuthState {
  logged: boolean
  /** `true` mentre la richiesta di login è in corso. */
  busy: boolean
  login: (password: string) => Promise<boolean>
  logout: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set) => ({
  // Stato iniziale: `false`. Se una sessione persistita esiste, Firebase Auth
  // ce la rimanda tramite `onAuthStateChanged` (vedi `initAdminAuth`).
  logged: false,
  busy: false,

  login: async (password) => {
    if (password.length === 0) return false
    set({ busy: true })
    try {
      await signInWithEmailAndPassword(auth, ADMIN_EMAIL, password)
      // onAuthStateChanged aggiornerà `logged: true`; qui bastasetare `busy`
      // a false per riabilitare il tasto.
      return true
    } catch (error) {
      console.warn('Login admin fallito:', error)
      return false
    } finally {
      set({ busy: false })
    }
  },

  logout: async () => {
    try {
      await signOut(auth)
    } catch (error) {
      console.warn('Logout admin fallito:', error)
    }
  },
}))

/**
 * Aggancia lo store a Firebase Auth in tempo reale. Da chiamare una volta sola
 * all'avvio dell'app.
 *
 * All'apertura del tab Firebase Auth ci notifica lo stato della sessione
 * persistita; da lì in poi ogni login/logout aggiorna lo store.
 */
export function initAdminAuth(): () => void {
  return onAuthStateChanged(auth, (user) => {
    // La sessione è valida solo se corrisponde davvero all'account admin: se
    // qualcuno arriva loggato con un altro utente (improbabile — Auth non ha
    // altri account — ma non costa nulla essere prudenti) resta fuori.
    const isAdmin = user !== null && user.email?.toLowerCase() === ADMIN_EMAIL
    useAuthStore.setState({ logged: isAdmin })
  })
}
