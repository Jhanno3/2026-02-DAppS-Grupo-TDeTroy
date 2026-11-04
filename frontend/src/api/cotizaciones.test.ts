import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from './client'
import {
  dispararIngestaFixtures,
  dispararIngestaRendimiento,
  dispararRecalculoManual,
} from './cotizaciones'

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

  it('dispararIngestaRendimiento hace POST /cotizaciones/ingesta-rendimiento con el token', async () => {
    await dispararIngestaRendimiento('admin-token')

    expect(client.apiFetch).toHaveBeenCalledWith(
      '/cotizaciones/ingesta-rendimiento',
      {
        method: 'POST',
        token: 'admin-token',
      },
    )
  })

  it('dispararIngestaFixtures hace POST /cotizaciones/ingesta-fixtures con el token', async () => {
    await dispararIngestaFixtures('admin-token')

    expect(client.apiFetch).toHaveBeenCalledWith(
      '/cotizaciones/ingesta-fixtures',
      {
        method: 'POST',
        token: 'admin-token',
      },
    )
  })
})
