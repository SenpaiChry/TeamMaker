/**
 * Costanti di dominio portate da AppAndroid/.../Model/Constants.java.
 *
 * Le vecchie costanti di stat (STAT_KEYS, STAT_LABELS, STAT_MAX, STAT_STEP,
 * HEIGHT_VALUES) sono state rimosse: le stat non sono più cablate nel codice,
 * ma arrivano dal catalogo dinamico. Vedi `domain/statCatalog.ts`.
 */

/** Numero di giocatori per squadra selezionabili nella schermata di generazione. */
export const PLAYERS_PER_TEAM_OPTIONS = [2, 3, 4, 5] as const

/** Chiave usata dal generatore per marcare il turno di riposo nel round robin. */
export const BYE_KEY = '__BYE__'
