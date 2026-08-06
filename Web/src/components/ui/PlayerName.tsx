import type { Player } from '@/domain/models'

/**
 * Nome del giocatore colorato per genere, come in tutte le liste dell'app
 * Android (varianti chiare, leggibili su fondo scuro).
 */
export function PlayerName({ player, showNickname = true }: { player: Player; showNickname?: boolean }) {
  const color = player.gender === 'F' ? 'text-women-dark' : 'text-men-dark'

  return (
    <span className="min-w-0">
      <span className={`font-semibold ${color}`}>{player.name}</span>
      {player.surname.length > 0 && (
        <span className={`ml-1 ${color} opacity-80`}>{player.surname}</span>
      )}
      {showNickname && player.nickname.length > 0 && (
        <span className="ml-2 text-sm text-list-text-muted">«{player.nickname}»</span>
      )}
    </span>
  )
}
