import { create } from 'zustand'
import type { StatDefinition } from '@/domain/statCatalog'
import { subscribeStats } from '@/data/statsRepo'

/**
 * Catalogo delle statistiche in tempo reale — porta la lista `DEFINITIONS`
 * di StatsUtility. Sostituisce le vecchie costanti `STAT_KEYS`/`STAT_LABELS`.
 *
 * La lista è sempre ordinata per `order` (lo garantisce il repo), quindi i
 * consumatori possono iterarla senza sort. Finché il primo snapshot da
 * Firebase non è arrivato, `loading` resta `true` e `catalog` è vuoto.
 */

interface StatCatalogState {
  catalog: StatDefinition[]
  loading: boolean
}

export const useStatCatalogStore = create<StatCatalogState>(() => ({
  catalog: [],
  loading: true,
}))

/**
 * Aggancia lo store al nodo `teammaker/stats/` di Firebase. Da chiamare una
 * volta sola all'avvio dell'app; l'unsubscribe torna utile solo nei test.
 */
export function initStatCatalog(): () => void {
  return subscribeStats(
    (catalog) => useStatCatalogStore.setState({ catalog, loading: false }),
    (error) => {
      console.error('Lettura catalogo stats fallita:', error)
      // Non svuotiamo `catalog`: se è già arrivato una volta, tenerlo evita
      // di far collassare la UI a una lista vuota per un errore temporaneo.
      useStatCatalogStore.setState({ loading: false })
    },
  )
}
