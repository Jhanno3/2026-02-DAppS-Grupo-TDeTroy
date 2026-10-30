import {
  Link,
  NavLink,
  Outlet,
  matchPath,
  useLocation,
  useNavigate,
} from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { useTema } from './tema'
import styles from './RootLayout.module.css'

// Pantallas ya migradas al tema claro/oscuro (modeloHTML/). El resto sigue con su paleta oscura
// propia, así que ahí el header también queda oscuro para no mezclar un header claro con una
// página oscura.
const RUTAS_CON_TEMA = ['/jugadores/:id']

function claseLink({ isActive }: { isActive: boolean }) {
  return isActive ? `${styles.link} ${styles.linkActivo}` : styles.link
}

export function RootLayout() {
  const { isAuthenticated, user, logout } = useAuth()
  const { tema } = useTema()
  const navigate = useNavigate()
  const { pathname } = useLocation()

  const usaTema = RUTAS_CON_TEMA.some((ruta) => matchPath(ruta, pathname))

  function handleLogout() {
    logout()
    void navigate('/login')
  }

  return (
    <div className={styles.layout} data-tema={usaTema ? tema : 'oscuro'}>
      <header className={styles.header}>
        <div className={styles.izquierda}>
          <Link to="/mercado" className={styles.marca}>
            Valuación de Jugadores
          </Link>
          <nav className={styles.nav} aria-label="Principal">
            <NavLink to="/mercado" className={claseLink}>
              Mercado
            </NavLink>
            <NavLink to="/p2p" className={claseLink}>
              P2P
            </NavLink>
            <NavLink to="/subastas" className={claseLink}>
              Subastas
            </NavLink>
            {isAuthenticated && user && (
              <NavLink to="/cartera" className={claseLink}>
                Mi cartera
              </NavLink>
            )}
            {user?.rol === 'ADMIN' && (
              <NavLink to="/admin/jugadores" className={claseLink}>
                Admin
              </NavLink>
            )}
          </nav>
        </div>

        <div className={styles.derecha}>
          {isAuthenticated && user ? (
            <>
              <span className={styles.usuario}>
                {user.email}
                {user.rol === 'ADMIN' && ' (ADMIN)'}
              </span>
              <button
                type="button"
                className={styles.botonSecundario}
                onClick={handleLogout}
              >
                Salir
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className={styles.botonSecundario}>
                Iniciar sesión
              </Link>
              <Link to="/registro" className={styles.botonPrimario}>
                Crear cuenta
              </Link>
            </>
          )}
        </div>
      </header>
      <Outlet />
    </div>
  )
}
