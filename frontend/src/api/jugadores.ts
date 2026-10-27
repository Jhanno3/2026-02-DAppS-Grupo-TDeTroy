import { apiFetch } from './client'

export interface JugadorResponse {
  id: string
  nombre: string
  club: string
  posicion: string
  fechaNacimiento: string
  nacionalidad: string
  liga: string
  estado: 'ACTIVO' | 'INACTIVO'
  tokensDisponibles: number
  fechaUltimaActualizacionRendimiento: string | null
  fotoUrl: string | null
}

/**
 * UC-07: catálogo público, incluidos los jugadores {@code INACTIVO} (spec.md UC-07). Deliberadamente
 * sin cotización vigente todavía — {@code CotizacionService} (backend Fase 4) no existe; ver
 * javadoc de {@code JugadorResponse} en el backend.
 */
export function listarJugadores(): Promise<JugadorResponse[]> {
  return apiFetch<JugadorResponse[]>('/jugadores')
}

/** UC-07: detalle de un jugador puntual. */
export function obtenerJugador(id: string): Promise<JugadorResponse> {
  return apiFetch<JugadorResponse>(`/jugadores/${id}`)
}

export type OrdenMercado =
  'nombre' | 'precio-desc' | 'precio-asc' | 'suba' | 'disponibles'

export interface JugadorConPrecioResponse {
  jugador: JugadorResponse
  precio: string | null
  variacionPorcentual: string | null
}

export interface PaginaMercadoResponse {
  contenido: JugadorConPrecioResponse[]
  pagina: number
  tamanioPagina: number
  totalElementos: number
  totalPaginas: number
  posicionesDisponibles: string[]
}

export interface ParametrosMercado {
  pagina: number
  tamanio?: number
  orden: OrdenMercado
  posicion?: string
  busqueda?: string
}

/**
 * UC-07: catálogo paginado con cotización vigente embebida, ya filtrado/ordenado en el backend —
 * reemplaza a {@link listarJugadores} + N llamadas a {@link listarCotizaciones} en la pantalla de
 * Mercado (ese patrón tardaba segundos con ~2600 jugadores, uno por cada request de cotización).
 */
export function listarMercado(
  params: ParametrosMercado,
): Promise<PaginaMercadoResponse> {
  const query = new URLSearchParams({
    pagina: String(params.pagina),
    tamanio: String(params.tamanio ?? 50),
    orden: params.orden,
  })
  if (params.posicion && params.posicion !== 'Todos') {
    query.set('posicion', params.posicion)
  }
  if (params.busqueda) {
    query.set('busqueda', params.busqueda)
  }
  return apiFetch<PaginaMercadoResponse>(
    `/jugadores/mercado?${query.toString()}`,
  )
}

export interface DatosIdentificatoriosJugador {
  nombre: string
  club: string
  posicion: string
  fechaNacimiento: string
  nacionalidad: string
  liga: string
}

/** UC-03 (ADMIN). */
export function crearJugador(
  request: DatosIdentificatoriosJugador,
  token: string,
): Promise<JugadorResponse> {
  return apiFetch<JugadorResponse>('/jugadores', {
    method: 'POST',
    body: request,
    token,
  })
}

/** UC-04 (ADMIN). Nunca toca estado ni tokens — eso lo garantiza el backend. */
export function editarJugador(
  id: string,
  request: DatosIdentificatoriosJugador,
  token: string,
): Promise<JugadorResponse> {
  return apiFetch<JugadorResponse>(`/jugadores/${id}`, {
    method: 'PUT',
    body: request,
    token,
  })
}

/** UC-15 (ADMIN). Inactivación + compensación de cada usuario afectado, no es un `DELETE` físico. */
export function darDeBajaJugador(
  id: string,
  token: string,
): Promise<JugadorResponse> {
  return apiFetch<JugadorResponse>(`/jugadores/${id}/baja`, {
    method: 'POST',
    token,
  })
}

export interface CotizacionResponse {
  id: string
  semana: string
  valor: string
  origen: 'AUTOMATICO' | 'MANUAL'
  fechaCalculo: string
}

/**
 * UC-08: historial cronológico de cotización de un jugador, público (sin cuenta). Un jugador sin
 * cálculos todavía devuelve `[]` — no es un error (spec.md UC-08).
 */
export function listarCotizaciones(id: string): Promise<CotizacionResponse[]> {
  return apiFetch<CotizacionResponse[]>(`/jugadores/${id}/cotizaciones`)
}
