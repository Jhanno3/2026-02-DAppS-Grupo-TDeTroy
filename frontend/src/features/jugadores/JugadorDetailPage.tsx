import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { obtenerJugador } from '../../api/jugadores'
import { ApiError } from '../../api/client'
import { LinkButton } from '../../shared/components/LinkButton'
import styles from './JugadorDetailPage.module.css'

// fechaNacimiento es un LocalDate (sin huso horario) — forzar 'UTC' acá evita el clásico
// off-by-one de `new Date('YYYY-MM-DD')` (se parsea como medianoche UTC) formateado después
// en el huso horario local, que puede correr la fecha un día para atrás.
const formatoFecha = new Intl.DateTimeFormat('es-AR', {
  dateStyle: 'long',
  timeZone: 'UTC',
})
// fechaUltimaActualizacionRendimiento sí es un Instant real — mostrarlo en el huso local del
// navegador es lo correcto acá, sin forzar UTC.
const formatoFechaHora = new Intl.DateTimeFormat('es-AR', {
  dateStyle: 'long',
  timeStyle: 'short',
})

/**
 * UC-07: detalle de un jugador — identificatorios, tokens disponibles y fecha de última
 * actualización de rendimiento. Sin cotización/histórico todavía (UC-08 es T4.7, no construido).
 */
export function JugadorDetailPage() {
  const { id } = useParams<{ id: string }>()

  const { data, isLoading, error } = useQuery({
    queryKey: ['jugadores', id],
    queryFn: () => obtenerJugador(id!),
    enabled: Boolean(id),
  })

  return (
    <div className={styles.container}>
      <LinkButton to="/jugadores" variant="ghost" small className={styles.back}>
        ← Volver al catálogo
      </LinkButton>

      {isLoading && <p>Cargando…</p>}

      {error && (
        <p role="alert">
          {error instanceof ApiError && error.status === 404
            ? 'No existe un jugador con ese id.'
            : 'No se pudo cargar el jugador. Intentá de nuevo.'}
        </p>
      )}

      {data && (
        <>
          <div className={styles.header}>
            <h1 className={styles.nombre}>{data.nombre}</h1>
            <span
              className={`${styles.badge} ${
                data.estado === 'ACTIVO'
                  ? styles.badgeActivo
                  : styles.badgeInactivo
              }`}
            >
              {data.estado}
            </span>
          </div>

          <dl className={styles.datos}>
            <dt>Club</dt>
            <dd>{data.club}</dd>

            <dt>Posición</dt>
            <dd>{data.posicion}</dd>

            <dt>Nacionalidad</dt>
            <dd>{data.nacionalidad}</dd>

            <dt>Fecha de nacimiento</dt>
            <dd>{formatoFecha.format(new Date(data.fechaNacimiento))}</dd>

            <dt>Tokens disponibles</dt>
            <dd>{data.tokensDisponibles} / 100</dd>

            <dt>Última actualización de rendimiento</dt>
            <dd>
              {data.fechaUltimaActualizacionRendimiento
                ? formatoFechaHora.format(
                    new Date(data.fechaUltimaActualizacionRendimiento),
                  )
                : 'Todavía sin datos de rendimiento'}
            </dd>
          </dl>
        </>
      )}
    </div>
  )
}
