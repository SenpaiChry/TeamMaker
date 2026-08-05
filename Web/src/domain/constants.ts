/**
 * Costanti di dominio portate da AppAndroid/.../Model/Constants.java.
 *
 * L'ordine di STAT_KEYS è significativo: corrisponde a `statsDescriptionEng`
 * e agli array paralleli `statsMax` / `statsStep` del Java. Qui gli array
 * paralleli diventano mappe, così un errore di indice non è più possibile.
 */

export const STAT_KEYS = [
  'height',
  'agility',
  'fallacy',
  'pass',
  'reception',
  'attack',
  'wall',
  'serve',
  'game-vision',
  'team-play',
  'mentality',
  'bonus',
] as const

export type StatKey = (typeof STAT_KEYS)[number]

/** Etichette italiane, da `statsDescription`. */
export const STAT_LABELS: Record<StatKey, string> = {
  height: 'Altezza',
  agility: 'Agilità',
  fallacy: 'Fallosità',
  pass: 'Palleggio',
  reception: 'Ricezione',
  attack: 'Attacco',
  wall: 'Muro',
  serve: 'Battuta',
  'game-vision': 'Visione di gioco',
  'team-play': 'Gioco di squadra',
  mentality: 'Mentalità',
  bonus: 'Bonus',
}

/** Valore massimo per statistica, da `statsMax`. */
export const STAT_MAX: Record<StatKey, number> = {
  height: 6,
  agility: 4,
  fallacy: 2,
  pass: 4,
  reception: 4,
  attack: 6,
  wall: 4,
  serve: 6,
  'game-vision': 4,
  'team-play': 2,
  mentality: 2,
  bonus: 2,
}

/** Incremento per statistica, da `statsStep`. */
export const STAT_STEP: Record<StatKey, number> = {
  height: 1,
  agility: 1,
  fallacy: 1,
  pass: 1,
  reception: 1,
  attack: 1.5,
  wall: 1,
  serve: 1.5,
  'game-vision': 2,
  'team-play': 1,
  mentality: 1,
  bonus: 1,
}

/**
 * Fasce d'altezza, da `valueHeight`. L'indice corrisponde al valore della
 * statistica `height` diviso per il suo step.
 */
export const HEIGHT_VALUES = [
  '<150cm',
  '151-160cm',
  '161-170cm',
  '171-180cm',
  '181-190cm',
  '191-200cm',
  '>200cm',
] as const

/** Numero di giocatori per squadra selezionabili nella schermata di generazione. */
export const PLAYERS_PER_TEAM_OPTIONS = [2, 3, 4, 5] as const

/** Chiave usata dal generatore per marcare il turno di riposo nel round robin. */
export const BYE_KEY = '__BYE__'
