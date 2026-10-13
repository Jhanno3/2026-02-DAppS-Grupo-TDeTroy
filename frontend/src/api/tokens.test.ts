import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from './client'
import { comprarTokens, venderTokens } from './tokens'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof client>()
  return { ...actual, apiFetch: vi.fn() }
})

describe('api/tokens', () => {
  beforeEach(() => {
    vi.mocked(client.apiFetch).mockResolvedValue(undefined)
  })

  it('comprarTokens hace POST /jugadores/{id}/compras con cantidad y token (UC-09)', async () => {
    await comprarTokens('abc-123', 5, 'user-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/jugadores/abc-123/compras', {
      method: 'POST',
      body: { cantidad: 5 },
      token: 'user-token',
    })
  })

  it('venderTokens hace POST /jugadores/{id}/ventas con cantidad y token (UC-10)', async () => {
    await venderTokens('abc-123', 2, 'user-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/jugadores/abc-123/ventas', {
      method: 'POST',
      body: { cantidad: 2 },
      token: 'user-token',
    })
  })
})
