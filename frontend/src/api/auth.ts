import { apiFetch } from './client'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
}

export interface RegistrarUsuarioRequest {
  email: string
  password: string
}

export interface UsuarioResponse {
  id: string
  email: string
  rol: 'USER' | 'ADMIN'
  saldoVirtual: string
  fechaCreacion: string
}

/** {@code POST /auth/login} (UC-02). */
export function login(request: LoginRequest): Promise<LoginResponse> {
  return apiFetch<LoginResponse>('/auth/login', {
    method: 'POST',
    body: request,
  })
}

/** {@code POST /auth/registro} (UC-01). */
export function registrar(
  request: RegistrarUsuarioRequest,
): Promise<UsuarioResponse> {
  return apiFetch<UsuarioResponse>('/auth/registro', {
    method: 'POST',
    body: request,
  })
}
