import { useAuth } from '../auth/AuthContext'
import styles from './HomePage.module.css'

/** Placeholder hasta T8.2 (catálogo de jugadores, UC-07) — hoy sólo refleja el estado de sesión. */
export function HomePage() {
  const { isAuthenticated, user } = useAuth()

  return (
    <main className={styles.main}>
      {isAuthenticated && user ? (
        <p>Sesión iniciada como {user.email}.</p>
      ) : (
        <p>Iniciá sesión o creá una cuenta para operar con tus tokens.</p>
      )}
    </main>
  )
}
