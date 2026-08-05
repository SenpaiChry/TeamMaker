/**
 * Schermata mostrata quando l'app non riesce nemmeno a partire, tipicamente
 * perché manca `.env.local`. Meglio un messaggio con le istruzioni che una
 * pagina bianca e un errore nascosto in console.
 */
export function ConfigErrorScreen({ error }: { error: unknown }) {
  const message = error instanceof Error ? error.message : String(error)
  const isMissingEnv = message.includes("Variabili d'ambiente mancanti")

  return (
    <div className="mx-auto max-w-2xl p-6">
      <h1 className="text-2xl font-bold text-action-warning">Configurazione incompleta</h1>
      <p className="mt-2 text-list-text-secondary">{message}</p>

      {isMissingEnv && (
        <div className="mt-6 rounded-lg border border-list-card-border bg-list-card p-4">
          <p className="font-semibold">Come sistemare</p>
          <ol className="mt-3 flex list-decimal flex-col gap-2 pl-5 text-list-text-secondary">
            <li>
              Copia <code className="text-list-highlight-text">.env.example</code> in{' '}
              <code className="text-list-highlight-text">.env.local</code> dentro la cartella{' '}
              <code className="text-list-highlight-text">Web/</code>.
            </li>
            <li>Riavvia il server di sviluppo: Vite legge i file .env solo all’avvio.</li>
          </ol>
          <p className="mt-4 text-sm text-list-text-muted">
            Per leggere e scrivere sul database bastano <code>VITE_FIREBASE_DATABASE_URL</code>,{' '}
            <code>VITE_FIREBASE_PROJECT_ID</code> e <code>VITE_DB_ROOT</code>, già compilati
            nell’esempio. <code>apiKey</code> e <code>appId</code> servono solo
            all’autenticazione, che arriva nella fase 6.
          </p>
        </div>
      )}
    </div>
  )
}
