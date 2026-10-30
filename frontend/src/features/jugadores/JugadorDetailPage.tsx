import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import {
  listarCotizaciones,
  obtenerEstadisticas,
  obtenerJugador,
  type CotizacionResponse,
  type EstadisticasTemporadaResponse,
  type JugadorResponse,
} from '../../api/jugadores'
import { listarOfertas } from '../../api/ofertas'
import { listarSubastas } from '../../api/subastas'
import { ApiError } from '../../api/client'
import { useTema } from '../../app/tema'
import { Avatar } from '../../shared/components/Avatar'
import { formatMoney } from '../../shared/format'
import { CotizacionHistorialChart } from './CotizacionHistorialChart'
import { TradePanel } from './TradePanel'
import styles from './JugadorDetailPage.module.css'

// fechaNacimiento es un LocalDate (sin huso horario) — forzar 'UTC' acá evita el clásico
// off-by-one de `new Date('YYYY-MM-DD')` (se parsea como medianoche UTC) formateado después
// en el huso horario local, que puede correr la fecha un día para atrás.
const formatoNacimiento = new Intl.DateTimeFormat('es-AR', {
  dateStyle: 'medium',
  timeZone: 'UTC',
})
// fechaUltimaActualizacionRendimiento sí es un Instant real — se muestra en el huso local.
const formatoActualizacion = new Intl.DateTimeFormat('es-AR', {
  dateStyle: 'short',
  timeStyle: 'short',
})
const formatoPorcentaje = new Intl.NumberFormat('es-AR', {
  maximumFractionDigits: 1,
  signDisplay: 'always',
})

const RANGOS = [
  { id: '4S', semanas: 4 },
  { id: '12S', semanas: 12 },
  { id: 'Todo', semanas: null },
] as const
type Rango = (typeof RANGOS)[number]['id']

const EMISION_TOTAL = 100

/**
 * UC-07/UC-08: ficha de un jugador (modeloHTML/ficha-jugador-*.html) — identificatorios,
 * cotización vigente e histórica, rendimiento de temporada y operaciones: compra/venta contra el
 * sistema (UC-09/UC-10), ofertas P2P (UC-17) y subastas (UC-18). Respeta el tema claro/oscuro
 * del usuario (ver `app/TemaProvider`).
 */
export function JugadorDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { tema, alternar } = useTema()
  const [rango, setRango] = useState<Rango>('Todo')

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

  const estadisticas = useQuery({
    queryKey: ['estadisticas', id],
    queryFn: () => obtenerEstadisticas(id!),
    enabled: Boolean(id),
  })

  const historial = cotizaciones.data ?? []
  const semanasDelRango = RANGOS.find((r) => r.id === rango)!.semanas
  const historialVisible =
    semanasDelRango === null ? historial : historial.slice(-semanasDelRango)
  const cotizacionVigente = historial.at(-1)
    ? Number(historial.at(-1)!.valor)
    : null

  return (
    <div className={styles.scope}>
      <main className={styles.container}>
        <div className={styles.migas}>
          <Link to="/mercado" className={styles.migasLink}>
            ← Mercado
            {data && (
              <>
                {' '}
                / {data.liga} /{' '}
                <span className={styles.migasActual}>
                  {apellidoDe(data.nombre)}
                </span>
              </>
            )}
          </Link>
          <button
            type="button"
            className={styles.botonTema}
            onClick={alternar}
            aria-label={
              tema === 'oscuro'
                ? 'Cambiar a modo claro'
                : 'Cambiar a modo oscuro'
            }
          >
            {tema === 'oscuro' ? '☀ Modo claro' : '☾ Modo oscuro'}
          </button>
        </div>

        {isLoading && <p className={styles.estado}>Cargando…</p>}

        {error && (
          <p role="alert" className={styles.estado}>
            {error instanceof ApiError && error.status === 404
              ? 'No existe un jugador con ese id.'
              : 'No se pudo cargar el jugador. Intentá de nuevo.'}
          </p>
        )}

        {data && (
          <>
            <Encabezado jugador={data} historial={historial} />

            <div className={styles.cuerpo}>
              <div className={styles.principal}>
                <section
                  className={styles.tarjeta}
                  aria-label="Historial de cotización"
                >
                  <div className={styles.tarjetaTitulo}>
                    <h2>Historial de cotización</h2>
                    <div
                      className={styles.segmentado}
                      role="group"
                      aria-label="Rango del historial"
                    >
                      {RANGOS.map((r) => (
                        <button
                          key={r.id}
                          type="button"
                          aria-pressed={rango === r.id}
                          onClick={() => setRango(r.id)}
                        >
                          {r.id}
                        </button>
                      ))}
                    </div>
                  </div>
                  {cotizaciones.isLoading && (
                    <p className={styles.muted}>Cargando…</p>
                  )}
                  {cotizaciones.error && (
                    <p role="alert">No se pudo cargar el historial.</p>
                  )}
                  {cotizaciones.data && (
                    <CotizacionHistorialChart cotizaciones={historialVisible} />
                  )}
                </section>

                <Rendimiento
                  estadisticas={estadisticas.data}
                  cargando={estadisticas.isLoading}
                  conError={Boolean(estadisticas.error)}
                  actualizado={data.fechaUltimaActualizacionRendimiento}
                />
              </div>

              <aside className={styles.lateral}>
                <section aria-label="Operar">
                  <TradePanel
                    jugadorId={data.id}
                    cotizacionVigente={cotizacionVigente}
                    tokensDisponibles={data.tokensDisponibles}
                    estaActivo={data.estado === 'ACTIVO'}
                  />
                </section>

                <MercadoP2P
                  jugadorId={data.id}
                  activo={data.estado === 'ACTIVO'}
                />
                <SubastasDelJugador
                  jugadorId={data.id}
                  activo={data.estado === 'ACTIVO'}
                />
              </aside>
            </div>
          </>
        )}
      </main>
    </div>
  )
}

