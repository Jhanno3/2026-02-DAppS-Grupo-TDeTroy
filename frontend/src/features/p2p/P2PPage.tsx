import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import {
  cancelarOferta,
  comprarDeOferta,
  listarOfertas,
  type OfertaResponse,
} from '../../api/ofertas'
import { listarJugadores, type JugadorResponse } from '../../api/jugadores'
import { obtenerPortfolio } from '../../api/portfolio'
import { ApiError } from '../../api/client'
import { useAuth } from '../auth/AuthContext'
import { Avatar } from '../../shared/components/Avatar'
import { Button } from '../../shared/components/Button'
import { SelectorJugadorPropio } from '../../shared/components/SelectorJugadorPropio'
import { jugadoresConTokensDe } from '../../shared/jugadoresConTokens'
import { formatMoney } from '../../shared/format'
import styles from './P2PPage.module.css'

const formatoFechaHora = new Intl.DateTimeFormat('es-AR', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

/**
 * UC-17: listado global de ofertas P2P abiertas de todo el catálogo (pestaña "P2P", al lado de
 * "Mercado") — `GET /ofertas` sin `jugadorId` (ver `OfertaController.listar`). El precio de cada
 * oferta es el que fijó libremente su propio vendedor al publicarla, nunca la cotización vigente
 * del jugador; si se cumple el tiempo límite sin venderse, el sistema la cancela sola.
 */
export function P2PPage() {
  const { token, user, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [cantidades, setCantidades] = useState<Record<string, number>>({})
  const [error, setError] = useState<string | null>(null)
  const [selectorAbierto, setSelectorAbierto] = useState(false)

  const ofertas = useQuery({
    queryKey: ['ofertas'],
    queryFn: () => listarOfertas(),
  })

  const jugadores = useQuery({
    queryKey: ['jugadores'],
    queryFn: listarJugadores,
  })

  const portfolio = useQuery({
    queryKey: ['portfolio', 'me'],
    queryFn: () => obtenerPortfolio(token!),
    enabled: isAuthenticated && selectorAbierto,
  })

  const jugadorPorId = useMemo(() => {
    const mapa = new Map<string, JugadorResponse>()
    for (const j of jugadores.data ?? []) mapa.set(j.id, j)
    return mapa
  }, [jugadores.data])

  const jugadoresConTokens = useMemo(
    () => jugadoresConTokensDe(portfolio.data?.tenencias, jugadorPorId),
    [portfolio.data, jugadorPorId],
  )

  function handleCrearOferta() {
    if (!isAuthenticated) {
      void navigate('/login')
      return
    }
    setSelectorAbierto(true)
  }

  function cerrarSelector() {
    setSelectorAbierto(false)
  }

  async function invalidarTodo() {
    await queryClient.invalidateQueries({ queryKey: ['ofertas'] })
  }

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

  function cantidadDe(ofertaId: string): number {
    return cantidades[ofertaId] ?? 1
  }

  const isLoading = ofertas.isLoading || jugadores.isLoading
  const isError = ofertas.isError || jugadores.isError

  return (
    <div className={styles.scope}>
      <main className={styles.wrap}>
        <div className={styles.toolbar}>
          <div>
            <h1>P2P</h1>
            <p className={styles.lead}>
              Ofertas publicadas por otros usuarios para vender tokens: cada una
              a su propio precio y con un tiempo límite fijado por el vendedor
              (UC-17).
            </p>
          </div>
          <Button variant="primary" onClick={handleCrearOferta}>
            Crear nueva oferta
          </Button>
        </div>

        {isLoading && <p className={styles.lead}>Cargando ofertas…</p>}
        {isError && (
          <p role="alert" className={styles.lead}>
            No se pudieron cargar las ofertas. Intentá de nuevo.
          </p>
        )}

        {error != null && (
          <p role="alert" className={styles.error}>
            {error}
          </p>
        )}

        {!isLoading && !isError && (
          <section className={styles.grid} aria-label="Ofertas P2P abiertas">
            {(ofertas.data ?? []).length === 0 ? (
              <p className={styles.empty}>
                No hay ofertas P2P abiertas en este momento.
              </p>
            ) : (
              (ofertas.data ?? []).map((oferta) => (
                <TarjetaOferta
                  key={oferta.id}
                  oferta={oferta}
                  jugador={jugadorPorId.get(oferta.jugadorId)}
                  esPropia={oferta.vendedorId === user?.usuarioId}
                  isAuthenticated={isAuthenticated}
                  cantidad={cantidadDe(oferta.id)}
                  onCantidadChange={(cantidad) =>
                    setCantidades((prev) => ({
                      ...prev,
                      [oferta.id]: cantidad,
                    }))
                  }
                  onComprar={() =>
                    comprarMutation.mutate({
                      ofertaId: oferta.id,
                      cantidad: cantidadDe(oferta.id),
                    })
                  }
                  onCancelar={() => cancelarMutation.mutate(oferta.id)}
                  procesando={
                    comprarMutation.isPending || cancelarMutation.isPending
                  }
                />
              ))
            )}
          </section>
        )}
      </main>

      {selectorAbierto && (
        <SelectorJugadorPropio
          jugadores={jugadoresConTokens}
          cargando={portfolio.isLoading}
          destinoDe={(jugadorId) => `/jugadores/${jugadorId}/p2p`}
          ariaLabel="Elegí un jugador para publicar una oferta"
          onClose={cerrarSelector}
        />
      )}
    </div>
  )
}

interface TarjetaOfertaProps {
  oferta: OfertaResponse
  jugador: JugadorResponse | undefined
  esPropia: boolean
  isAuthenticated: boolean
  cantidad: number
  onCantidadChange: (cantidad: number) => void
  onComprar: () => void
  onCancelar: () => void
  procesando: boolean
}

function TarjetaOferta({
  oferta,
  jugador,
  esPropia,
  isAuthenticated,
  cantidad,
  onCantidadChange,
  onComprar,
  onCancelar,
  procesando,
}: TarjetaOfertaProps) {
  return (
    <div className={styles.card}>
      <Link to={`/jugadores/${oferta.jugadorId}`} className={styles.cardHeader}>
        <Avatar
          src={jugador?.fotoUrl ?? null}
          nombre={jugador?.nombre ?? '?'}
        />
        <div>
          <div className={styles.cardName}>{jugador?.nombre ?? 'Jugador'}</div>
          {jugador && (
            <div className={styles.cardClub}>
              {jugador.club} · {jugador.posicion}
            </div>
          )}
        </div>
      </Link>

      <div className={styles.cardPrice}>
        <strong>{formatMoney(oferta.precioUnitario)}</strong>
        <span>por token</span>
      </div>

      <div className={styles.cardMeta}>
        <span>
          {oferta.cantidadDisponible} / {oferta.cantidadPublicada} disponibles
        </span>
        <span>
          Vence {formatoFechaHora.format(new Date(oferta.fechaVencimiento))}
        </span>
      </div>

      {esPropia && <span className={styles.badgePropia}>Tu oferta</span>}

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
        <div className={styles.cardComprar}>
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
            className={styles.btnComprar}
            onClick={onComprar}
            disabled={procesando}
          >
            Comprar
          </button>
        </div>
      ) : (
        <p className={styles.hint}>
          <Link to="/login">Iniciá sesión</Link> para comprar.
        </p>
      )}
    </div>
  )
}
