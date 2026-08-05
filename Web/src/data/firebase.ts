import { initializeApp } from 'firebase/app'
import { getDatabase, ref, type DatabaseReference } from 'firebase/database'

/**
 * Inizializzazione di Firebase e risoluzione dei percorsi del database.
 *
 * La radice del database (`teammaker/` in produzione, `teammakerStaging/` in
 * sviluppo) arriva dall'ambiente: nessun percorso è scritto a mano altrove.
 */

function requireEnv(name: string): string {
  const value = import.meta.env[name] as string | undefined
  if (value === undefined || value.length === 0) {
    throw new Error(
      `Variabile d'ambiente ${name} mancante. Copia .env.example in .env.local e compilalo.`,
    )
  }
  return value
}

const firebaseConfig = {
  apiKey: requireEnv('VITE_FIREBASE_API_KEY'),
  authDomain: requireEnv('VITE_FIREBASE_AUTH_DOMAIN'),
  databaseURL: requireEnv('VITE_FIREBASE_DATABASE_URL'),
  projectId: requireEnv('VITE_FIREBASE_PROJECT_ID'),
  storageBucket: requireEnv('VITE_FIREBASE_STORAGE_BUCKET'),
  messagingSenderId: requireEnv('VITE_FIREBASE_MESSAGING_SENDER_ID'),
  appId: requireEnv('VITE_FIREBASE_APP_ID'),
}

export const app = initializeApp(firebaseConfig)
export const database = getDatabase(app)

/** Radice del database, con la barra finale garantita. */
export const DB_ROOT = requireEnv('VITE_DB_ROOT').replace(/\/*$/, '/')

/** `true` se stiamo lavorando sui dati di produzione condivisi con l'app Android. */
export const IS_PRODUCTION_DATA = DB_ROOT === 'teammaker/'

/** Riferimento a un percorso relativo alla radice configurata. */
export function dbRef(path: string): DatabaseReference {
  return ref(database, `${DB_ROOT}${path.replace(/^\/+/, '')}`)
}