function Encabezado({
  jugador,
  historial,
}: {
  jugador: JugadorResponse
  historial: CotizacionResponse[]
}) {
  const vigente = historial.at(-1)
  const anterior = historial.at(-2)
  const variacion =
    vigente && anterior && Number(anterior.valor) !== 0
      ? ((Number(vigente.valor) - Number(anterior.valor)) /
          Number(anterior.valor)) *
        100
      : null
  const activo = jugador.estado === 'ACTIVO'
  const porcentajeTokens = (jugador.tokensDisponibles / EMISION_TOTAL) * 100

  return (
    <section className={styles.hero}>
      <div className={styles.identidad}>
        <div className={styles.avatar}>
          <Avatar src={jugador.fotoUrl} nombre={jugador.nombre} size="fill" />
        </div>
        <div className={styles.identidadTexto}>
          <div className={styles.nombreFila}>
            <h1>{jugador.nombre}</h1>
            <span
              className={`${styles.estadoPill} ${
                activo ? '' : styles.estadoPillInactivo
              }`}
            >
              ● {jugador.estado}
            </span>
          </div>
          <div className={styles.subtitulo}>
            {jugador.club} · {jugador.posicion} · {jugador.liga} ·{' '}
            {jugador.nacionalidad} · {edadDe(jugador.fechaNacimiento)} años (
            {formatoNacimiento.format(new Date(jugador.fechaNacimiento))})
          </div>
        </div>
      </div>

      <div className={styles.metricas}>
        <div className={styles.metrica} aria-label="Cotización">
          <span className={styles.metricaEtiqueta}>Cotización</span>
          <span className={styles.cotizacion}>
            {vigente ? formatMoney(vigente.valor) : '—'}
          </span>
          <span className={styles.metricaDetalle}>
            {!vigente ? (
              'Sin cotización calculada'
            ) : variacion === null ? (
              `${vigente.semana} · var. — (1.ª semana)`
            ) : (
              <>
                {vigente.semana} · var.{' '}
                <span className={variacion >= 0 ? styles.sube : styles.baja}>
                  {formatoPorcentaje.format(variacion)}%
                </span>{' '}
                vs {anterior!.semana}
              </>
            )}
          </span>
        </div>

        <div className={`${styles.metrica} ${styles.metricaTokens}`}>
          <span className={styles.metricaEtiqueta}>Tokens disponibles</span>
          <span className={styles.tokens}>
            {jugador.tokensDisponibles}{' '}
            <span className={styles.muted}>/ {EMISION_TOTAL}</span>
          </span>
          <div className={styles.barra} aria-hidden="true">
            <div style={{ width: `${porcentajeTokens}%` }} />
          </div>
        </div>
      </div>
    </section>
  )
}

function Rendimiento({
  estadisticas,
  cargando,
  conError,
  actualizado,
}: {
  estadisticas: EstadisticasTemporadaResponse | null | undefined
  cargando: boolean
  conError: boolean
  actualizado: string | null
}) {
  return (
    <section className={styles.tarjeta} aria-label="Rendimiento de temporada">
      <div className={styles.tarjetaTitulo}>
        <h2>Rendimiento · temporada</h2>
        {actualizado && (
          <span className={styles.muted}>
            Actualizado {formatoActualizacion.format(new Date(actualizado))}
          </span>
        )}
      </div>

      {cargando && <p className={styles.muted}>Cargando…</p>}
      {conError && <p role="alert">No se pudieron cargar las estadísticas.</p>}
      {estadisticas === null && (
        <p className={styles.muted}>Todavía sin estadísticas de temporada.</p>
      )}

      {estadisticas && (
        <>
          <div className={styles.statsScroll}>
            <dl className={styles.stats}>
              {(
                [
                  ['PJ', estadisticas.partidosJugados, false],
                  ['Minutos', estadisticas.minutosJugados, false],
                  ['Goles', estadisticas.goles, true],
                  ['Asist.', estadisticas.asistencias, true],
                  ['Figura', estadisticas.figuraDelPartido, false],
                  ['Amarillas', estadisticas.tarjetasAmarillas, false],
                  ['Rojas', estadisticas.tarjetasRojas, false],
                ] as const
              ).map(([etiqueta, valor, destacada]) => (
                <div
                  key={etiqueta}
                  className={`${styles.stat} ${
                    destacada ? styles.statDestacada : ''
                  }`}
                >
                  <dt>{etiqueta}</dt>
                  <dd>{valor}</dd>
                </div>
              ))}
            </dl>
          </div>
          <Derivadas estadisticas={estadisticas} />
        </>
      )}
    </section>
  )
}

