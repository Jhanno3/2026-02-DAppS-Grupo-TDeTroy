import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { obtenerPortfolio } from '../../api/portfolio'
import { listarJugadores } from '../../api/jugadores'
import { useAuth } from '../auth/AuthContext'
import { Avatar } from '../../shared/components/Avatar'
import { formatMoney } from '../../shared/format'
import styles from './CarteraPage.module.css'

/**
 * UC-11: cartera del usuario autenticado. `GET /portfolios/me` sólo trae `jugadorId` por tenencia
 * (plan.md §2.5: sin entidad `Portfolio` propia) — se cruza con el catálogo público (UC-07) acá
 * para mostrar nombre/foto/club, en vez de pedirle eso al backend.
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

  const isLoading = portfolio.isLoading || jugadores.isLoading
  const isError = portfolio.isError || jugadores.isError

  const jugadoresPorId = new Map((jugadores.data ?? []).map((j) => [j.id, j]))

  return (
    <div className={styles.container}>
      <h1 className={styles.title}>Mi cartera</h1>

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
            <div className={styles.tableScroll}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Jugador</th>
                    <th className={styles.num}>Cantidad</th>
                    <th className={styles.num}>Cotización vigente</th>
                    <th className={styles.num}>Valor</th>
                    <th>
                      <span className={styles.srOnly}>Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {portfolio.data.tenencias.map((tenencia) => {
                    const jugador = jugadoresPorId.get(tenencia.jugadorId)
                    return (
                      <tr key={tenencia.jugadorId}>
                        <td>
                          <div className={styles.jugador}>
                            <Avatar
                              src={jugador?.fotoUrl ?? null}
                              nombre={jugador?.nombre ?? '?'}
                            />
                            <div>
                              <b>{jugador?.nombre ?? 'Jugador no disponible'}</b>
                              <small>{jugador?.club}</small>
                            </div>
                          </div>
                        </td>
                        <td className={styles.num}>{tenencia.cantidad}</td>
                        <td className={styles.num}>
                          {tenencia.cotizacionVigente
                            ? formatMoney(tenencia.cotizacionVigente)
                            : '—'}
                        </td>
                        <td className={styles.num}>
                          {formatMoney(tenencia.valor)}
                        </td>
                        <td className={styles.accion}>
                          <Link to={`/jugadores/${tenencia.jugadorId}`}>
                            Comprar / vender
                          </Link>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  )
}
