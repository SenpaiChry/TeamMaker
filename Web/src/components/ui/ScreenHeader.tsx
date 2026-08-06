import type { ReactNode } from 'react'

/**
 * Intestazione con il tasto indietro, come la barra in cima alle Activity:
 * quadrato blu da 42dp con la freccia, titolo in helvetica black corsivo.
 */
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
          className="grid size-[42px] shrink-0 place-items-center rounded-[11px] bg-brand-blue
                     text-xl text-white transition hover:bg-brand-blue-pressed"
        >
          ←
        </button>
      )}
      <h1 className="app-title grow truncate text-2xl">{title}</h1>
      {right}
    </header>
  )
}
