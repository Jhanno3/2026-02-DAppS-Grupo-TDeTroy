import { apiFetch } from './client'
import type { MovimientoResponse } from './tokens'

export type { MovimientoResponse }

/**
 * UC-12: historial cronológico propio del usuario autenticado — compras/ventas contra el sistema,
 * operaciones P2P (identificadas por {@code contraparteUsuarioId}) y compensaciones por baja de
 * jugador, todas distinguibles por {@code tipo}.
 */
export function listarMisMovimientos(
  token: string,
): Promise<MovimientoResponse[]> {
  return apiFetch<MovimientoResponse[]>('/movimientos/me', { token })
}
