import { describe, expect, it } from 'vitest'
import { decodeJwt } from './jwt'

function tokenConPayload(payload: object): string {
  const base64url = btoa(JSON.stringify(payload))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
  return `header.${base64url}.firma`
}

describe('decodeJwt', () => {
  it('decodifica los claims del payload (base64url, con - y _)', () => {
    const claims = {
      sub: 'user@example.com',
      usuarioId: 'u1',
      rol: 'USER' as const,
      iat: 1000,
      exp: 2000,
    }

    expect(decodeJwt(tokenConPayload(claims))).toEqual(claims)
  })

  it('sin un segundo segmento (sin punto), lanza un error', () => {
    expect(() => decodeJwt('tokeninvalido')).toThrow(
      'Token con formato inválido',
    )
  })

  it('con el payload vacío entre puntos, lanza un error', () => {
    expect(() => decodeJwt('header..firma')).toThrow(
      'Token con formato inválido',
    )
  })
})
