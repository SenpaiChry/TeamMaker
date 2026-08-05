import { initializeApp } from 'firebase/app'
import { getDatabase, ref, type DatabaseReference } from 'firebase/database'

/**
 * Inizializzazione di Firebase e risoluzione dei percorsi del database.
 *
 * La radice del database (`teammaker/` in produzione, `teammakerStaging/` in
 * sviluppo) arriva dall'ambiente: nessun percorso è scritto a mano altrove.
 *
 * Per LEGGERE e SCRIVERE sul Realtime Database bastano `databaseURL` e
 * `projectId`. `apiKey` e `appId` servono solo all'autenticazione, che arriva
 * nella fase 6: finché mancano l'app funziona lo stesso.
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

/** `true` se l'autenticazione è configurabile; in fase 6 diventerà obbligatoria. */
export const HAS_AUTH_CONFIG = firebaseConfig.apiKey.length > 0 && firebaseConfig.appId.length > 0

/** Radice del database, con la barra finale garantita. */
export const DB_ROOT = env('VITE_DB_ROOT').replace(/\/*$/, '/')

/** `true` se stiamo lavorando sui dati di produzione condivisi con l'app Android. */
export const IS_PRODUCTION_DATA = DB_ROOT === 'teammaker/'

/** Riferimento a un percorso relativo alla radice configurata. */
export function dbRef(path: string): DatabaseReference {
  return ref(database, `${DB_ROOT}${path.replace(/^\/+/, '')}`)
}
