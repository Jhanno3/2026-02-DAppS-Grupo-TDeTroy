import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

/**
 * Guarda de rutas para el panel de administrador (UC-03/UC-04). Sin cuenta -> /login; logueado
 * pero sin rol ADMIN -> /. El backend igual re-valida el rol en cada request (`@PreAuthorize`,
 * `JugadorController`) — esto es sólo UX, nunca la autorización real.
 */
export function AdminRoute() {
  const { isAuthenticated, user } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  if (user?.rol !== 'ADMIN') {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
