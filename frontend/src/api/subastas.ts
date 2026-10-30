import { apiFetch } from './client'

export type EstadoSubasta = 'ACTIVA' | 'ADJUDICADA' | 'DESIERTA' | 'CANCELADA'

/** Incremento admitido para pujar: 0 (igualar el precio inicial, sólo como 1ª puja), 5, 10 o 20%. */
export interface OpcionPuja {
  porcentaje: number
  precioUnitario: string
  montoTotal: string
}

export interface PujaResponse {
  postorId: string
  precioUnitario: string
  fecha: string
}

export interface SubastaResponse {
  id: string
  vendedorId: string
  jugadorId: string
  cantidad: number
  precioInicial: string
  /** `null` hasta la primera puja. */
  precioActual: string | null
  mejorPostorId: string | null
  cantidadPujas: number
  estado: EstadoSubasta
  fechaCreacion: string
  fechaVencimiento: string
  /** Cuándo deja de aceptar pujas: última puja + 2 min si ya hubo pujas, fin de la duración si no. */
  cierraEn: string
  /** Precios exactos de cada incremento admitido ahora mismo; vacío si no está activa. */
  opcionesPuja: OpcionPuja[]
  /** Sólo en el detalle (`null` en el listado), la más reciente primero. */
  pujas: PujaResponse[] | null
}

/** UC-18: subastas activas, de todo el catálogo o de un jugador. Público. */
export function listarSubastas(jugadorId?: string): Promise<SubastaResponse[]> {
  return apiFetch<SubastaResponse[]>(
    jugadorId ? `/subastas?jugadorId=${jugadorId}` : '/subastas',
  )
}

/** UC-18: detalle de una subasta (en cualquier estado) con su historial de pujas. Público. */
export function obtenerSubasta(id: string): Promise<SubastaResponse> {
  return apiFetch<SubastaResponse>(`/subastas/${id}`)
}

export interface CrearSubastaRequest {
  jugadorId: string
  cantidad: number
  precioInicial: number
  duracionMinutos: number
}

/** UC-18: el vendedor subasta tokens que posee; quedan reservados mientras esté activa. */
export function crearSubasta(
  request: CrearSubastaRequest,
  token: string,
): Promise<SubastaResponse> {
  return apiFetch<SubastaResponse>('/subastas', {
    method: 'POST',
    body: request,
    token,
  })
}

/** UC-18: puja subiendo `porcentaje` sobre el precio vigente; el backend calcula el precio. */
export function pujar(
  subastaId: string,
  porcentaje: number,
  token: string,
): Promise<SubastaResponse> {
  return apiFetch<SubastaResponse>(`/subastas/${subastaId}/pujas`, {
    method: 'POST',
    body: { porcentaje },
    token,
  })
}

/** UC-18: el vendedor cancela su subasta activa. */
export function cancelarSubasta(
  subastaId: string,
  token: string,
): Promise<void> {
  return apiFetch<void>(`/subastas/${subastaId}`, { method: 'DELETE', token })
}
