import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ApiError } from '../../api/client'
import {
  cancelarOferta,
  listarOfertas,
  type OfertaResponse,
} from '../../api/ofertas'
import { obtenerPortfolio, type TenenciaPortfolio } from '../../api/portfolio'
import { listarSubastas, type SubastaResponse } from '../../api/subastas'
import { listarJugadores, type JugadorResponse } from '../../api/jugadores'
import {
  listarMisMovimientos,
  type MovimientoResponse,
} from '../../api/movimientos'
import { useAuth } from '../auth/AuthContext'
import { Avatar } from '../../shared/components/Avatar'
import { formatMoney } from '../../shared/format'
import styles from './CarteraPage.module.css'

const formatoFechaHora = new Intl.DateTimeFormat('es-AR', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

/**
 * Costo promedio de compra de un jugador: suma de (cantidad × precioUnitario) de cada compra
 * -contra el sistema o P2P, da igual quién fue el vendedor- dividido la cantidad total comprada,
 * ponderado por cantidad (no es el promedio simple de los precios unitarios). Es el costo
 * promedio ponderado estándar: sólo lo mueven las compras nuevas, nunca una venta parcial — vender
 * una parte de la tenencia no cambia el costo promedio de lo que queda. `null` si todavía no hay
 * ninguna compra registrada para ese jugador (no debería pasar para una tenencia con `cantidad >
 * 0`, pero una venta/compensación podría dejar movimientos inconsistentes en un dato de prueba).
 */
function costoPromedioDeCompra(
  movimientos: MovimientoResponse[],
  jugadorId: string,
): number | null {
  const compras = movimientos.filter(
    (m) =>
      m.jugadorId === jugadorId &&
      (m.tipo === 'COMPRA_SISTEMA' ||
        m.tipo === 'COMPRA_P2P' ||
        m.tipo === 'COMPRA_SUBASTA'),
  )
  const cantidadComprada = compras.reduce((acc, m) => acc + m.cantidad, 0)
  if (cantidadComprada === 0) return null

  const montoInvertido = compras.reduce(
    (acc, m) => acc + m.cantidad * Number(m.precioUnitario),
    0,
  )
  return montoInvertido / cantidadComprada
}

/**
 * UC-11: cartera del usuario autenticado. `GET /portfolios/me` sólo trae `jugadorId` por tenencia
 * (plan.md §2.5: sin entidad `Portfolio` propia) — se cruza con el catálogo público (UC-07) acá
 * para mostrar nombre/foto/club, y con el historial propio (UC-12) para el costo promedio de
 * compra, en vez de pedirle cualquiera de las dos cosas al backend. Las tenencias se muestran como
 * tarjetas, mismo formato que el catálogo de {@code MercadoPage} (UC-07), para que el usuario
 * reconozca el mismo jugador en los dos lugares.
 *
 * Los tokens reservados en ofertas P2P abiertas (UC-17) no están en `tenencias`: se listan aparte
 * en "En venta en P2P" (a partir de las ofertas abiertas propias, para poder cancelarlas desde
 * acá), y su valor sigue sumando al valor total.
 */
export function CarteraPage() {
  const { token, user } = useAuth()
  const queryClient = useQueryClient()

  const portfolio = useQuery({
    queryKey: ['portfolio', 'me'],
    queryFn: () => obtenerPortfolio(token!),
    enabled: Boolean(token),
  })

  const jugadores = useQuery({
    queryKey: ['jugadores'],
    queryFn: listarJugadores,
  })

  const movimientos = useQuery({
    queryKey: ['movimientos', 'me'],
    queryFn: () => listarMisMovimientos(token!),
    enabled: Boolean(token),
  })

  const ofertas = useQuery({
    queryKey: ['ofertas'],
    queryFn: () => listarOfertas(),
    enabled: Boolean(token),
  })

  const subastas = useQuery({
    queryKey: ['subastas'],
    queryFn: () => listarSubastas(),
    enabled: Boolean(token),
  })

  const cancelar = useMutation({
    mutationFn: (ofertaId: string) => cancelarOferta(ofertaId, token!),
    onSuccess: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: ['ofertas'] }),
        queryClient.invalidateQueries({ queryKey: ['portfolio', 'me'] }),
      ]),
  })

  const isLoading =
    portfolio.isLoading || jugadores.isLoading || movimientos.isLoading
  const isError = portfolio.isError || jugadores.isError

  const jugadoresPorId = new Map((jugadores.data ?? []).map((j) => [j.id, j]))
  const misOfertas = (ofertas.data ?? []).filter(
    (o) => o.vendedorId === user?.usuarioId && o.estado === 'ABIERTA',
  )
  // El listado público ya trae sólo subastas ACTIVA: acá se queda con las que me involucran.
  const misSubastas = (subastas.data ?? []).filter(
    (s) =>
      s.vendedorId === user?.usuarioId || s.mejorPostorId === user?.usuarioId,
  )
  const tieneTokensReservados =
    misOfertas.length > 0 ||
    misSubastas.some((s) => s.vendedorId === user?.usuarioId)

  return (
    <div className={styles.scope}>
      <main className={styles.container}>
        <div className={styles.titleRow}>
          <h1 className={styles.title}>Mi cartera</h1>
          <Link to="/cartera/movimientos" className={styles.historialLink}>
            Ver historial de movimientos →
          </Link>
        </div>

        {isLoading && <p>Cargando cartera…</p>}
        {isError && (
          <p role="alert">No se pudo cargar la cartera. Intentá de nuevo.</p>
        )}

        {portfolio.data && (
          <>
            <dl className={styles.kpis}>
              <div>
                <dt>Valor total</dt>
                <dd>{formatMoney(portfolio.data.valorTotal)}</dd>
              </div>
              <div>
                <dt>Saldo disponible</dt>
                <dd>{formatMoney(portfolio.data.saldoVirtual)}</dd>
              </div>
              <div>
                <dt>En venta (P2P y subastas)</dt>
                <dd>{formatMoney(portfolio.data.valorEnVenta)}</dd>
              </div>
              <div>
                <dt>Reservado en pujas</dt>
                <dd>{formatMoney(portfolio.data.saldoEnPujas)}</dd>
              </div>
              <div>
                <dt>Jugadores distintos</dt>
                <dd>{portfolio.data.tenencias.length}</dd>
              </div>
            </dl>

            {portfolio.data.tenencias.length === 0 ? (
              <p className={styles.empty}>
                {tieneTokensReservados
                  ? 'No tenés tokens disponibles: todos los que tenés están en venta en P2P o en subasta. '
                  : 'Todavía no tenés tokens. '}
                <Link to="/mercado">Mirá el mercado</Link> para comprar.
              </p>
            ) : (
              <section className={styles.grid} aria-label="Mis jugadores">
                {portfolio.data.tenencias.map((tenencia) => (
                  <CartaTenencia
                    key={tenencia.jugadorId}
                    tenencia={tenencia}
                    jugador={jugadoresPorId.get(tenencia.jugadorId)}
                    costoPromedio={costoPromedioDeCompra(
                      movimientos.data ?? [],
                      tenencia.jugadorId,
                    )}
                  />
                ))}
              </section>
            )}

            {misOfertas.length > 0 && (
              <section
                className={styles.enVenta}
                aria-labelledby="en-venta-titulo"
              >
                <h2 id="en-venta-titulo" className={styles.subtitulo}>
                  En venta en P2P
                </h2>
                <p className={styles.enVentaHint}>
                  Estos tokens están reservados en tus ofertas abiertas: si se
                  venden cobrás el precio publicado; si la oferta vence o la
                  cancelás, vuelven a tu cartera.
                </p>
                {cancelar.isError && (
                  <p role="alert" className={styles.error}>
                    {cancelar.error instanceof ApiError
                      ? cancelar.error.message
                      : 'No se pudo cancelar la oferta.'}
                  </p>
                )}
                <ul className={styles.ofertas}>
                  {misOfertas.map((oferta) => (
                    <FilaOfertaPropia
                      key={oferta.id}
                      oferta={oferta}
                      jugador={jugadoresPorId.get(oferta.jugadorId)}
                      onCancelar={() => cancelar.mutate(oferta.id)}
                      cancelando={
                        cancelar.isPending && cancelar.variables === oferta.id
                      }
                    />
                  ))}
                </ul>
              </section>
            )}

            {misSubastas.length > 0 && (
              <section
                className={styles.enVenta}
                aria-labelledby="subastas-titulo"
              >
                <h2 id="subastas-titulo" className={styles.subtitulo}>
                  Subastas en curso
                </h2>
                <p className={styles.enVentaHint}>
                  Las que creaste tienen tus tokens reservados; en las que vas
                  ganando, tu saldo queda reservado hasta que se adjudiquen o
                  alguien te supere.
                </p>
                <ul className={styles.ofertas}>
                  {misSubastas.map((subasta) => (
                    <FilaSubasta
                      key={subasta.id}
                      subasta={subasta}
                      jugador={jugadoresPorId.get(subasta.jugadorId)}
                      esVendedor={subasta.vendedorId === user?.usuarioId}
                    />
                  ))}
                </ul>
              </section>
            )}
          </>
        )}
      </main>
    </div>
  )
}

