import type { JugadorResponse } from '../api/jugadores'
import type { TenenciaPortfolio } from '../api/portfolio'

export interface JugadorConTenencia {
  tenencia: TenenciaPortfolio
  jugador: JugadorResponse
}

/**
 * Cruza las tenencias con tokens disponibles con el catálogo, descartando las de jugadores que el
 * catálogo todavía no trajo.
 */
export function jugadoresConTokensDe(
  tenencias: TenenciaPortfolio[] | undefined,
  jugadorPorId: Map<string, JugadorResponse>,
): JugadorConTenencia[] {
  return (tenencias ?? []).flatMap((tenencia) => {
    const jugador = jugadorPorId.get(tenencia.jugadorId)
    return tenencia.cantidad > 0 && jugador ? [{ tenencia, jugador }] : []
  })
}
