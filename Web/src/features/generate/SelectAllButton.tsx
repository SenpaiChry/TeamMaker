/**
 * Pulsante icona per selezionare o deselezionare tutti i giocatori.
 *
 * Sostituisce il testo "SELEZIONA / DESELEZIONA TUTTI" della schermata di
 * generazione: due glifi diversi a seconda dello stato, stessa altezza del
 * campo di ricerca a fianco, `aria-label` per gli screen reader.
 */
export function SelectAllButton({
  allSelected,
  onClick,
}: {
  allSelected: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={allSelected ? 'Deseleziona tutti' : 'Seleziona tutti'}
      title={allSelected ? 'Deseleziona tutti' : 'Seleziona tutti'}
      className={`grid size-11 shrink-0 place-items-center rounded-full border transition
                  active:scale-95 ${
                    allSelected
                      ? 'border-player-selected-border bg-player-selected-fill text-player-selected-border hover:brightness-110'
                      : 'border-list-card-border bg-list-card text-brand-blue hover:bg-score-panel'
                  }`}
    >
      {allSelected ? (
        // Cerchio con X: azzera la selezione.
        <svg
          viewBox="0 0 24 24"
          className="size-6"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden
        >
          <circle cx="12" cy="12" r="9" />
          <path d="M9 9l6 6M15 9l-6 6" />
        </svg>
      ) : (
        // "List checks": lista con doppio spunta, riconoscibile come "seleziona tutti".
        <svg
          viewBox="0 0 24 24"
          className="size-6"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden
        >
          <path d="M3 7l2.2 2.2L9.5 5" />
          <path d="M3 15l2.2 2.2L9.5 13" />
          <path d="M13 7.5h8" />
          <path d="M13 15.5h8" />
        </svg>
      )}
    </button>
  )
}
