import { PlayersList } from '@/features/players/PlayersList'
import { IS_PRODUCTION_DATA, DB_ROOT } from '@/data/firebase'

export default function App() {
  return (
    <>
      {IS_PRODUCTION_DATA && (
        <div className="bg-action-warning px-4 py-1 text-center text-sm font-bold text-black">
          DATI DI PRODUZIONE ({DB_ROOT}) — condivisi con l’app Android
        </div>
      )}
      <PlayersList />
    </>
  )
}
