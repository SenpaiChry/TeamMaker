import type { ReactNode } from 'react'

/** Intestazione con il tasto indietro, come la barra in cima alle Activity. */
export function ScreenHeader({
  title,
  onBack,
  right,
}: {
  title: string
  onBack?: () => void
  right?: ReactNode
}) {
  return (
    <header className="mb-4 flex items-center gap-3">
      {onBack !== undefined && (
        <button
          type="button"
          onClick={onBack}
          aria-label="Indietro"
          className="grid size-9 shrink-0 place-items-center rounded-lg border
                     border-list-card-border bg-list-card text-lg hover:bg-score-panel"
        >
          ←
        </button>
      )}
      <h1 className="grow truncate text-xl font-bold tracking-wide">{title}</h1>
      {right}
    </header>
  )
}