function Derivadas({
  estadisticas,
}: {
  estadisticas: EstadisticasTemporadaResponse
}) {
  const { partidosJugados, minutosJugados, goles, asistencias } = estadisticas
  const golesMasAsistencias = goles + asistencias
  return (
    <div className={styles.derivadas}>
      <span>
        Min / partido{' '}
        <span className={styles.derivadaValor}>
          {partidosJugados > 0
            ? Math.round(minutosJugados / partidosJugados)
            : '—'}
        </span>
      </span>
      <span>
        Goles + asist.{' '}
        <span className={styles.derivadaValor}>{golesMasAsistencias}</span>
      </span>
      <span>
        Min por G+A{' '}
        <span className={styles.derivadaValor}>
          {golesMasAsistencias > 0
            ? Math.round(minutosJugados / golesMasAsistencias)
            : '—'}
        </span>
      </span>
    </div>
  )
}

function MercadoP2P({
  jugadorId,
  activo,
}: {
  jugadorId: string
  activo: boolean
}) {
  const ofertas = useQuery({
    queryKey: ['ofertas', jugadorId],
    queryFn: () => listarOfertas(jugadorId),
  })
  const abiertas = (ofertas.data ?? []).filter((o) => o.estado === 'ABIERTA')
  const mejorPrecio = abiertas.length
    ? Math.min(...abiertas.map((o) => Number(o.precioUnitario)))
    : null

  return (
    <section className={styles.tarjetaChica} aria-label="Mercado P2P">
      <div className={styles.tarjetaTitulo}>
        <h2>Mercado P2P</h2>
        {activo && (
          <Link to={`/jugadores/${jugadorId}/p2p`} className={styles.accion}>
            {abiertas.length > 0 ? 'Ver ofertas →' : 'Publicar oferta →'}
          </Link>
        )}
      </div>
      <p className={styles.muted}>
        {abiertas.length === 0
          ? 'Todavía no hay ofertas entre usuarios para este jugador.'
          : `${abiertas.length} ${
              abiertas.length === 1 ? 'oferta abierta' : 'ofertas abiertas'
            } · desde ${formatMoney(mejorPrecio!)} por token.`}
      </p>
    </section>
  )
}

function SubastasDelJugador({
  jugadorId,
  activo,
}: {
  jugadorId: string
  activo: boolean
}) {
  const subastas = useQuery({
    queryKey: ['subastas', 'jugador', jugadorId],
    queryFn: () => listarSubastas(jugadorId),
  })
  const enCurso = subastas.data ?? []

  return (
    <section className={styles.tarjetaChica} aria-label="Subastas">
      <div className={styles.tarjetaTitulo}>
        <h2>Subastas</h2>
        {activo && (
          <Link
            to={`/jugadores/${jugadorId}/subasta`}
            className={styles.accion}
          >
            Subastar →
          </Link>
        )}
      </div>
      {enCurso.length === 0 ? (
        <p className={styles.muted}>
          No hay subastas en curso de este jugador.
        </p>
      ) : (
        <ul className={styles.listaSubastas}>
          {enCurso.map((subasta) => (
            <li key={subasta.id}>
              <Link to={`/subastas/${subasta.id}`}>
                <span>
                  {subasta.cantidad} token(s) · {subasta.cantidadPujas}{' '}
                  {subasta.cantidadPujas === 1 ? 'puja' : 'pujas'}
                </span>
                <span className={styles.derivadaValor}>
                  {formatMoney(subasta.precioActual ?? subasta.precioInicial)}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function apellidoDe(nombre: string): string {
  return nombre.trim().split(/\s+/).at(-1) ?? nombre
}

function edadDe(fechaNacimiento: string): number {
  const nacimiento = new Date(fechaNacimiento)
  const hoy = new Date()
  let edad = hoy.getUTCFullYear() - nacimiento.getUTCFullYear()
  const cumplioEsteAnio =
    hoy.getUTCMonth() > nacimiento.getUTCMonth() ||
    (hoy.getUTCMonth() === nacimiento.getUTCMonth() &&
      hoy.getUTCDate() >= nacimiento.getUTCDate())
  if (!cumplioEsteAnio) edad--
  return edad
}
