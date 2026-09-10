import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { StatStars } from '@/components/ui/StatStars'
import type { Player } from '@/domain/models'
import { getVote } from '@/domain/player'
import { useStatCatalog } from '@/hooks/useStatCatalog'

/**
 * Scheda con le statistiche del giocatore.
 * Porta ActivityInfoPlayer + PlayerInfoAdapter.
 *
 * Il catalogo è dinamico: le righe si costruiscono da `useStatCatalog()` (nodo
 * `teammaker/stats/`), non da una lista hardcoded. Per ogni stat, se il
 * catalogo prevede il bonus e il giocatore ce l'ha, compare una stella ciano
 * nella terza colonna (come su Android dopo il refactor a 3 colonne).
 */
export function PlayerStatsModal({ player, onClose }: { player: Player | null; onClose: () => void }) {
  const { catalog } = useStatCatalog()
  const color = player?.gender === 'F' ? 'text-women-dark' : 'text-men-dark'

  return (
    <Modal
      open={player !== null}
      onClose={onClose}
      title={
        player === null ? null : (
          <div className="flex items-baseline justify-between gap-4">
            <div className={`min-w-0 ${color}`}>
              <div className="truncate text-xl font-bold">
                {player.name} {player.surname}
              </div>
              {player.nickname.length > 0 && (
                <div className="truncate text-sm opacity-80">«{player.nickname}»</div>
              )}
            </div>
            <div className={`text-3xl font-black tabular-nums ${color}`}>
              {/* L'app Android mostra il voto con la virgola decimale. */}
              {String(getVote(player, catalog)).replace('.', ',')}
            </div>
          </div>
        )
      }
    >
      {player !== null && (
        <>
          <ul className="flex flex-col divide-y divide-list-card-border">
            {catalog.map((stat) => (
              <li key={stat.key} className="flex items-center justify-between gap-4 py-2">
                <span className="text-sm text-list-text-secondary">{stat.label}</span>
                <span className="flex items-center gap-2">
                  <StatStars stat={stat} value={player.stats[stat.key] ?? 0} />
                  {stat.allowBonus && player.bonus[stat.key] === true && (
                    <span
                      className="text-lg leading-none text-bracket-header"
                      title={`Bonus ${stat.label}: +${stat.step}`}
                      aria-label={`Bonus attivo su ${stat.label}`}
                    >
                      ★
                    </span>
                  )}
                </span>
              </li>
            ))}
          </ul>
          <Button variant="ghost" onClick={onClose} className="mt-5 w-full">
            OK
          </Button>
        </>
      )}
    </Modal>
  )
}
