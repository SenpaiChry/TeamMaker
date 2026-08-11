/**
 * Campo di ricerca, portato da bg_search_dark: pillola stondata con bordo
 * chiaro appena accennato e la crocetta per svuotarlo, come nell'originale.
 */
export function SearchField({
  value,
  onChange,
  placeholder = 'Cerca giocatore…',
}: {
  value: string
  onChange: (value: string) => void
  placeholder?: string
}) {
  return (
    <div className="relative">
      <input
        type="search"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-[20px] border border-search-box-border bg-search-box
                   py-2.5 pl-5 pr-10 text-list-text
                   placeholder:text-list-text-secondary placeholder:opacity-100
                   focus:border-brand-blue focus:outline-none"
      />
      {value.length > 0 && (
        <button
          type="button"
          onClick={() => onChange('')}
          aria-label="Cancella ricerca"
          className="absolute right-3 top-1/2 -translate-y-1/2 rounded px-1
                     text-list-text-muted hover:text-list-text"
        >
          ✕
        </button>
      )}
    </div>
  )
}
