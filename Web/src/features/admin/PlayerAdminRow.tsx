import type { Player } from '@/domain/models'
import { getSurnameOrNickname, getVote } from '@/domain/player'

/**
 * Riga dell'anagrafica, portata da layout_player_admin.xml + PlayerAdminAdapter.
 *
 * Un giocatore archiviato resta nella stessa lista ma spento: sfondo più
 * scuro, testo grigio e avatar sbiadito al 40%. Il tasto archivia si scambia
 * con quello riattiva, come nell'originale.
 */
export function PlayerAdminRow({
  player,
  onEdit,
  onToggleActive,
  onDelete,
  onInfo,
}: {
  player: Player
  onEdit: () => void
  onToggleActive: () => void
  onDelete: () => void
  onInfo: () => void
}) {
  const active = player.isActive
  const isFemale = player.gender === 'F'

  const textColor = !active
    ? 'text-list-text-muted'
    : isFemale
      ? 'text-women-dark'
      : 'text-men-dark'

  return (
    <div
      className={`flex items-center px-2.5 py-1.5 ${
        active
          ? 'rounded-2xl border border-list-card-border bg-list-card'
          : 'rounded-2xl border border-player-archived-border bg-player-archived'
      }`}
    >
      <span
        className={`grid size-[45px] shrink-0 place-items-center rounded-[18px] ${
          isFemale ? 'bg-women-icon-bg' : 'bg-men-icon-bg'
        } ${active ? '' : 'opacity-40'}`}
        aria-hidden
      >
        <span className="text-2xl leading-none text-white">{isFemale ? '♀' : '♂'}</span>
      </span>

      <span className={`ml-2 min-w-0 grow px-[3px] ${textColor}`}>
        <span className="app-title block truncate text-base leading-tight">{player.name}</span>
        <span className="app-title block truncate text-base leading-tight">
          {getSurnameOrNickname(player)}
        </span>
      </span>

      <span
        className={`mx-1 min-w-10 shrink-0 rounded-xl bg-points-pill px-[7px] py-[5px]
                    text-center text-base ${textColor} app-title`}
      >
        {getVote(player)}
      </span>

      <IconAction
        label={active ? `Archivia ${player.name}` : `Riattiva ${player.name}`}
        onClick={onToggleActive}
      >
        {active ? '🚫' : '✔'}
      </IconAction>
      <IconAction label={`Modifica ${player.name}`} onClick={onEdit}>
        ✎
      </IconAction>
      <IconAction label={`Elimina ${player.name}`} onClick={onDelete}>
        🗑
      </IconAction>
      <IconAction label={`Statistiche di ${player.name}`} onClick={onInfo}>
        ⓘ
      </IconAction>
    </div>
  )
}

/** Tasto icona da 30dp con lo sfondo di bg_icon_action. */
function IconAction({
  label,
  onClick,
  children,
}: {
  label: string
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      title={label}
      className="mx-px grid size-[30px] shrink-0 place-items-center rounded-lg
                 bg-icon-action text-sm leading-none hover:brightness-150"
    >
      {children}
    </button>
  )
}
