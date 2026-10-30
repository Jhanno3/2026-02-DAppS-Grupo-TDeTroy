import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from './client'
import {
  crearJugador,
  darDeBajaJugador,
  editarJugador,
  listarCotizaciones,
  listarJugadores,
  listarMercado,
  obtenerEstadisticas,
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

  it('darDeBajaJugador hace POST /jugadores/{id}/baja con el token (UC-15)', async () => {
    await darDeBajaJugador('abc-123', 'admin-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/jugadores/abc-123/baja', {
      method: 'POST',
      token: 'admin-token',
    })
  })

  describe('listarMercado', () => {
    it('pide GET /jugadores/mercado con pagina/tamanio/orden, sin posicion ni busqueda si no vienen', async () => {
      await listarMercado({ pagina: 0, orden: 'precio-desc' })

      expect(client.apiFetch).toHaveBeenCalledWith(
        '/jugadores/mercado?pagina=0&tamanio=50&orden=precio-desc',
      )
    })

    it('usa el tamanio pedido en vez del default', async () => {
      await listarMercado({ pagina: 2, tamanio: 20, orden: 'nombre' })

      expect(client.apiFetch).toHaveBeenCalledWith(
        '/jugadores/mercado?pagina=2&tamanio=20&orden=nombre',
      )
    })

    it('incluye posicion cuando no es "Todos"', async () => {
      await listarMercado({ pagina: 0, orden: 'suba', posicion: 'Arquero' })

      expect(client.apiFetch).toHaveBeenCalledWith(
        '/jugadores/mercado?pagina=0&tamanio=50&orden=suba&posicion=Arquero',
      )
    })

    it('omite posicion cuando es "Todos"', async () => {
      await listarMercado({
        pagina: 0,
        orden: 'precio-desc',
        posicion: 'Todos',
      })

      expect(client.apiFetch).toHaveBeenCalledWith(
        '/jugadores/mercado?pagina=0&tamanio=50&orden=precio-desc',
      )
    })

    it('incluye busqueda cuando viene, la omite si viene vacía', async () => {
      await listarMercado({
        pagina: 0,
        orden: 'precio-desc',
        busqueda: 'Messi',
      })
      await listarMercado({ pagina: 0, orden: 'precio-desc', busqueda: '' })

      expect(client.apiFetch).toHaveBeenNthCalledWith(
        1,
        '/jugadores/mercado?pagina=0&tamanio=50&orden=precio-desc&busqueda=Messi',
      )
      expect(client.apiFetch).toHaveBeenNthCalledWith(
        2,
        '/jugadores/mercado?pagina=0&tamanio=50&orden=precio-desc',
      )
    })
  })

  describe('obtenerEstadisticas', () => {
    it('pide GET /jugadores/{id}/estadisticas y devuelve el acumulado', async () => {
      const estadisticas = {
        partidosJugados: 9,
        minutosJugados: 503,
        goles: 2,
        asistencias: 3,
        figuraDelPartido: 0,
        tarjetasAmarillas: 1,
        tarjetasRojas: 0,
        fechaActualizacion: '2026-10-29T19:43:00Z',
      }
      vi.mocked(client.apiFetch).mockResolvedValue(estadisticas)

      expect(await obtenerEstadisticas('jugador-1')).toEqual(estadisticas)
      expect(client.apiFetch).toHaveBeenCalledWith(
        '/jugadores/jugador-1/estadisticas',
      )
    })

    it('con 204 (todavía sin datos de rendimiento) devuelve null, no undefined', async () => {
      vi.mocked(client.apiFetch).mockResolvedValue(undefined)

      expect(await obtenerEstadisticas('jugador-1')).toBeNull()
    })
  })
})
