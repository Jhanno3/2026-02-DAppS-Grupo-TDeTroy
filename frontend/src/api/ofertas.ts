import { apiFetch } from './client'
import type { MovimientoResponse } from './tokens'

export interface OfertaResponse {
  id: string
  vendedorId: string
  jugadorId: string
  cantidadPublicada: number
  cantidadDisponible: number
  precioUnitario: string
  estado: 'ABIERTA' | 'AGOTADA' | 'CANCELADA'
  fechaCreacion: string
  fechaVencimiento: string
}

/**
 * UC-17: ofertas abiertas, públicas (para que un comprador elija). Sin `jugadorId`, trae las de
 * todo el catálogo (pestaña "P2P"); con `jugadorId`, sólo las de ese jugador (página de detalle).
 */
export function listarOfertas(jugadorId?: string): Promise<OfertaResponse[]> {
  return apiFetch<OfertaResponse[]>(
    jugadorId ? `/ofertas?jugadorId=${jugadorId}` : '/ofertas',
  )
}

/**
 * UC-17: el vendedor publica una cantidad de tokens que posee de un jugador, a un precio por
 * token y un tiempo límite en horas que él mismo fija libremente.
 */
export function publicarOferta(
  jugadorId: string,
  cantidad: number,
  precioUnitario: number,
  horasLimite: number,
  token: string,
): Promise<OfertaResponse> {
  return apiFetch<OfertaResponse>('/ofertas', {
    method: 'POST',
    body: { jugadorId, cantidad, precioUnitario, horasLimite },
    token,
  })
}

/** UC-17: un comprador ejecuta una compra contra la oferta, al precio publicado por el vendedor. */
export function comprarDeOferta(
  ofertaId: string,
  cantidad: number,
  token: string,
): Promise<MovimientoResponse> {
  return apiFetch<MovimientoResponse>(`/ofertas/${ofertaId}/compras`, {
    method: 'POST',
    body: { cantidad },
    token,
  })
}

/** UC-17: el vendedor cancela lo no vendido de su propia oferta. */
export function cancelarOferta(ofertaId: string, token: string): Promise<void> {
  return apiFetch<void>(`/ofertas/${ofertaId}`, { method: 'DELETE', token })
}
