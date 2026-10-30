import { useCallback, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useLocation, useParams } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { obtenerJugador } from '../../api/jugadores'
import {
  cancelarSubasta,
  obtenerSubasta,
  pujar,
  type PujaResponse,
  type SubastaResponse,
} from '../../api/subastas'
import { useAuth } from '../auth/AuthContext'
import { Avatar } from '../../shared/components/Avatar'
import { Toast } from '../../shared/components/Toast'
import { formatMoney } from '../../shared/format'
import { formatRestante, useMsRestantes } from './cuentaRegresiva'
import styles from './Subastas.module.css'

const formatoHora = new Intl.DateTimeFormat('es-AR', { timeStyle: 'medium' })

/**
 * UC-18: detalle de una subasta. Mientras está activa se refresca cada 2 segundos para seguir las
 * pujas de los demás; los botones de puja muestran los precios exactos que calcula el backend
 * (`opcionesPuja`), nunca un monto calculado acá.
 */
export function SubastaDetailPage() {
  const { id } = useParams<{ id: string }>()
  const location = useLocation()
  const { token, user, isAuthenticated } = useAuth()
  const queryClient = useQueryClient()
  const [aviso, setAviso] = useState<string | null>(
    (location.state as { creada?: boolean } | null)?.creada
      ? 'Subasta creada exitosamente'
      : null,
  )
  const cerrarAviso = useCallback(() => setAviso(null), [])

  const subasta = useQuery({
    queryKey: ['subastas', id],
    queryFn: () => obtenerSubasta(id!),
    enabled: Boolean(id),
    refetchInterval: (query) =>
      query.state.data?.estado === 'ACTIVA' ? 2000 : false,
  })

  const jugadorId = subasta.data?.jugadorId
  const jugador = useQuery({
    queryKey: ['jugadores', jugadorId],
    queryFn: () => obtenerJugador(jugadorId!),
    enabled: Boolean(jugadorId),
  })

  async function refrescar() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['subastas'] }),
      queryClient.invalidateQueries({ queryKey: ['portfolio', 'me'] }),
    ])
  }

  const pujarMutation = useMutation({
    mutationFn: (porcentaje: number) => pujar(id!, porcentaje, token!),
    onSuccess: async () => {
      setAviso('Puja registrada: vas ganando')
      await refrescar()
    },
  })

  const cancelarMutation = useMutation({
    mutationFn: () => cancelarSubasta(id!, token!),
    onSuccess: async () => {
      setAviso('Subasta cancelada')
      await refrescar()
    },
  })

  const errorAccion = pujarMutation.error ?? cancelarMutation.error

  return (
    <div className={styles.scope}>
      <main className={styles.containerAngosto}>
        <Link to="/subastas" className={styles.back}>
          ← Volver a subastas
        </Link>

        {subasta.isLoading && <p>Cargando…</p>}
        {subasta.error && (
          <p role="alert">
            {subasta.error instanceof ApiError && subasta.error.status === 404
              ? 'No existe esa subasta.'
              : 'No se pudo cargar la subasta. Intentá de nuevo.'}
          </p>
        )}

        {subasta.data && (
          <>
            <div className={styles.header}>
              <Link
                to={`/jugadores/${subasta.data.jugadorId}`}
                className={styles.headerJugador}
              >
                <Avatar
                  src={jugador.data?.fotoUrl ?? null}
                  nombre={jugador.data?.nombre ?? '?'}
                  size="large"
                />
                <div>
                  <h1>{jugador.data?.nombre ?? 'Jugador'}</h1>
                  <p>
                    {jugador.data?.club}
                    {jugador.data?.posicion && ` · ${jugador.data.posicion}`}
                  </p>
                </div>
              </Link>
              <EstadoBadge estado={subasta.data.estado} />
            </div>

            {errorAccion && (
              <p role="alert" className={styles.error}>
                {errorAccion instanceof ApiError
                  ? errorAccion.message
                  : 'No se pudo completar la operación.'}
              </p>
            )}

            <div className={styles.layout}>
              <PanelPrecio subasta={subasta.data} usuarioId={user?.usuarioId} />

              <PanelPujar
                subasta={subasta.data}
                usuarioId={user?.usuarioId}
                isAuthenticated={isAuthenticated}
                pujando={pujarMutation.isPending}
                onPujar={(porcentaje) => {
                  cancelarMutation.reset()
                  pujarMutation.mutate(porcentaje)
                }}
                cancelando={cancelarMutation.isPending}
                onCancelar={() => {
                  pujarMutation.reset()
                  if (
                    window.confirm(
                      '¿Cancelar la subasta? Los tokens vuelven a tu cartera y, si había un mejor postor, se le devuelve su saldo.',
                    )
                  ) {
                    cancelarMutation.mutate()
                  }
                }}
              />
            </div>

            <section className={`${styles.panel} ${styles.separado}`}>
              <h2>Historial de pujas</h2>
              <Historial
                pujas={subasta.data.pujas ?? []}
                usuarioId={user?.usuarioId}
              />
            </section>
          </>
        )}
      </main>

      {aviso && <Toast mensaje={aviso} onClose={cerrarAviso} />}
    </div>
  )
}

