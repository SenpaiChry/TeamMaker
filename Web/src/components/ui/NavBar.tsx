import { NavLink } from 'react-router-dom'

const LINKS = [
  { to: '/', label: 'Home', end: true },
  { to: '/giocatori', label: 'Giocatori' },
  { to: '/genera', label: 'Genera squadre' },
] as const

/** Navigazione principale, sostituisce il menu a cassetto dell'app Android. */
export function NavBar() {
  return (
    <nav className="border-b border-list-card-border bg-score-bg-top/60 backdrop-blur">
      <div className="mx-auto flex max-w-2xl items-center gap-1 overflow-x-auto p-2">
        {LINKS.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            end={'end' in link ? link.end : false}
            className={({ isActive }) =>
              `whitespace-nowrap rounded-lg px-3 py-2 text-sm font-semibold transition-colors ${
                isActive
                  ? 'bg-list-card-highlight text-list-highlight-text'
                  : 'text-list-text-secondary hover:text-list-text'
              }`
            }
          >
            {link.label}
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
