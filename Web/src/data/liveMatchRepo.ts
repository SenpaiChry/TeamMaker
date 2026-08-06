import { onDisconnect, onValue, set, update } from 'firebase/database'
import type { LiveMatch } from '@/domain/models'
import { dbRef } from './firebase'
import { parseLiveMatch, serializeLiveMatch } from './mappers'

/**
 * Nodo `live_match`: il segnapunti scrive, gli altri dispositivi leggono.
 * Porta LiveMatchUtility.
 *
 * C'è un solo nodo live per database: una partita trasmessa alla volta, come
 * nell'app Android.
 */

/** Scrive lo stato corrente della partita. Marca il nodo come attivo. */
export async function writeLiveMatch(live: Omit<LiveMatch, 'timestamp'>): Promise<void> {
  await update(dbRef('live_match'), serializeLiveMatch(live))
}

/**
 * Segna la partita come non più in diretta.
 * Come nel Java non cancella il nodo: azzera solo `active`, così l'ultimo
 * punteggio resta leggibile.
 */
export async function clearLiveMatch(): Promise<void> {
  await set(dbRef('live_match/active'), false)
}

/**
 * Chiede al server di spegnere la diretta appena questo client si disconnette.
 *
 * Serve perché la pulizia lato browser non è garantita: se l'utente chiude la
 * scheda, ricarica o perde la rete, nessun codice nostro viene eseguito e la
 * partita resterebbe "in diretta" per sempre. `onDisconnect` registra invece
 * l'operazione sul server Firebase, che la esegue quando la connessione cade.
 */
export function reserveLiveShutdown(): void {
  void onDisconnect(dbRef('live_match/active')).set(false)
}

/**
 * Annulla la prenotazione, quando la diretta viene chiusa normalmente.
 *
 * ⚠️ Volutamente senza `await`: `cancel()` azzera TUTTE le prenotazioni su quel
 * percorso, quindi un annullamento che arrivasse in ritardo cancellerebbe una
 * prenotazione registrata nel frattempo. Lasciando le operazioni in coda sulla
 * stessa connessione, l'ordine è garantito e l'ultima parola resta a chi ha
 * scritto per ultimo.
 */
export function cancelLiveShutdown(): void {
  void onDisconnect(dbRef('live_match/active')).cancel()
}

/** Ascolta la partita in diretta. Restituisce la funzione per smettere. */
export function subscribeToLiveMatch(
  onChange: (live: LiveMatch | null) => void,
  onError?: (error: Error) => void,
): () => void {
  return onValue(
    dbRef('live_match'),
    (snapshot) => onChange(parseLiveMatch(snapshot.val())),
    (error) => onError?.(error),
  )
}
