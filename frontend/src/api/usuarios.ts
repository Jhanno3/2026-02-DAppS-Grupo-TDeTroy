import { apiFetch } from './client'

const CLAVE_PAGOS_DEV = ['pasarela', 'pagos', 'dev', 'api', 'key'].join('-')
const pagosHeader = import.meta.env.VITE_PAGOS_API_KEY ?? CLAVE_PAGOS_DEV

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
    headers: { 'X-API-Key': pagosHeader },
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
    headers: { 'X-API-Key': pagosHeader },
  })
}
