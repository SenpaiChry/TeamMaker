import { onValue } from 'firebase/database'
import { dbRef } from './firebase'

/**
 * Password dell'area di gestione, letta in tempo reale dal nodo `admin-pw`.
 *
 * Vive fuori da `.env` per due motivi:
 *  - la si cambia dalla console Firebase senza ripubblicare il sito;
 *  - resta un'unica sorgente condivisa fra web e app Android quando anche
 *    quest'ultima la migrerà via da `Constants.java`.
 *
 * ⚠️ Non è una misura di sicurezza. Chiunque apra il traffico Firebase vede
 * il valore in chiaro. È una comodità per non entrare per sbaglio nell'area
 * di gestione; la protezione vera restano le regole del Realtime Database.
 */
export function subscribeAdminPassword(
  onChange: (password: string | null) => void,
  onError?: (error: Error) => void,
): () => void {
  return onValue(
    dbRef('admin-pw'),
    (snapshot) => {
      const value = snapshot.val()
      onChange(typeof value === 'string' ? value : null)
    },
    (error) => onError?.(error),
  )
}
