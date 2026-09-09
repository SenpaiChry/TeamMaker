import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

/**
 * Manda alla schermata di accesso chi non è ancora entrato, come fa
 * MainActivity aprendo ActivityLogin invece di TournamentActivityManage.
 *
 * L'auth vera vive lato Firebase (regole del DB + Firebase Authentication):
 * anche se qualcuno bypassa questo gate, le scritture al DB falliscono con
 * `PERMISSION_DENIED`. Questo componente serve solo a evitare che i
 * non-admin arrivino su schermate che darebbero errore al primo tocco.
 */
export function AdminGate({ children }: { children: ReactNode }) {
  const logged = useAuthStore((s) => s.logged)

  if (!logged) return <Navigate to="/login" replace />
  return <>{children}</>
}