function EstadoBadge({ estado }: { estado: SubastaResponse['estado'] }) {
  const etiqueta = {
    ACTIVA: 'En curso',
    ADJUDICADA: 'Adjudicada',
    DESIERTA: 'Desierta',
    CANCELADA: 'Cancelada',
  }[estado]
  return (
    <span
      className={`${styles.badge} ${
        estado === 'ACTIVA' ? styles.badgeActiva : styles.badgeCerrada
      }`}
    >
      {etiqueta}
    </span>
  )
}

function PanelPrecio({
  subasta,
  usuarioId,
}: {
  subasta: SubastaResponse
  usuarioId: string | undefined
}) {
  const restante = useMsRestantes(subasta.cierraEn)
  const activa = subasta.estado === 'ACTIVA'
  const precio = subasta.precioActual ?? subasta.precioInicial
  const totalVigente = Number(precio) * subasta.cantidad

  return (
    <section className={styles.panel} aria-label="Precio vigente">
      <div className={styles.precioVigente}>
        <small>
          {subasta.precioActual ? 'Mejor puja' : 'Precio inicial'} · por token
        </small>
        <strong>{formatMoney(precio)}</strong>
        <span>
          {subasta.cantidad} token(s) · total {formatMoney(totalVigente)}
        </span>
      </div>

      {activa && (
        <div className={styles.cierre} role="timer" aria-live="off">
          <small>{subasta.precioActual ? 'Se adjudica en' : 'Vence en'}</small>
          <div>
            <strong>
              {restante > 0 ? formatRestante(restante) : 'Cerrando…'}
            </strong>
          </div>
          <p>
            {subasta.precioActual
              ? 'Si nadie puja antes, gana el mejor postor. Cada puja nueva reinicia el reloj a 2 minutos.'
              : 'Si vence sin pujas, la subasta queda desierta y los tokens vuelven al vendedor.'}
          </p>
        </div>
      )}

      <dl className={styles.datos}>
        <dt>Precio inicial</dt>
        <dd>{formatMoney(subasta.precioInicial)}</dd>
        <dt>Pujas</dt>
        <dd>{subasta.cantidadPujas}</dd>
        <dt>Mejor postor</dt>
        <dd>
          {subasta.mejorPostorId == null
            ? '—'
            : subasta.mejorPostorId === usuarioId
              ? 'Vos'
              : 'Otro usuario'}
        </dd>
      </dl>
    </section>
  )
}

