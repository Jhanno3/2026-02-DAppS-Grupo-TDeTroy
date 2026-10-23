import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from './client'
import { dispararRecalculoManual } from './cotizaciones'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof client>()
  return { ...actual, apiFetch: vi.fn() }
})

describe('api/cotizaciones', () => {
  beforeEach(() => {
    vi.mocked(client.apiFetch).mockResolvedValue(undefined)
  })

  it('dispararRecalculoManual hace POST /cotizaciones/recalculo con el token (UC-13)', async () => {
    await dispararRecalculoManual('admin-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/cotizaciones/recalculo', {
      method: 'POST',
      token: 'admin-token',
    })
  })
})
