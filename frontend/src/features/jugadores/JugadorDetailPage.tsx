import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { listarCotizaciones, obtenerJugador } from '../../api/jugadores'
import { ApiError } from '../../api/client'
import { Avatar } from '../../shared/components/Avatar'
import { LinkButton } from '../../shared/components/LinkButton'
import { CotizacionChart } from './CotizacionChart'
import { TradePanel } from './TradePanel'
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
 * UC-07/UC-08: detalle de un jugador — identificatorios, cotización histórica (gráfico) y el panel
 * de compra/venta contra el sistema (UC-09/UC-10).
 */
export function JugadorDetailPage() {
  const { id } = useParams<{ id: string }>()

  const { data, isLoading, error } = useQuery({
    queryKey: ['jugadores', id],
    queryFn: () => obtenerJugador(id!),
    enabled: Boolean(id),
  })

  const cotizaciones = useQuery({
    queryKey: ['cotizaciones', id],
    queryFn: () => listarCotizaciones(id!),
    enabled: Boolean(id),
  })

  return (
    <div className={styles.scope}>
      <main className={styles.container}>
        <Link to="/mercado" className={styles.back}>
          ← Volver al mercado
        </Link>

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
              <div className={styles.headerIdentidad}>
                <Avatar src={data.fotoUrl} nombre={data.nombre} size="large" />
                <h1 className={styles.nombre}>{data.nombre}</h1>
              </div>
              <div className={styles.headerRight}>
                <span
                  className={`${styles.badge} ${
                    data.estado === 'ACTIVO'
                      ? styles.badgeActivo
                      : styles.badgeInactivo
                  }`}
                >
                  {data.estado}
                </span>
                {data.estado === 'ACTIVO' && (
                  <LinkButton
                    to={`/jugadores/${data.id}/p2p`}
                    variant="secondary"
                    small
                  >
                    Oferta P2P
                  </LinkButton>
                )}
              </div>
            </div>

            <dl className={styles.datos}>
              <dt>Club</dt>
              <dd>{data.club}</dd>

              <dt>Posición</dt>
              <dd>{data.posicion}</dd>

              <dt>Nacionalidad</dt>
              <dd>{data.nacionalidad}</dd>

              <dt>Liga</dt>
              <dd>{data.liga}</dd>

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

            <div className={styles.layout}>
              <section className={styles.panel} aria-label="Cotización">
                <h2>Cotización</h2>
                {cotizaciones.isLoading && <p>Cargando…</p>}
                {cotizaciones.data && (
                  <CotizacionChart cotizaciones={cotizaciones.data} />
                )}
              </section>

              <section aria-label="Operar">
                <TradePanel
                  jugadorId={data.id}
                  cotizacionVigente={
                    cotizaciones.data?.at(-1)
                      ? Number(cotizaciones.data.at(-1)!.valor)
                      : null
                  }
                  tokensDisponibles={data.tokensDisponibles}
                  estaActivo={data.estado === 'ACTIVO'}
                />
              </section>
            </div>
          </>
        )}
      </main>
    </div>
  )
}
