import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

/**
 * Manda alla schermata di accesso chi non è ancora entrato, come fa
 * MainActivity aprendo ActivityLogin invece di TournamentActivityManage.
 *
 * ⚠️ Non è una misura di sicurezza: vedi il commento in `authStore`.
 */
export function AdminGate({ children }: { children: ReactNode }) {
  const logged = useAuthStore((s) => s.logged)

  if (!logged) return <Navigate to="/login" replace />
  return <>{children}</>
}