/** Foto, nombre y club del jugador de una fila de la cartera, enlazando a `to`. */
function JugadorEnFila({
  to,
  jugador,
}: {
  to: string
  jugador: JugadorResponse | undefined
}) {
  return (
    <Link to={to} className={styles.ofertaJugador}>
      <Avatar src={jugador?.fotoUrl ?? null} nombre={jugador?.nombre ?? '?'} />
      <span>
        <span className={styles.ofertaNombre}>
          {jugador?.nombre ?? 'Jugador no disponible'}
        </span>
        <span className={styles.ofertaClub}>{jugador?.club}</span>
      </span>
    </Link>
  )
}

function FilaSubasta({
  subasta,
  jugador,
  esVendedor,
}: {
  subasta: SubastaResponse
  jugador: JugadorResponse | undefined
  esVendedor: boolean
}) {
  const precio = subasta.precioActual ?? subasta.precioInicial
  return (
    <li className={styles.oferta}>
      <JugadorEnFila to={`/jugadores/${subasta.jugadorId}`} jugador={jugador} />
      <span className={styles.ofertaDato}>
        {esVendedor ? 'Vendés' : 'Vas ganando'} ·{' '}
        <strong>{subasta.cantidad}</strong> token(s)
      </span>
      <span className={styles.ofertaPrecio}>{formatMoney(precio)} / token</span>
      <span className={styles.ofertaVence}>
        {subasta.cantidadPujas} {subasta.cantidadPujas === 1 ? 'puja' : 'pujas'}
      </span>
      <Link to={`/subastas/${subasta.id}`} className={styles.verSubasta}>
        Ver subasta
      </Link>
    </li>
  )
}

