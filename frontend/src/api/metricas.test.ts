import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from './client'
import {
  actualizarConfiguracionMetricas,
  obtenerConfiguracionMetricas,
  type ConfiguracionMetricaResponse,
} from './metricas'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof client>()
  return { ...actual, apiFetch: vi.fn() }
})

describe('api/metricas', () => {
  const mockConfig: ConfiguracionMetricaResponse[] = [
    {
      clave: 'goals',
      etiqueta: 'Goles',
      sentido: 'POSITIVO',
      activo: true,
      pesoGeneral: 0.5,
      pesosPorPosicion: { Delantero: 0.6, Defensor: 1.0 },
    },
  ]

  beforeEach(() => {
    vi.mocked(client.apiFetch).mockResolvedValue(mockConfig)
  })

  it('obtenerConfiguracionMetricas hace GET /configuracion/metricas con el token', async () => {
    const res = await obtenerConfiguracionMetricas('admin-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/configuracion/metricas', {
      method: 'GET',
      token: 'admin-token',
    })
    expect(res).toEqual(mockConfig)
  })

  it('actualizarConfiguracionMetricas hace PUT /configuracion/metricas con body y token', async () => {
    const request = {
      metricas: [
        {
          clave: 'goals',
          pesoGeneral: 0.8,
          activo: true,
          pesosPorPosicion: { Delantero: 0.9 },
        },
      ],
    }
    const res = await actualizarConfiguracionMetricas(request, 'admin-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/configuracion/metricas', {
      method: 'PUT',
      token: 'admin-token',
      body: request,
    })
    expect(res).toEqual(mockConfig)
  })
})
