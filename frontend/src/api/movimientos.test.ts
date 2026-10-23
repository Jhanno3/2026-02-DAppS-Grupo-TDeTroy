import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from './client'
import { listarMisMovimientos } from './movimientos'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof client>()
  return { ...actual, apiFetch: vi.fn() }
})

describe('api/movimientos', () => {
  beforeEach(() => {
    vi.mocked(client.apiFetch).mockResolvedValue(undefined)
  })

  it('listarMisMovimientos pide GET /movimientos/me con el token (UC-12)', async () => {
    await listarMisMovimientos('user-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/movimientos/me', {
      token: 'user-token',
    })
  })
})
