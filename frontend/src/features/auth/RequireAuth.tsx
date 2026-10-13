import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'

/** Guarda de rutas que requieren sesión, sin importar el rol (a diferencia de `AdminRoute`). */
export function RequireAuth() {
  const { isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}
