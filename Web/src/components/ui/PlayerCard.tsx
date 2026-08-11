import type { Player } from '@/domain/models'

/**
 * Riga giocatore della schermata di generazione, portata da
 * layout_player_generate.xml + PlayerGenerateAdapter: card stondata da 16dp,
 * quadrato colorato da 45dp con l'icona del sesso, nome e cognome su due righe
 * da 17sp in helvetica black corsivo colorati per sesso.
 *
 * Come nell'originale è l'INTERA riga a selezionare: l'indicatore a destra
 * (cerchio + / cerchio ✓ pieno) è solo uno stato, non un tasto a sé (nel layout
 * Android è infatti `clickable="false"`).
 */
export function PlayerCard({
  player,
  selected,
  onToggle,
}: {
  player: Player
  selected: boolean
  onToggle: () => void
}) {
  const isFemale = player.gender === 'F'
  const nameColor = isFemale ? 'text-women-dark' : 'text-men-dark'
  const iconBg = isFemale ? 'bg-women-icon-bg' : 'bg-men-icon-bg'

  return (
    <button
      type="button"
      onClick={onToggle}
      aria-pressed={selected}
      className={`flex w-full items-center px-2.5 py-1.5 text-left transition ${
        selected
          ? 'rounded-2xl border-2 border-player-selected-border bg-player-selected-fill'
          : 'rounded-2xl border border-list-card-border bg-list-card'
      }`}
    >
      <span
        className={`grid size-[45px] shrink-0 place-items-center rounded-[18px] ${iconBg}`}
        aria-hidden
      >
        <span className="text-2xl leading-none text-white">{isFemale ? '♀' : '♂'}</span>
      </span>

      <span className={`ml-2 min-w-0 grow px-[3px] ${nameColor}`}>
        <span className="app-title block truncate text-[17px] leading-tight">{player.name}</span>
        {player.surname.length > 0 && (
          <span className="app-title block truncate text-[17px] leading-tight">
            {player.surname}
          </span>
        )}
      </span>

      <span className="grid w-[50px] shrink-0 place-items-center" aria-hidden>
        {selected ? (
          <svg
            viewBox="0 0 24 24"
            className="size-8 text-player-selected-border drop-shadow-sm"
            fill="currentColor"
          >
            <circle cx="12" cy="12" r="11" />
            <path
              d="M7 12.5l3.2 3.2L17 9"
              fill="none"
              stroke="#ffffff"
              strokeWidth="2.6"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        ) : (
          <svg
            viewBox="0 0 24 24"
            className="size-8 text-brand-blue"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
          >
            <circle cx="12" cy="12" r="10" />
            <path d="M12 8v8M8 12h8" />
          </svg>
        )}
      </span>
    </button>
  )
}
