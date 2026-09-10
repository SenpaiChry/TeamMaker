import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles/index.css'

const container = document.getElementById('root')
if (container === null) throw new Error('Elemento #root non trovato in index.html')

const root = createRoot(container)

/**
 * L'app viene importata in modo dinamico perché la configurazione di Firebase
 * viene validata a tempo di import: se manca, senza questo `catch` l'errore
 * uscirebbe solo in console e la pagina resterebbe bianca.
 */
try {
  const { default: App } = await import('./App')
  // Aggancia lo store al Firebase Auth: se una sessione era persistita, la
  // recuperiamo prima del primo render, così i tasti admin appaiono già
  // sbloccati invece di comparire dopo un flicker.
  const { initAdminAuth } = await import('./store/authStore')
  initAdminAuth()
  // Catalogo statistiche in tempo reale: senza queste definizioni voto,
  // pillole e schede giocatore sarebbero vuoti al primo render.
  const { initStatCatalog } = await import('./store/statCatalogStore')
  initStatCatalog()
  root.render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
} catch (error) {
  const { ConfigErrorScreen } = await import('./features/setup/ConfigErrorScreen')
  root.render(<ConfigErrorScreen error={error} />)
}
