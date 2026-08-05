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
  root.render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
} catch (error) {
  const { ConfigErrorScreen } = await import('./features/setup/ConfigErrorScreen')
  root.render(<ConfigErrorScreen error={error} />)
}
