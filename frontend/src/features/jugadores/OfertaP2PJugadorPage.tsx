import { useCallback, useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import {
  cancelarOferta,
  comprarDeOferta,
  listarOfertas,
  publicarOferta,
  type OfertaResponse,
} from '../../api/ofertas'
import { listarCotizaciones, obtenerJugador } from '../../api/jugadores'
import { obtenerPortfolio } from '../../api/portfolio'
import { ApiError } from '../../api/client'
import { useAuth } from '../auth/AuthContext'
import { Avatar } from '../../shared/components/Avatar'
import { Toast } from '../../shared/components/Toast'
import { formatMoney } from '../../shared/format'
import styles from './OfertaP2PJugadorPage.module.css'

const formatoFechaHora = new Intl.DateTimeFormat('es-AR', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

/**
 * UC-17: venta directa entre usuarios mediante oferta abierta, para un jugador puntual — vía
 * adicional que convive con la compra/venta contra el sistema ({@link TradePanel}) sin
 * reemplazarla. El vendedor fija libremente, al publicar, el precio por token, la cantidad y un
 * tiempo límite en horas (si se cumple sin venderse, el sistema cancela la oferta automáticamente
 * y reintegra los tokens al vendedor). El campo de precio arranca con la cotización vigente sólo
 * como referencia: el vendedor puede fijar cualquier otro valor.
 */
export function OfertaP2PJugadorPage() {
  const { id } = useParams<{ id: string }>()
  const { token, user, isAuthenticated } = useAuth()
  const queryClient = useQueryClient()
  const [cantidadAPublicar, setCantidadAPublicar] = useState(1)
  // null = el usuario todavía no tocó el campo: se muestra la cotización vigente como punto de
  // partida (sólo referencia — el precio de la oferta lo fija libremente el vendedor, UC-17).
  const [precioEditado, setPrecioEditado] = useState<number | null>(null)
  const [ofertaCreada, setOfertaCreada] = useState(false)
  const [horasAPublicar, setHorasAPublicar] = useState(24)
  const [cantidadesAComprar, setCantidadesAComprar] = useState<
    Record<string, number>
  >({})
  const [error, setError] = useState<string | null>(null)

  const jugador = useQuery({
    queryKey: ['jugadores', id],
    queryFn: () => obtenerJugador(id!),
    enabled: Boolean(id),
  })

  const ofertas = useQuery({
    queryKey: ['ofertas', id],
    queryFn: () => listarOfertas(id),
    enabled: Boolean(id),
  })

  const cotizaciones = useQuery({
    queryKey: ['cotizaciones', id],
    queryFn: () => listarCotizaciones(id!),
    enabled: Boolean(id),
  })

  const cotizacionVigente = cotizaciones.data?.at(-1)
    ? Number(cotizaciones.data.at(-1)!.valor)
    : null
  const precioAPublicar = precioEditado ?? cotizacionVigente ?? 1

  const portfolio = useQuery({
    queryKey: ['portfolio', 'me'],
    queryFn: () => obtenerPortfolio(token!),
    enabled: isAuthenticated,
  })

  const misCopias =
    portfolio.data?.tenencias.find((t) => t.jugadorId === id)?.cantidad ?? 0

  async function invalidarTodo() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['ofertas', id] }),
      queryClient.invalidateQueries({ queryKey: ['portfolio', 'me'] }),
    ])
  }

  const publicarMutation = useMutation({
    mutationFn: () =>
      publicarOferta(
        id!,
        cantidadAPublicar,
        precioAPublicar,
        horasAPublicar,
        token!,
      ),
    onSuccess: async () => {
      setError(null)
      setCantidadAPublicar(1)
      setPrecioEditado(null)
      setOfertaCreada(true)
      setHorasAPublicar(24)
      await invalidarTodo()
    },
    onError: (err) =>
      setError(
        err instanceof ApiError
          ? err.message
          : 'No se pudo publicar la oferta.',
      ),
  })

  const comprarMutation = useMutation({
    mutationFn: ({
      ofertaId,
      cantidad,
    }: {
      ofertaId: string
      cantidad: number
    }) => comprarDeOferta(ofertaId, cantidad, token!),
    onSuccess: async () => {
      setError(null)
      await invalidarTodo()
    },
    onError: (err) =>
      setError(
        err instanceof ApiError
          ? err.message
          : 'No se pudo completar la compra.',
      ),
  })

  const cancelarMutation = useMutation({
    mutationFn: (ofertaId: string) => cancelarOferta(ofertaId, token!),
    onSuccess: async () => {
      setError(null)
      await invalidarTodo()
    },
    onError: (err) =>
      setError(
        err instanceof ApiError
          ? err.message
          : 'No se pudo cancelar la oferta.',
      ),
  })

  const cerrarAvisoOfertaCreada = useCallback(() => setOfertaCreada(false), [])

  function handlePublicar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    publicarMutation.mutate()
  }

  function cantidadAComprarDe(ofertaId: string): number {
    return cantidadesAComprar[ofertaId] ?? 1
  }

  return (
    <div className={styles.scope}>
      <main className={styles.container}>
        {id && (
          <Link to={`/jugadores/${id}`} className={styles.back}>
            ← Volver al jugador
          </Link>
        )}

        {jugador.isLoading && <p>Cargando…</p>}
        {jugador.isError && (
          <p role="alert">No se pudo cargar el jugador. Intentá de nuevo.</p>
        )}

        {jugador.data && (
          <div className={styles.header}>
            <Avatar
              src={jugador.data.fotoUrl}
              nombre={jugador.data.nombre}
              size="large"
            />
            <div>
              <h1 className={styles.nombre}>{jugador.data.nombre}</h1>
              <p className={styles.sub}>
                {jugador.data.club} · {jugador.data.posicion}
              </p>
            </div>
          </div>
        )}

        <section className={styles.panel}>
          <h2>Ofertas P2P de este jugador</h2>
          <p className={styles.hint}>
            Cualquier usuario puede publicar una cantidad de tokens que posee a
            un precio y un tiempo límite que fija libremente; si pasa ese tiempo
            sin venderse, se cancela sola y los tokens vuelven al vendedor
            (UC-17).
          </p>

          {ofertas.isLoading && <p>Cargando ofertas…</p>}
          {ofertas.isError && (
            <p role="alert">No se pudieron cargar las ofertas.</p>
          )}

          {ofertas.data && ofertas.data.length === 0 && (
            <p className={styles.hint}>
              No hay ofertas abiertas para este jugador.
            </p>
          )}

          {ofertas.data && ofertas.data.length > 0 && (
            <ul className={styles.lista}>
              {ofertas.data.map((oferta) => (
                <FilaOferta
                  key={oferta.id}
                  oferta={oferta}
                  esPropia={oferta.vendedorId === user?.usuarioId}
                  isAuthenticated={isAuthenticated}
                  cantidad={cantidadAComprarDe(oferta.id)}
                  onCantidadChange={(cantidad) =>
                    setCantidadesAComprar((prev) => ({
                      ...prev,
                      [oferta.id]: cantidad,
                    }))
                  }
                  onComprar={() =>
                    comprarMutation.mutate({
                      ofertaId: oferta.id,
                      cantidad: cantidadAComprarDe(oferta.id),
                    })
                  }
                  onCancelar={() => cancelarMutation.mutate(oferta.id)}
                  procesando={
                    comprarMutation.isPending || cancelarMutation.isPending
                  }
                />
              ))}
            </ul>
          )}

          {error != null && (
            <p role="alert" className={styles.error}>
              {error}
            </p>
          )}

          {!isAuthenticated && (
            <p className={styles.hint}>
              <Link to="/login">Iniciá sesión</Link> o{' '}
              <Link to="/registro">creá una cuenta</Link> para publicar o
              comprar tokens vía oferta directa.
            </p>
          )}

          {isAuthenticated &&
            (misCopias === 0 ? (
              <p className={styles.hint}>
                No tenés tokens disponibles de este jugador para publicar en una
                oferta.
              </p>
            ) : (
              <form className={styles.formPublicar} onSubmit={handlePublicar}>
                <h3 className={styles.formTitulo}>
                  Publicar oferta (tenés {misCopias} disponibles)
                </h3>
                <div className={styles.formGrid}>
                  <label className={styles.label}>
                    Cantidad
                    <input
                      type="number"
                      name="cantidadPublicar"
                      min={1}
                      max={misCopias}
                      value={cantidadAPublicar}
                      onChange={(e) =>
                        setCantidadAPublicar(
                          Math.min(
                            misCopias,
                            Math.max(
                              1,
                              Number.parseInt(e.target.value, 10) || 1,
                            ),
                          ),
                        )
                      }
                    />
                  </label>
                  <label className={styles.label}>
                    Precio por token
                    <input
                      type="number"
                      name="precioPublicar"
                      min={0.01}
                      step={0.01}
                      value={precioAPublicar}
                      onChange={(e) =>
                        setPrecioEditado(
                          Math.max(
                            0.01,
                            Number.parseFloat(e.target.value) || 0.01,
                          ),
                        )
                      }
                    />
                    {cotizacionVigente !== null && (
                      <span className={styles.referencia}>
                        Cotización actual: {formatMoney(cotizacionVigente)}
                      </span>
                    )}
                  </label>
                  <label className={styles.label}>
                    Tiempo límite (horas)
                    <input
                      type="number"
                      name="horasPublicar"
                      min={1}
                      value={horasAPublicar}
                      onChange={(e) =>
                        setHorasAPublicar(
                          Math.max(1, Number.parseInt(e.target.value, 10) || 1),
                        )
                      }
                    />
                  </label>
                </div>
                <button
                  type="submit"
                  className={styles.btnPrimario}
                  disabled={publicarMutation.isPending}
                >
                  {publicarMutation.isPending
                    ? 'Publicando…'
                    : 'Publicar oferta'}
                </button>
              </form>
            ))}
        </section>
      </main>

      {ofertaCreada && (
        <Toast
          mensaje="Oferta creada exitosamente"
          onClose={cerrarAvisoOfertaCreada}
        />
      )}
    </div>
  )
}

