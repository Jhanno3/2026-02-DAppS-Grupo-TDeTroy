import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from './client'
import {
  cancelarOferta,
  comprarDeOferta,
  listarOfertas,
  publicarOferta,
} from './ofertas'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof client>()
  return { ...actual, apiFetch: vi.fn() }
})

describe('api/ofertas', () => {
  beforeEach(() => {
    vi.mocked(client.apiFetch).mockResolvedValue(undefined)
  })

  it('listarOfertas sin jugadorId pide GET /ofertas (UC-17, catálogo completo)', async () => {
    await listarOfertas()

    expect(client.apiFetch).toHaveBeenCalledWith('/ofertas')
  })

  it('listarOfertas con jugadorId pide GET /ofertas?jugadorId={id} (UC-17)', async () => {
    await listarOfertas('jugador-1')

    expect(client.apiFetch).toHaveBeenCalledWith('/ofertas?jugadorId=jugador-1')
  })

  it('publicarOferta hace POST /ofertas con jugadorId, cantidad, precioUnitario, horasLimite y token (UC-17)', async () => {
    await publicarOferta('jugador-1', 6, 50, 24, 'vendedor-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/ofertas', {
      method: 'POST',
      body: {
        jugadorId: 'jugador-1',
        cantidad: 6,
        precioUnitario: 50,
        horasLimite: 24,
      },
      token: 'vendedor-token',
    })
  })

  it('comprarDeOferta hace POST /ofertas/{id}/compras con cantidad y token (UC-17)', async () => {
    await comprarDeOferta('oferta-1', 3, 'comprador-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/ofertas/oferta-1/compras', {
      method: 'POST',
      body: { cantidad: 3 },
      token: 'comprador-token',
    })
  })

  it('cancelarOferta hace DELETE /ofertas/{id} con token (UC-17)', async () => {
    await cancelarOferta('oferta-1', 'vendedor-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/ofertas/oferta-1', {
      method: 'DELETE',
      token: 'vendedor-token',
    })
  })
})
