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
          className="-ml-2 grid size-11 shrink-0 place-items-center rounded-full text-list-text
                     transition hover:bg-list-card active:scale-95"
        >
          {/* Freccia SVG stroke: sostituisce «←» che veniva reso a blocchi
              variabili a seconda del font di sistema. */}
          <svg
            viewBox="0 0 24 24"
            className="size-7"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden
          >
            <path d="M15 6l-6 6 6 6" />
          </svg>
        </button>
      )}
      {/* `leading-none` toglie l'extra line-height (default 1.5) che spostava
          il titolo in alto rispetto ai tasti sui lati. */}
      <h1 className="app-title grow truncate text-2xl leading-none">{title}</h1>
      {right}
    </header>
  )
}