function PanelPujar({
  subasta,
  usuarioId,
  isAuthenticated,
  pujando,
  onPujar,
  cancelando,
  onCancelar,
}: {
  subasta: SubastaResponse
  usuarioId: string | undefined
  isAuthenticated: boolean
  pujando: boolean
  onPujar: (porcentaje: number) => void
  cancelando: boolean
  onCancelar: () => void
}) {
  const restante = useMsRestantes(subasta.cierraEn)
  const esVendedor = subasta.vendedorId === usuarioId
  const esMejorPostor = subasta.mejorPostorId === usuarioId

  if (subasta.estado !== 'ACTIVA') {
    return (
      <section className={styles.panel} aria-label="Resultado">
        <h2>Resultado</h2>
        <p className={styles.resultado}>{resultadoDe(subasta, usuarioId)}</p>
      </section>
    )
  }

  return (
    <section className={styles.panel} aria-label="Pujar">
      <h2>{esVendedor ? 'Tu subasta' : 'Pujar'}</h2>

      {esVendedor ? (
        <>
          <p className={`${styles.hint} ${styles.hintConMargen}`}>
            No podés pujar en tu propia subasta. Podés cancelarla en cualquier
            momento: los tokens vuelven a tu cartera y al mejor postor se le
            devuelve su saldo.
          </p>
          <button
            type="button"
            className={styles.btnCancelar}
            onClick={onCancelar}
            disabled={cancelando}
          >
            {cancelando ? 'Cancelando…' : 'Cancelar subasta'}
          </button>
        </>
      ) : !isAuthenticated ? (
        <p className={styles.hint}>
          <Link to="/login">Iniciá sesión</Link> o{' '}
          <Link to="/registro">creá una cuenta</Link> para pujar.
        </p>
      ) : esMejorPostor ? (
        <p className={styles.resultado}>
          Vas ganando. Si nadie te supera en{' '}
          {restante > 0 ? formatRestante(restante) : 'unos segundos'}, la
          subasta es tuya.
        </p>
      ) : (
        <>
          <div className={styles.opciones}>
            {subasta.opcionesPuja.map((opcion) => (
              <button
                key={opcion.porcentaje}
                type="button"
                className={styles.opcion}
                onClick={() => onPujar(opcion.porcentaje)}
                disabled={pujando || restante === 0}
              >
                <span className={styles.opcionIncremento}>
                  {opcion.porcentaje === 0
                    ? 'Precio inicial'
                    : `+${opcion.porcentaje}%`}
                </span>
                <span className={styles.opcionPrecio}>
                  {formatMoney(opcion.precioUnitario)}
                </span>
                <span className={styles.opcionTotal}>
                  Total {formatMoney(opcion.montoTotal)}
                </span>
              </button>
            ))}
          </div>
          <p className={styles.hint}>
            Al pujar se te reserva el total de tu saldo. Si alguien te supera,
            se te devuelve completo.
          </p>
        </>
      )}
    </section>
  )
}

function resultadoDe(subasta: SubastaResponse, usuarioId: string | undefined) {
  const total = formatMoney(
    Number(subasta.precioActual ?? 0) * subasta.cantidad,
  )
  switch (subasta.estado) {
    case 'ADJUDICADA':
      if (subasta.mejorPostorId === usuarioId)
        return `¡Ganaste la subasta! Recibiste ${subasta.cantidad} token(s) por ${total}.`
      if (subasta.vendedorId === usuarioId)
        return `Vendiste ${subasta.cantidad} token(s) por ${total}.`
      return `Adjudicada a ${formatMoney(subasta.precioActual ?? 0)} por token.`
    case 'DESIERTA':
      return 'Venció sin pujas: los tokens volvieron al vendedor.'
    case 'CANCELADA':
      return 'El vendedor canceló la subasta.'
    default:
      return ''
  }
}

function Historial({
  pujas,
  usuarioId,
}: {
  pujas: PujaResponse[]
  usuarioId: string | undefined
}) {
  if (pujas.length === 0) {
    return <p className={styles.hint}>Todavía no hay pujas.</p>
  }

  // Alias estable por orden de primera puja (el historial viene del más reciente al más viejo).
  const alias = new Map<string, string>()
  for (const puja of [...pujas].reverse()) {
    if (!alias.has(puja.postorId)) {
      alias.set(puja.postorId, `Postor ${alias.size + 1}`)
    }
  }

  return (
    <ul className={styles.historial}>
      {pujas.map((puja) => {
        const esVos = puja.postorId === usuarioId
        return (
          <li key={`${puja.postorId}-${puja.fecha}`}>
            <span
              className={
                esVos ? styles.historialPostorVos : styles.historialPostor
              }
            >
              {esVos ? 'Vos' : alias.get(puja.postorId)}
            </span>
            <span className={styles.historialPrecio}>
              {formatMoney(puja.precioUnitario)}
            </span>
            <span className={styles.historialFecha}>
              {formatoHora.format(new Date(puja.fecha))}
            </span>
          </li>
        )
      })}
    </ul>
  )
}
