import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { listarMisMovimientos } from '../../api/movimientos'
import { listarJugadores } from '../../api/jugadores'
import { useAuth } from '../auth/AuthContext'
import { formatMoney } from '../../shared/format'
import styles from './MovimientosPage.module.css'

const ETIQUETAS_TIPO: Record<string, string> = {
  COMPRA_SISTEMA: 'Compra',
  VENTA_SISTEMA: 'Venta',
  COMPRA_P2P: 'Compra P2P',
  VENTA_P2P: 'Venta P2P',
  RECARGA_SALDO: 'Recarga de saldo',
  COMPENSACION_BAJA: 'Compensación por baja',
}

const formatoFecha = new Intl.DateTimeFormat('es-AR', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

/**
 * UC-12: historial cronológico propio del usuario — compras/ventas contra el sistema, operaciones
 * P2P y compensaciones por baja, cada una identificada por su tipo (spec.md UC-12: "identificada
 * como tal, no como una venta/compra ordinaria"). `GET /movimientos/me` no trae el nombre del
 * jugador (sólo `jugadorId`) — se cruza con el catálogo público (UC-07) acá, mismo criterio que
 * {@code CarteraPage}.
 */
export function MovimientosPage() {
  const { token } = useAuth()

  const movimientos = useQuery({
    queryKey: ['movimientos', 'me'],
    queryFn: () => listarMisMovimientos(token!),
    enabled: Boolean(token),
  })

  const jugadores = useQuery({
    queryKey: ['jugadores'],
    queryFn: listarJugadores,
  })

  const nombresPorJugadorId = new Map(
    (jugadores.data ?? []).map((j) => [j.id, j.nombre]),
  )

  const isLoading = movimientos.isLoading || jugadores.isLoading
  const isError = movimientos.isError

  return (
    <div className={styles.container}>
      <Link to="/cartera" className={styles.back}>
        ← Volver a mi cartera
      </Link>
      <h1 className={styles.title}>Historial de movimientos</h1>

      {isLoading && <p>Cargando…</p>}
      {isError && (
        <p role="alert">No se pudo cargar el historial. Intentá de nuevo.</p>
      )}

      {movimientos.data &&
        (movimientos.data.length === 0 ? (
          <p className={styles.empty}>Todavía no tenés movimientos.</p>
        ) : (
          <div className={styles.tableScroll}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Tipo</th>
                  <th>Jugador</th>
                  <th className={styles.num}>Cantidad</th>
                  <th className={styles.num}>Monto</th>
                </tr>
              </thead>
              <tbody>
                {[...movimientos.data].reverse().map((movimiento) => (
                  <tr key={movimiento.id}>
                    <td>{formatoFecha.format(new Date(movimiento.fecha))}</td>
                    <td>
                      <span className={styles.badge}>
                        {ETIQUETAS_TIPO[movimiento.tipo] ?? movimiento.tipo}
                      </span>
                    </td>
                    <td>
                      {movimiento.jugadorId
                        ? (nombresPorJugadorId.get(movimiento.jugadorId) ?? '—')
                        : '—'}
                    </td>
                    <td className={styles.num}>{movimiento.cantidad ?? '—'}</td>
                    <td className={styles.num}>
                      {formatMoney(movimiento.montoTotal)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
    </div>
  )
}