interface FilaOfertaProps {
  oferta: OfertaResponse
  esPropia: boolean
  isAuthenticated: boolean
  cantidad: number
  onCantidadChange: (cantidad: number) => void
  onComprar: () => void
  onCancelar: () => void
  procesando: boolean
}

function FilaOferta({
  oferta,
  esPropia,
  isAuthenticated,
  cantidad,
  onCantidadChange,
  onComprar,
  onCancelar,
  procesando,
}: FilaOfertaProps) {
  return (
    <li className={styles.fila}>
      <div className={styles.filaInfo}>
        <span className={styles.filaCantidad}>
          {oferta.cantidadDisponible} / {oferta.cantidadPublicada} disponibles
        </span>
        <span className={styles.filaPrecio}>
          {formatMoney(oferta.precioUnitario)} / token
        </span>
        <span className={styles.filaVence}>
          Vence {formatoFechaHora.format(new Date(oferta.fechaVencimiento))}
        </span>
        {esPropia && <span className={styles.badgePropia}>Tu oferta</span>}
      </div>

      {esPropia ? (
        <button
          type="button"
          className={styles.btnCancelar}
          onClick={onCancelar}
          disabled={procesando}
        >
          Cancelar
        </button>
      ) : isAuthenticated ? (
        <div className={styles.filaComprar}>
          <input
            type="number"
            name="cantidadComprar"
            min={1}
            max={oferta.cantidadDisponible}
            value={cantidad}
            onChange={(e) =>
              onCantidadChange(
                Math.min(
                  oferta.cantidadDisponible,
                  Math.max(1, Number.parseInt(e.target.value, 10) || 1),
                ),
              )
            }
            aria-label="Cantidad a comprar"
          />
          <button
            type="button"
            className={styles.btnSecundario}
            onClick={onComprar}
            disabled={procesando}
          >
            Comprar de la oferta
          </button>
        </div>
      ) : null}
    </li>
  )
}
