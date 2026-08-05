import { onValue } from 'firebase/database'
import type { Player } from '@/domain/models'
import { dbRef } from './firebase'
import { parsePlayers } from './mappers'

/**
 * Accesso al nodo `players/`.
 *
 * Per ora solo lettura: le scritture arrivano nella fase 6, insieme all'admin.
 * Come nell'app Android l'ascolto è in tempo reale, così una modifica fatta da
 * un telefono si vede subito anche nel browser.
 */

/**
 * Ascolta l'elenco dei giocatori. Restituisce la funzione per smettere di
 * ascoltare, da chiamare quando il componente viene smontato.
 */
export function subscribeToPlayers(
  onChange: (players: Player[]) => void,
  onError?: (error: Error) => void,
): () => void {
  return onValue(
    dbRef('players'),
    (snapshot) => {
      onChange([...parsePlayers(snapshot.val()).values()])
    },
    (error) => onError?.(error),
  )
}
