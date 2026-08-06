import { create } from 'zustand'
import type { Player, Team } from '@/domain/models'

/**
 * Giocatori selezionati per la generazione e squadre generate.
 *
 * Sostituisce `Constants.playersSelected` e `Constants.teams`: stesso ruolo,
 * ma lo stato è osservabile e i componenti si aggiornano da soli invece di
 * dover chiamare `notifyDataSetChanged()` a mano.
 *
 * Si conservano le CHIAVI, non gli oggetti: i giocatori arrivano da Firebase in
 * tempo reale e vengono ricreati a ogni aggiornamento, quindi un riferimento
 * salvato diventerebbe subito obsoleto. È lo stesso motivo per cui nell'app
 * Android una modifica a un giocatore durante la selezione poteva perdersi.
 */

interface SelectionState {
  selectedKeys: Set<string>
  teams: Team[]
  /** Giocatori per squadra scelti nell'ultima generazione, per il reroll. */
  playersPerTeam: number

  toggle: (key: string) => void
  selectAll: (keys: string[]) => void
  clear: () => void
  isSelected: (key: string) => boolean

  setTeams: (teams: Team[], playersPerTeam: number) => void
  clearTeams: () => void
}

export const useSelectionStore = create<SelectionState>((set, get) => ({
  selectedKeys: new Set(),
  teams: [],
  playersPerTeam: 0,

  toggle: (key) =>
    set((state) => {
      const next = new Set(state.selectedKeys)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return { selectedKeys: next }
    }),

  selectAll: (keys) => set({ selectedKeys: new Set(keys) }),

  clear: () => set({ selectedKeys: new Set() }),

  isSelected: (key) => get().selectedKeys.has(key),

  setTeams: (teams, playersPerTeam) => set({ teams, playersPerTeam }),

  clearTeams: () => set({ teams: [], playersPerTeam: 0 }),
}))

/** Giocatori selezionati, risolti sull'elenco aggiornato arrivato da Firebase. */
export function resolveSelected(players: Player[], selectedKeys: Set<string>): Player[] {
  return players.filter((p) => selectedKeys.has(p.key))
}
