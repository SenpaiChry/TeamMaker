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
        className="w-full rounded-lg border border-list-card-border bg-list-card py-2 pl-4 pr-10
                   text-list-text placeholder:text-list-text-muted
                   focus:border-list-highlight-text focus:outline-none"
      />
      {value.length > 0 && (
        <button
          type="button"
          onClick={() => onChange('')}
          aria-label="Cancella ricerca"
          className="absolute right-2 top-1/2 -translate-y-1/2 rounded px-2 py-1
                     text-list-text-muted hover:text-list-text"
        >
          ✕
        </button>
      )}
    </div>
  )
}
