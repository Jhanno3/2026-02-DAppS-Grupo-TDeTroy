import { Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { Button } from '../shared/components/Button'
import { LinkButton } from '../shared/components/LinkButton'
import styles from './RootLayout.module.css'

export function RootLayout() {
  const { isAuthenticated, user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <>
      <header className={styles.header}>
        <h1 className={styles.brand}>
          <LinkButton to="/" variant="ghost">
            Valuación de Jugadores
          </LinkButton>
        </h1>
        <nav className={styles.nav}>
          <LinkButton to="/jugadores" variant="ghost" small>
            Catálogo
          </LinkButton>
          {isAuthenticated && user ? (
            <>
              {user.rol === 'ADMIN' && (
                <LinkButton to="/admin/jugadores" variant="ghost" small>
                  Admin
                </LinkButton>
              )}
              <span>
                {user.email} ({user.rol})
              </span>
              <Button variant="secondary" small onClick={handleLogout}>
                Salir
              </Button>
            </>
          ) : (
            <>
              <LinkButton to="/login" variant="ghost" small>
                Iniciar sesión
              </LinkButton>
              <LinkButton to="/registro" variant="primary" small>
                Crear cuenta
              </LinkButton>
            </>
          )}
        </nav>
      </header>
      <Outlet />
    </>
  )
}
