import { onValue } from 'firebase/database'
import type { Player, Tournament } from '@/domain/models'
import { dbRef } from './firebase'
import { parseTournaments } from './mappers'

/**
 * Accesso al nodo `tournaments/`.
 *
 * I tornei referenziano i giocatori solo per chiave, quindi la lettura ha
 * bisogno dell'anagrafica già risolta: è lo stesso motivo per cui l'app Android
 * avviava il listener dei tornei solo dopo quello dei giocatori.
 */
export function subscribeToTournaments(
  players: Map<string, Player>,
  onChange: (tournaments: Tournament[]) => void,
  onError?: (error: Error) => void,
): () => void {
  return onValue(
    dbRef('tournaments'),
    (snapshot) => {
      onChange(parseTournaments(snapshot.val(), players))
    },
    (error) => onError?.(error),
  )
}