function FilaOfertaPropia({
  oferta,
  jugador,
  onCancelar,
  cancelando,
}: {
  oferta: OfertaResponse
  jugador: JugadorResponse | undefined
  onCancelar: () => void
  cancelando: boolean
}) {
  return (
    <li className={styles.oferta}>
      <JugadorEnFila
        to={`/jugadores/${oferta.jugadorId}/p2p`}
        jugador={jugador}
      />
      <span className={styles.ofertaDato}>
        <strong>{oferta.cantidadDisponible}</strong> /{' '}
        {oferta.cantidadPublicada} token(s)
      </span>
      <span className={styles.ofertaPrecio}>
        {formatMoney(oferta.precioUnitario)} / token
      </span>
      <span className={styles.ofertaVence}>
        Vence {formatoFechaHora.format(new Date(oferta.fechaVencimiento))}
      </span>
      <button
        type="button"
        className={styles.btnCancelar}
        onClick={onCancelar}
        disabled={cancelando}
      >
        {cancelando ? 'Cancelando…' : 'Cancelar'}
      </button>
    </li>
  )
}

function CartaTenencia({
  tenencia,
  jugador,
  costoPromedio,
}: {
  tenencia: TenenciaPortfolio
  jugador: JugadorResponse | undefined
  costoPromedio: number | null
}) {
  return (
    <Link to={`/jugadores/${tenencia.jugadorId}`} className={styles.card}>
      <div className={styles.cardPhoto}>
        <Avatar
          src={jugador?.fotoUrl ?? null}
          nombre={jugador?.nombre ?? '?'}
          size="fill"
        />
      </div>
      <div className={styles.cardBody}>
        <div>
          <div className={styles.cardName}>
            {jugador?.nombre ?? 'Jugador no disponible'}
          </div>
          <div className={styles.cardClub}>
            {jugador?.club} {jugador?.posicion && `· ${jugador.posicion}`}
          </div>
        </div>
        <div className={styles.cardPrice}>
          <strong>
            {tenencia.cotizacionVigente
              ? formatMoney(tenencia.cotizacionVigente)
              : 'Sin cotización'}
          </strong>
        </div>
        <div className={styles.tenencia}>
          <span>
            Tenés <strong>{tenencia.cantidad}</strong> token(s)
          </span>
          <strong>{formatMoney(tenencia.valor)}</strong>
        </div>
        <div className={styles.costoPromedio}>
          <span>Costo promedio de compra</span>
          <span>
            {costoPromedio !== null ? formatMoney(costoPromedio) : '—'}
          </span>
        </div>
        <span className={styles.cta}>Comprar / vender</span>
      </div>
    </Link>
  )
}
