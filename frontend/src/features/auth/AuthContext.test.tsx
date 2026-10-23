import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from './AuthContext'
import { apiFetch } from '../../api/client'

function token(claims: {
  sub: string
  usuarioId: string
  rol: 'USER' | 'ADMIN'
  exp: number
}): string {
  const base64url = btoa(JSON.stringify({ ...claims, iat: 0 }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
  return `header.${base64url}.firma`
}

const unaHoraEnElFuturo = Math.floor(Date.now() / 1000) + 3600
const unaHoraEnElPasado = Math.floor(Date.now() / 1000) - 3600
const wrapper = ({ children }: { children: ReactNode }) => (
  <AuthProvider>{children}</AuthProvider>
)

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('sin token guardado, arranca sin sesión', () => {
    const { result } = renderHook(() => useAuth(), { wrapper })

    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.user).toBeNull()
  })

  it('login guarda el token en localStorage y decodifica el usuario', () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    const valido = token({
      sub: 'user@example.com',
      usuarioId: 'u1',
      rol: 'USER',
      exp: unaHoraEnElFuturo,
    })

    act(() => result.current.login(valido))

    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.user).toEqual({
      usuarioId: 'u1',
      email: 'user@example.com',
      rol: 'USER',
    })
    expect(localStorage.getItem('valuacion.token')).toBe(valido)
  })

  it('logout limpia el token y la sesión', () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    act(() =>
      result.current.login(
        token({
          sub: 'user@example.com',
          usuarioId: 'u1',
          rol: 'USER',
          exp: unaHoraEnElFuturo,
        }),
      ),
    )

    act(() => result.current.logout())

    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.user).toBeNull()
    expect(localStorage.getItem('valuacion.token')).toBeNull()
  })

  it('un token vencido no autentica, aunque esté guardado', () => {
    const { result } = renderHook(() => useAuth(), { wrapper })

    act(() =>
      result.current.login(
        token({
          sub: 'user@example.com',
          usuarioId: 'u1',
          rol: 'USER',
          exp: unaHoraEnElPasado,
        }),
      ),
    )

    expect(result.current.isAuthenticated).toBe(false)
  })

  it('un token corrupto en localStorage no rompe el arranque, sólo deja sin sesión', () => {
    localStorage.setItem('valuacion.token', 'esto-no-es-un-jwt')

    const { result } = renderHook(() => useAuth(), { wrapper })

    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.user).toBeNull()
  })

  it('un 401 de cualquier request autenticada cierra la sesión local automáticamente', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    act(() =>
      result.current.login(
        token({
          sub: 'user@example.com',
          usuarioId: 'u1',
          rol: 'USER',
          exp: unaHoraEnElFuturo,
        }),
      ),
    )
    expect(result.current.isAuthenticated).toBe(true)

    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          new Response(
            JSON.stringify({ detail: 'Se requiere un token válido...' }),
            { status: 401 },
          ),
        ),
    )

    await act(async () => {
      await apiFetch('/jugadores/1/ventas', {
        method: 'POST',
        token: result.current.token,
      }).catch(() => {})
    })

    expect(result.current.isAuthenticated).toBe(false)
    expect(localStorage.getItem('valuacion.token')).toBeNull()

    vi.unstubAllGlobals()
  })

  it('usar useAuth fuera de AuthProvider lanza un error claro', () => {
    expect(() => renderHook(() => useAuth())).toThrow(
      'useAuth debe usarse dentro de <AuthProvider>',
    )
  })
})
