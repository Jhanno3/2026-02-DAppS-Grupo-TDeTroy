import { apiFetch } from './client'

export interface MovimientoResponse {
  id: string
  jugadorId: string
  tipo: string
  cantidad: number
  precioUnitario: string
  montoTotal: string
  contraparteUsuarioId: string | null
  fecha: string
}

/** UC-09: compra de tokens contra el sistema, a la cotización vigente del jugador. */
export function comprarTokens(
  jugadorId: string,
  cantidad: number,
  token: string,
): Promise<MovimientoResponse> {
  return apiFetch<MovimientoResponse>(`/jugadores/${jugadorId}/compras`, {
    method: 'POST',
    body: { cantidad },
    token,
  })
}

/** UC-10: venta de tokens contra el sistema (requiere tenencia disponible, no reservada). */
export function venderTokens(
  jugadorId: string,
  cantidad: number,
  token: string,
): Promise<MovimientoResponse> {
  return apiFetch<MovimientoResponse>(`/jugadores/${jugadorId}/ventas`, {
    method: 'POST',
    body: { cantidad },
    token,
  })
}
