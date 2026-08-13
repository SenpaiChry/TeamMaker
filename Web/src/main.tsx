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
  // Attacca la password admin alla store, in tempo reale sul nodo `admin-pw`.
  // Va fatto qui e non dentro un componente per essere pronta anche prima che
  // qualcuno provi a fare login (la lettura è comunque asincrona).
  const { initAdminPassword } = await import('./store/authStore')
  initAdminPassword()
  root.render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
} catch (error) {
  const { ConfigErrorScreen } = await import('./features/setup/ConfigErrorScreen')
  root.render(<ConfigErrorScreen error={error} />)
}
