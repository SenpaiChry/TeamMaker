import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { HomeScreen } from '@/features/home/HomeScreen'
import { GenerateScreen } from '@/features/generate/GenerateScreen'
import { TeamsScreen } from '@/features/teams/TeamsScreen'
import { PlayersList } from '@/features/players/PlayersList'
import { TournamentScreen } from '@/features/tournament/TournamentScreen'
import { ScorecardScreen } from '@/features/scorecard/ScorecardScreen'
import { LiveScreen } from '@/features/live/LiveScreen'
import { IS_PRODUCTION_DATA, DB_ROOT } from '@/data/firebase'

export default function App() {
  return (
    <BrowserRouter>
      {IS_PRODUCTION_DATA && (
        <div className="bg-action-warning px-4 py-1 text-center text-sm font-bold text-black">
          DATI DI PRODUZIONE ({DB_ROOT}) — condivisi con l’app Android
        </div>
      )}
      <Routes>
        <Route path="/" element={<HomeScreen />} />
        <Route path="/genera" element={<GenerateScreen />} />
        <Route path="/squadre" element={<TeamsScreen />} />
        <Route path="/giocatori" element={<PlayersList />} />
        <Route path="/torneo" element={<TournamentScreen />} />
        <Route path="/segnapunti" element={<ScorecardScreen />} />
        <Route path="/diretta" element={<LiveScreen />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
