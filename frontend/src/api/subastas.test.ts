import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from './client'
import {
  cancelarSubasta,
  crearSubasta,
  listarSubastas,
  obtenerSubasta,
  pujar,
} from './subastas'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof client>()
  return { ...actual, apiFetch: vi.fn() }
})

describe('api/subastas', () => {
  beforeEach(() => {
    vi.mocked(client.apiFetch).mockResolvedValue(undefined)
  })

  it('listarSubastas sin jugadorId pide GET /subastas (UC-18, todas las activas)', async () => {
    await listarSubastas()

    expect(client.apiFetch).toHaveBeenCalledWith('/subastas')
  })

  it('listarSubastas con jugadorId pide GET /subastas?jugadorId={id}', async () => {
    await listarSubastas('jugador-1')

    expect(client.apiFetch).toHaveBeenCalledWith(
      '/subastas?jugadorId=jugador-1',
    )
  })

  it('obtenerSubasta pide GET /subastas/{id}', async () => {
    await obtenerSubasta('s1')

    expect(client.apiFetch).toHaveBeenCalledWith('/subastas/s1')
  })

  it('crearSubasta hace POST /subastas con cantidad, precio inicial, duración y token', async () => {
    const request = {
      jugadorId: 'jugador-1',
      cantidad: 2,
      precioInicial: 104.39,
      duracionMinutos: 30,
    }

    await crearSubasta(request, 'vendedor-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/subastas', {
      method: 'POST',
      body: request,
      token: 'vendedor-token',
    })
  })

  it('pujar hace POST /subastas/{id}/pujas sólo con el porcentaje (el precio lo calcula el backend)', async () => {
    await pujar('s1', 10, 'postor-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/subastas/s1/pujas', {
      method: 'POST',
      body: { porcentaje: 10 },
      token: 'postor-token',
    })
  })

  it('cancelarSubasta hace DELETE /subastas/{id}', async () => {
    await cancelarSubasta('s1', 'vendedor-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/subastas/s1', {
      method: 'DELETE',
      token: 'vendedor-token',
    })
  })
})
