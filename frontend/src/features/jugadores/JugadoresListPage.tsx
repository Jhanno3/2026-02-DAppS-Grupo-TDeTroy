import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listarJugadores } from '../../api/jugadores'
import styles from './JugadoresListPage.module.css'

/** UC-07: catálogo público — muestra también los jugadores dados de baja (spec.md UC-07). */
export function JugadoresListPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['jugadores'],
    queryFn: listarJugadores,
  })

  return (
    <div className={styles.container}>
      <h1 className={styles.title}>Catálogo de jugadores</h1>

      {isLoading && <p>Cargando catálogo…</p>}
      {isError && (
        <p role="alert">No se pudo cargar el catálogo. Intentá de nuevo.</p>
      )}
      {data && data.length === 0 && (
        <p className={styles.empty}>Todavía no hay jugadores dados de alta.</p>
      )}

      {data && data.length > 0 && (
        <div className={styles.grid}>
          {data.map((jugador) => (
            <Link
              key={jugador.id}
              to={`/jugadores/${jugador.id}`}
              className={styles.card}
            >
              <h2 className={styles.nombre}>{jugador.nombre}</h2>
              <p className={styles.club}>
                {jugador.club} · {jugador.posicion}
              </p>
              <div className={styles.meta}>
                <span>{jugador.tokensDisponibles}/100 tokens disponibles</span>
                <span
                  className={`${styles.badge} ${
                    jugador.estado === 'ACTIVO'
                      ? styles.badgeActivo
                      : styles.badgeInactivo
                  }`}
                >
                  {jugador.estado}
                </span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
