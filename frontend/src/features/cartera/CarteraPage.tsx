import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { obtenerPortfolio, type TenenciaPortfolio } from '../../api/portfolio'
import { listarJugadores, type JugadorResponse } from '../../api/jugadores'
import {
  listarMisMovimientos,
  type MovimientoResponse,
} from '../../api/movimientos'
import { useAuth } from '../auth/AuthContext'
import { Avatar } from '../../shared/components/Avatar'
import { formatMoney } from '../../shared/format'
import styles from './CarteraPage.module.css'

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
      (m.tipo === 'COMPRA_SISTEMA' || m.tipo === 'COMPRA_P2P'),
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
 */
export function CarteraPage() {
  const { token } = useAuth()

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

  const isLoading =
    portfolio.isLoading || jugadores.isLoading || movimientos.isLoading
  const isError = portfolio.isError || jugadores.isError

  const jugadoresPorId = new Map((jugadores.data ?? []).map((j) => [j.id, j]))

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
                <dt>Jugadores distintos</dt>
                <dd>{portfolio.data.tenencias.length}</dd>
              </div>
            </dl>

            {portfolio.data.tenencias.length === 0 ? (
              <p className={styles.empty}>
                Todavía no tenés tokens.{' '}
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
          </>
        )}
      </main>
    </div>
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
