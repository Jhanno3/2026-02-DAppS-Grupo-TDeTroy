export interface JwtClaims {
  sub: string
  usuarioId: string
  rol: 'USER' | 'ADMIN'
  iat: number
  exp: number
}

/**
 * Decodifica el payload de un JWT sin verificar su firma — sólo para leer datos en el cliente
 * (ej. mostrar el email/rol del usuario logueado). La firma la valida siempre el backend en cada
 * request; nunca hay que confiar en este decode para tomar una decisión de seguridad real.
 */
export function decodeJwt(token: string): JwtClaims {
  const payload = token.split('.')[1]
  if (!payload) {
    throw new Error('Token con formato inválido')
  }
  const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
  const json = atob(base64)
  return JSON.parse(json) as JwtClaims
}
