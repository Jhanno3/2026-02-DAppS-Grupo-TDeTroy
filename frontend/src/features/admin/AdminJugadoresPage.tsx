import { useQuery } from '@tanstack/react-query'
import { listarJugadores } from '../../api/jugadores'
import { LinkButton } from '../../shared/components/LinkButton'
import styles from './AdminJugadoresPage.module.css'

/** Panel ADMIN: catálogo con acciones de alta/edición (UC-03/UC-04). */
export function AdminJugadoresPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['jugadores'],
    queryFn: listarJugadores,
  })

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1 className={styles.title}>Administrar jugadores</h1>
        <LinkButton to="/admin/jugadores/nuevo">+ Nuevo jugador</LinkButton>
      </div>

      {isLoading && <p>Cargando…</p>}
      {isError && <p role="alert">No se pudo cargar el catálogo.</p>}

      {data && (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Club</th>
              <th>Posición</th>
              <th>Tokens disponibles</th>
              <th>Estado</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {data.map((jugador) => (
              <tr key={jugador.id}>
                <td>{jugador.nombre}</td>
                <td>{jugador.club}</td>
                <td>{jugador.posicion}</td>
                <td>{jugador.tokensDisponibles} / 100</td>
                <td>
                  <span
                    className={`${styles.badge} ${
                      jugador.estado === 'ACTIVO'
                        ? styles.badgeActivo
                        : styles.badgeInactivo
                    }`}
                  >
                    {jugador.estado}
                  </span>
                </td>
                <td>
                  <LinkButton
                    to={`/admin/jugadores/${jugador.id}/editar`}
                    variant="secondary"
                    small
                  >
                    Editar
                  </LinkButton>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
