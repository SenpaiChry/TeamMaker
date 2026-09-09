import { initializeApp } from 'firebase/app'
import { getAuth, type Auth } from 'firebase/auth'
import { getDatabase, ref, type DatabaseReference } from 'firebase/database'

/**
 * Inizializzazione di Firebase e risoluzione dei percorsi del database.
 *
 * La radice del database (`teammaker/` in produzione, `teammakerStaging/` in
 * sviluppo) arriva dall'ambiente: nessun percorso è scritto a mano altrove.
 *
 * Per LEGGERE e SCRIVERE sul Realtime Database bastano `databaseURL` e
 * `projectId`; per l'AUTH (login admin via Firebase Authentication) servono
 * anche `apiKey`, `authDomain` e `appId`. Le regole del database su
 * `teammaker/` accettano scritture solo se `auth.uid` è registrato nel nodo
 * `teammaker/admins`, quindi in produzione l'auth è obbligatoria per fare
 * qualunque modifica.
 */

/** Configurazione assente o incompleta: va mostrata all'utente, non ingoiata. */
export class ConfigError extends Error {
  constructor(readonly missing: string[]) {
    super(`Variabili d'ambiente mancanti: ${missing.join(', ')}`)
    this.name = 'ConfigError'
  }
}

function env(name: string): string {
  const value = import.meta.env[name] as string | undefined
  return value === undefined ? '' : value.trim()
}

const REQUIRED = ['VITE_FIREBASE_DATABASE_URL', 'VITE_FIREBASE_PROJECT_ID', 'VITE_DB_ROOT'] as const

const missing = REQUIRED.filter((name) => env(name).length === 0)
if (missing.length > 0) throw new ConfigError([...missing])

const firebaseConfig = {
  databaseURL: env('VITE_FIREBASE_DATABASE_URL'),
  projectId: env('VITE_FIREBASE_PROJECT_ID'),
  // Facoltativi finché non serve l'autenticazione.
  apiKey: env('VITE_FIREBASE_API_KEY'),
  authDomain: env('VITE_FIREBASE_AUTH_DOMAIN'),
  storageBucket: env('VITE_FIREBASE_STORAGE_BUCKET'),
  messagingSenderId: env('VITE_FIREBASE_MESSAGING_SENDER_ID'),
  appId: env('VITE_FIREBASE_APP_ID'),
}

export const app = initializeApp(firebaseConfig)
export const database = getDatabase(app)

/**
 * Auth di Firebase. Persiste da sé (localStorage): la sessione admin sopravvive
 * alla chiusura del tab, come sull'app Android. Se `apiKey`/`appId` mancano
 * dalla configurazione le chiamate di auth falliscono al primo tentativo, ma
 * la parte pubblica dell'app funziona lo stesso.
 */
export const auth: Auth = getAuth(app)

/** `true` se l'autenticazione è configurata; se no il login admin non funziona. */
export const HAS_AUTH_CONFIG = firebaseConfig.apiKey.length > 0 && firebaseConfig.appId.length > 0

/** Radice del database, con la barra finale garantita. */
export const DB_ROOT = env('VITE_DB_ROOT').replace(/\/*$/, '/')

/** `true` se stiamo lavorando sui dati di produzione condivisi con l'app Android. */
export const IS_PRODUCTION_DATA = DB_ROOT === 'teammaker/'

/** Riferimento a un percorso relativo alla radice configurata. */
export function dbRef(path: string): DatabaseReference {
  return ref(database, `${DB_ROOT}${path.replace(/^\/+/, '')}`)
}
