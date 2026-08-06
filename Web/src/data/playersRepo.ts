import { onValue, push, remove, set, update } from 'firebase/database'
import type { Player } from '@/domain/models'
import { dbRef } from './firebase'
import { parsePlayers, serializePlayer } from './mappers'

/**
 * Accesso al nodo `players/`.
 * Porta PlayerUtility. Come nell'app Android la lettura è in tempo reale, così
 * una modifica fatta da un telefono si vede subito nel browser e viceversa.
 */

/** Ascolta l'elenco dei giocatori. Restituisce la funzione per smettere. */
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

/** Crea un giocatore e restituisce la chiave assegnata da Firebase. */
export async function addPlayer(player: Omit<Player, 'key'>): Promise<string> {
  const ref = push(dbRef('players'))
  if (ref.key === null) throw new Error('Firebase non ha restituito una chiave per il giocatore.')

  await set(ref, serializePlayer({ ...player, key: ref.key }))
  return ref.key
}

/**
 * Aggiorna un giocatore esistente.
 *
 * Differenza voluta rispetto al Java: `addEditPlayer` riscriveva anche i
 * riferimenti dentro ogni torneo, ma quei nodi contengono già la chiave del
 * giocatore — che non cambia mai — quindi era lavoro inutile e rischioso.
 * I tornei leggono il giocatore aggiornato al prossimo caricamento.
 */
export async function updatePlayer(player: Player): Promise<void> {
  await update(dbRef(`players/${player.key}`), serializePlayer(player))
}

/** Archivia o riattiva un giocatore. Porta archivePlayer / unarchivePlayer. */
export async function setPlayerActive(playerKey: string, isActive: boolean): Promise<void> {
  await set(dbRef(`players/${playerKey}/is_active`), isActive)
}

/**
 * Elimina un giocatore dall'anagrafica.
 *
 * ⚠️ I tornei passati continuano a referenziarlo per chiave: la squadra
 * risulterà con un giocatore in meno. È lo stesso comportamento dell'app
 * Android, motivo per cui archiviare è quasi sempre preferibile.
 */
export async function deletePlayer(playerKey: string): Promise<void> {
  await remove(dbRef(`players/${playerKey}`))
}
