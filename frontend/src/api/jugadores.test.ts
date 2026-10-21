import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from './client'
import {
  crearJugador,
  editarJugador,
  listarCotizaciones,
  listarJugadores,
  obtenerJugador,
} from './jugadores'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof client>()
  return { ...actual, apiFetch: vi.fn() }
})

describe('api/jugadores', () => {
  beforeEach(() => {
    vi.mocked(client.apiFetch).mockResolvedValue(undefined)
  })

  it('listarJugadores pide GET /jugadores', async () => {
    await listarJugadores()

    expect(client.apiFetch).toHaveBeenCalledWith('/jugadores')
  })

  it('obtenerJugador pide GET /jugadores/{id}', async () => {
    await obtenerJugador('abc-123')

    expect(client.apiFetch).toHaveBeenCalledWith('/jugadores/abc-123')
  })

  it('listarCotizaciones pide GET /jugadores/{id}/cotizaciones', async () => {
    await listarCotizaciones('abc-123')

    expect(client.apiFetch).toHaveBeenCalledWith(
      '/jugadores/abc-123/cotizaciones',
    )
  })

  it('crearJugador hace POST /jugadores con el token (UC-03)', async () => {
    const datos = {
      nombre: 'Leonel Messi',
      club: 'Inter Miami CF',
      posicion: 'Delantero',
      fechaNacimiento: '1987-06-24',
      nacionalidad: 'Argentina',
      liga: 'Major League Soccer',
    }

    await crearJugador(datos, 'admin-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/jugadores', {
      method: 'POST',
      body: datos,
      token: 'admin-token',
    })
  })

  it('editarJugador hace PUT /jugadores/{id} con el token (UC-04)', async () => {
    const datos = {
      nombre: 'Leonel Messi',
      club: 'Inter Miami CF',
      posicion: 'Delantero',
      fechaNacimiento: '1987-06-24',
      nacionalidad: 'Argentina',
      liga: 'Major League Soccer',
    }

    await editarJugador('abc-123', datos, 'admin-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/jugadores/abc-123', {
      method: 'PUT',
      body: datos,
      token: 'admin-token',
    })
  })
})
