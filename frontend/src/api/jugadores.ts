import { apiFetch } from './client'

export interface JugadorResponse {
  id: string
  nombre: string
  club: string
  posicion: string
  fechaNacimiento: string
  nacionalidad: string
  estado: 'ACTIVO' | 'INACTIVO'
  tokensDisponibles: number
  fechaUltimaActualizacionRendimiento: string | null
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

export interface DatosIdentificatoriosJugador {
  nombre: string
  club: string
  posicion: string
  fechaNacimiento: string
  nacionalidad: string
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
