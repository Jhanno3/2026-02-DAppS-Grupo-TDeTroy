import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from './client'
import { obtenerPortfolio } from './portfolio'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof client>()
  return { ...actual, apiFetch: vi.fn() }
})

describe('api/portfolio', () => {
  beforeEach(() => {
    vi.mocked(client.apiFetch).mockResolvedValue(undefined)
  })

  it('obtenerPortfolio pide GET /portfolios/me con el token (UC-11)', async () => {
    await obtenerPortfolio('user-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/portfolios/me', {
      token: 'user-token',
    })
  })
})
