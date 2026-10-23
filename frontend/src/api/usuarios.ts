import { apiFetch } from './client'

export interface UsuarioResponse {
  id: string
  email: string
  rol: 'USER' | 'ADMIN'
  saldoVirtual: string
  fechaCreacion: string
}

/** UC-14 (ADMIN): acredita saldo virtual en la cuenta de un usuario existente. */
export function recargarSaldo(
  usuarioId: string,
  monto: number,
  token: string,
): Promise<UsuarioResponse> {
  return apiFetch<UsuarioResponse>(`/usuarios/${usuarioId}/saldo`, {
    method: 'POST',
    body: { monto },
    token,
  })
}
