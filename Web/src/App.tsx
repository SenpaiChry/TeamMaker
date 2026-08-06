import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { HomeScreen } from '@/features/home/HomeScreen'
import { GenerateScreen } from '@/features/generate/GenerateScreen'
import { TeamsScreen } from '@/features/teams/TeamsScreen'
import { PlayersList } from '@/features/players/PlayersList'
import { TournamentScreen } from '@/features/tournament/TournamentScreen'
import { ScorecardScreen } from '@/features/scorecard/ScorecardScreen'
import { LiveScreen } from '@/features/live/LiveScreen'
import { AdminGate } from '@/features/admin/AdminGate'
import { LoginScreen } from '@/features/admin/LoginScreen'
import { AdminScreen } from '@/features/admin/AdminScreen'
import { PlayersAdminScreen } from '@/features/admin/PlayersAdminScreen'
import { TournamentsAdminScreen } from '@/features/admin/TournamentsAdminScreen'
import { TournamentDetailScreen } from '@/features/admin/TournamentDetailScreen'
import { IS_PRODUCTION_DATA } from '@/data/firebase'

export default function App() {
  return (
    <BrowserRouter>
      {/*
        Banda STAGING come in activity_main.xml: sfondo arancione, testo blu.
        L'app Android la mostra quando dbRoot punta allo staging, e non mostra
        nulla in produzione. L'avviso sui dati reali resta dentro l'area di
        gestione, che è l'unico punto in cui si scrive davvero.
      */}
      {!IS_PRODUCTION_DATA && (
        <div className="app-title bg-brand-orange py-1 text-center text-xl text-brand-blue">
          Staging
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
        <Route path="/login" element={<LoginScreen />} />
        <Route
          path="/admin"
          element={
            <AdminGate>
              <AdminScreen />
            </AdminGate>
          }
        />
        <Route
          path="/admin/giocatori"
          element={
            <AdminGate>
              <PlayersAdminScreen />
            </AdminGate>
          }
        />
        <Route
          path="/admin/tornei"
          element={
            <AdminGate>
              <TournamentsAdminScreen />
            </AdminGate>
          }
        />
        <Route
          path="/admin/tornei/:key"
          element={
            <AdminGate>
              <TournamentDetailScreen />
            </AdminGate>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
