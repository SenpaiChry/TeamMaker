import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { StatStars } from '@/components/ui/StatStars'
import { STAT_KEYS, STAT_LABELS } from '@/domain/constants'
import type { Player } from '@/domain/models'
import { getVote } from '@/domain/player'

/**
 * Scheda con le 12 statistiche del giocatore.
 * Porta ActivityInfoPlayer + PlayerInfoAdapter.
 */
export function PlayerStatsModal({ player, onClose }: { player: Player | null; onClose: () => void }) {
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
              {String(getVote(player)).replace('.', ',')}
            </div>
          </div>
        )
      }
    >
      {player !== null && (
        <>
          <ul className="flex flex-col divide-y divide-list-card-border">
            {STAT_KEYS.map((stat) => (
              <li key={stat} className="flex items-center justify-between gap-4 py-2">
                <span className="text-sm text-list-text-secondary">{STAT_LABELS[stat]}</span>
                <StatStars stat={stat} value={player.stats[stat]} />
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
