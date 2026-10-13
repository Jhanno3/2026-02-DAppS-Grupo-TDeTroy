import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, ApiError } from './client'

describe('apiFetch', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('en una respuesta OK, devuelve el JSON parseado', async () => {
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ id: '1' }), { status: 200 }),
    )

    const result = await apiFetch<{ id: string }>('/jugadores/1')

    expect(result).toEqual({ id: '1' })
  })

  it('agrega el header Authorization cuando se pasa un token', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('{}', { status: 200 }))

    await apiFetch('/portfolios/me', { token: 'abc123' })

    const [, options] = vi.mocked(fetch).mock.calls[0]
    expect((options?.headers as Record<string, string>).Authorization).toBe(
      'Bearer abc123',
    )
  })

  it('en 204 No Content, devuelve undefined en vez de intentar parsear JSON', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 204 }))

    const result = await apiFetch('/ofertas/1')

    expect(result).toBeUndefined()
  })

  it('en un error con problem+json, lanza ApiError con el detail del backend', async () => {
    vi.mocked(fetch).mockResolvedValue(
      new Response(
        JSON.stringify({
          title: 'Saldo insuficiente',
          detail: 'No te alcanza el saldo',
        }),
        { status: 409 },
      ),
    )

    await expect(apiFetch('/jugadores/1/compras')).rejects.toMatchObject({
      status: 409,
      message: 'No te alcanza el saldo',
      title: 'Saldo insuficiente',
    })
  })

  it('en un error sin cuerpo parseable, usa el mensaje genérico', async () => {
    vi.mocked(fetch).mockResolvedValue(
      new Response('no es json', { status: 500 }),
    )

    await expect(apiFetch('/jugadores')).rejects.toThrow(
      'Ocurrió un error inesperado. Intentá de nuevo en unos minutos.',
    )
  })

  it('ApiError es instancia de Error con status y title', () => {
    const error = new ApiError(404, 'No encontrado', 'Not Found')

    expect(error).toBeInstanceOf(Error)
    expect(error.name).toBe('ApiError')
    expect(error.status).toBe(404)
    expect(error.title).toBe('Not Found')
  })
})
