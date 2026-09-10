import { useStatCatalogStore } from '@/store/statCatalogStore'
import type { StatDefinition } from '@/domain/statCatalog'

/**
 * Catalogo delle statistiche in lettura, aggiornato in tempo reale.
 *
 * `loading` è vero solo prima del primo snapshot da Firebase: subito dopo
 * resta false anche se il catalogo è vuoto (caso raro ma legittimo: nodo
 * `teammaker/stats/` cancellato dalla console).
 */
export function useStatCatalog(): { catalog: StatDefinition[]; loading: boolean } {
  const catalog = useStatCatalogStore((s) => s.catalog)
  const loading = useStatCatalogStore((s) => s.loading)
  return { catalog, loading }
}
