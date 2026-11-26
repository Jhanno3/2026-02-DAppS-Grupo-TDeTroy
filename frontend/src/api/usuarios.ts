import { apiFetch } from './client'

const PAGOS_API_KEY =
  import.meta.env.VITE_PAGOS_API_KEY ?? 'pasarela-pagos-dev-api-key'

export interface UsuarioResponse {
  id: string
  email: string
  rol: 'USER' | 'ADMIN'
  saldoVirtual: string
  fechaCreacion: string
}

export interface CargarSaldoRequest {
  monto: number
  numeroTarjeta: string
  titular: string
  vencimiento: string
  codigoSeguridad: string
}

export function cargarSaldoPropio(
  datos: CargarSaldoRequest,
  token: string,
): Promise<UsuarioResponse> {
  return apiFetch<UsuarioResponse>('/usuarios/me/saldo', {
    method: 'POST',
    body: datos,
    token,
    headers: { 'X-API-Key': PAGOS_API_KEY },
  })
}

export function recargarSaldo(
  usuarioId: string,
  monto: number,
  token: string,
): Promise<UsuarioResponse> {
  return apiFetch<UsuarioResponse>(`/usuarios/${usuarioId}/saldo`, {
    method: 'POST',
    body: { monto },
    token,
    headers: { 'X-API-Key': PAGOS_API_KEY },
  })
}
