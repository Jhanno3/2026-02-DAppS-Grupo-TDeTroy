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
        <nav className={styles.nav}>
          <LinkButton to="/mercado" variant="ghost" small>
            Mercado
          </LinkButton>
          <LinkButton to="/p2p" variant="ghost" small>
            P2P
          </LinkButton>
          {isAuthenticated && user ? (
            <>
              <LinkButton to="/cartera" variant="ghost" small>
                Mi cartera
              </LinkButton>
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
