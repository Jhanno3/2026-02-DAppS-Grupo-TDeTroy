import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MercadoPage } from './MercadoPage'
import * as jugadoresApi from '../../api/jugadores'
import type {
  JugadorConPrecioResponse,
  JugadorResponse,
  ParametrosMercado,
} from '../../api/jugadores'

vi.mock('../../api/jugadores')

function jugador(overrides: Partial<JugadorResponse> = {}): JugadorResponse {
  return {
    id: 'j1',
    nombre: 'Leonel Messi',
    club: 'Inter Miami CF',
    posicion: 'Delantero',
    fechaNacimiento: '1987-06-24',
    nacionalidad: 'Argentina',
    liga: 'Major League Soccer',
    estado: 'ACTIVO',
    tokensDisponibles: 57,
    fechaUltimaActualizacionRendimiento: null,
    fotoUrl: null,
    ...overrides,
  }
}

function item(
  jugadorOverrides: Partial<JugadorResponse> = {},
  precio: string | null = null,
  variacionPorcentual: string | null = null,
): JugadorConPrecioResponse {
  return { jugador: jugador(jugadorOverrides), precio, variacionPorcentual }
}

/**
 * Mock de `GET /jugadores/mercado` que honra filtro/orden/paginado tal como lo hace el backend
 * real (`JugadorServiceImpl#listarMercado`) -- el filtro/orden/paginado ahora vive del lado del
 * servidor, así que estos tests verifican que el componente manda los parámetros correctos y
 * renderiza lo que el backend le devuelve, no una lógica de filtro propia del componente.
 */
function mockMercado(catalogo: JugadorConPrecioResponse[]) {
  vi.mocked(jugadoresApi.listarMercado).mockImplementation(
    (params: ParametrosMercado) => {
      let filtrado = catalogo.filter(
        (it) =>
          (!params.posicion ||
            params.posicion === 'Todos' ||
            it.jugador.posicion === params.posicion) &&
          (!params.busqueda ||
            it.jugador.nombre
              .toLowerCase()
              .includes(params.busqueda.toLowerCase()) ||
            it.jugador.club
              .toLowerCase()
              .includes(params.busqueda.toLowerCase())),
      )

      const num = (v: string | null) => (v === null ? null : Number(v))
      const comparadores: Record<
        ParametrosMercado['orden'],
        (a: JugadorConPrecioResponse, b: JugadorConPrecioResponse) => number
      > = {
        nombre: (a, b) => a.jugador.nombre.localeCompare(b.jugador.nombre),
        'precio-desc': (a, b) => (num(b.precio) ?? -1) - (num(a.precio) ?? -1),
        'precio-asc': (a, b) =>
          (num(a.precio) ?? Infinity) - (num(b.precio) ?? Infinity),
        suba: (a, b) =>
          (num(b.variacionPorcentual) ?? -Infinity) -
          (num(a.variacionPorcentual) ?? -Infinity),
        disponibles: (a, b) =>
          b.jugador.tokensDisponibles - a.jugador.tokensDisponibles,
      }
      filtrado = [...filtrado].sort(comparadores[params.orden])

      const tamanio = params.tamanio ?? 50
      const desde = params.pagina * tamanio
      const posicionesDisponibles = [
        ...new Set(catalogo.map((it) => it.jugador.posicion)),
      ]

      return Promise.resolve({
        contenido: filtrado.slice(desde, desde + tamanio),
        pagina: params.pagina,
        tamanioPagina: tamanio,
        totalElementos: filtrado.length,
        totalPaginas: Math.max(1, Math.ceil(filtrado.length / tamanio)),
        posicionesDisponibles,
      })
    },
  )
}

function renderMercado() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <MercadoPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('MercadoPage', () => {
  beforeEach(() => {
    vi.useRealTimers()
  })

  it('catálogo vacío, muestra el estado vacío sin romper', async () => {
    mockMercado([])

    renderMercado()

    expect(
      await screen.findByText('No hay jugadores que coincidan con el filtro.'),
    ).toBeInTheDocument()
  })

  it('si falla el catálogo, muestra el mensaje de error', async () => {
    vi.mocked(jugadoresApi.listarMercado).mockRejectedValue(new Error('500'))

    renderMercado()

    expect(
      await screen.findByText(
        'No se pudo cargar el mercado. Intentá de nuevo.',
      ),
    ).toBeInTheDocument()
  })

  it('muestra el precio que manda el backend', async () => {
    mockMercado([item({}, '91.36')])

    renderMercado()

    expect(await screen.findByText('$ 91,36')).toBeInTheDocument()
  })

  it('un jugador sin cotización todavía, muestra "Sin cotización" en vez de un precio', async () => {
    mockMercado([item({}, null)])

    renderMercado()

    expect(await screen.findByText('Sin cotización')).toBeInTheDocument()
  })

  it('un jugador INACTIVO, muestra el badge correspondiente', async () => {
    mockMercado([item({ estado: 'INACTIVO' })])

    renderMercado()

    expect(await screen.findByText('INACTIVO')).toBeInTheDocument()
  })

  it('el orden por defecto es precio de mayor a menor, no nombre', async () => {
    mockMercado([])

    renderMercado()
    await screen.findByText('No hay jugadores que coincidan con el filtro.')

    expect(
      screen.getByRole('option', { name: 'Precio: mayor a menor' }),
    ).toHaveProperty('selected', true)
    expect(jugadoresApi.listarMercado).toHaveBeenCalledWith(
      expect.objectContaining({ orden: 'precio-desc' }),
    )
  })

  it('buscar por nombre pide al backend la búsqueda y muestra sólo lo que devuelve', async () => {
    mockMercado([
      item({ id: 'j1', nombre: 'Leonel Messi' }, '100.00'),
      item(
        { id: 'j2', nombre: 'Kylian Mbappe', club: 'Real Madrid CF' },
        '100.00',
      ),
    ])
    const user = userEvent.setup()

    renderMercado()
    await screen.findByText('Kylian Mbappe')

    await user.type(screen.getByPlaceholderText('Jugador o club'), 'Messi')

    await waitFor(() => {
      expect(screen.getByText('Leonel Messi')).toBeInTheDocument()
      expect(screen.queryByText('Kylian Mbappe')).not.toBeInTheDocument()
    })
  })

  it('las posiciones del filtro se arman a partir de lo que devuelve el backend', async () => {
    mockMercado([
      item({ id: 'j1', posicion: 'Delantero' }, '100.00'),
      item({ id: 'j2', posicion: 'Arquero' }, '100.00'),
    ])

    renderMercado()

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: 'Delantero' }),
      ).toBeInTheDocument()
      expect(
        screen.getByRole('button', { name: 'Arquero' }),
      ).toBeInTheDocument()
    })
  })

  it('filtrar por posición esconde a los jugadores de otra posición', async () => {
    mockMercado([
      item(
        { id: 'j1', nombre: 'Leonel Messi', posicion: 'Delantero' },
        '100.00',
      ),
      item(
        { id: 'j2', nombre: 'Emiliano Martinez', posicion: 'Arquero' },
        '100.00',
      ),
    ])
    const user = userEvent.setup()

    renderMercado()
    await screen.findByText('Emiliano Martinez')

    await user.click(screen.getByRole('button', { name: 'Arquero' }))

    await waitFor(() => {
      expect(screen.getByText('Emiliano Martinez')).toBeInTheDocument()
      expect(screen.queryByText('Leonel Messi')).not.toBeInTheDocument()
    })
  })

  it('cada carta linkea al detalle real del jugador', async () => {
    mockMercado([item({ id: 'abc-123' }, '100.00')])

    renderMercado()
    await screen.findByText('Leonel Messi')

    expect(screen.getByRole('link', { name: /Leonel Messi/ })).toHaveAttribute(
      'href',
      '/jugadores/abc-123',
    )
  })

  it('con más de una página, muestra los controles de paginación y pide la siguiente página', async () => {
    const catalogo = Array.from({ length: 51 }, (_, i) =>
      item({ id: `j${i}`, nombre: `Jugador ${i}` }, '100.00'),
    )
    mockMercado(catalogo)
    const user = userEvent.setup()

    renderMercado()
    await screen.findByText('Página 1 de 2 (51 jugadores)')

    await user.click(screen.getByRole('button', { name: 'Siguiente →' }))

    await waitFor(() => {
      expect(jugadoresApi.listarMercado).toHaveBeenCalledWith(
        expect.objectContaining({ pagina: 1 }),
      )
    })

    await user.click(screen.getByRole('button', { name: '← Anterior' }))

    await waitFor(() => {
      expect(jugadoresApi.listarMercado).toHaveBeenCalledWith(
        expect.objectContaining({ pagina: 0 }),
      )
    })
  })

  describe('ordenar por', () => {
    function catalogoDosJugadores(): JugadorConPrecioResponse[] {
      return [
        item(
          { id: 'barato', nombre: 'Zinedine Barato', tokensDisponibles: 10 },
          '60.00',
          '20.0',
        ),
        item(
          { id: 'caro', nombre: 'Andres Caro', tokensDisponibles: 90 },
          '150.00',
          '-25.0',
        ),
      ]
    }

    function nombresEnOrden() {
      return screen
        .getAllByRole('link')
        .map((el) => el.querySelector('span')?.textContent)
    }

    it('precio-desc (default) ordena del más caro al más barato', async () => {
      mockMercado(catalogoDosJugadores())

      renderMercado()
      await screen.findByText('Zinedine Barato')
      await screen.findByText('Andres Caro')

      expect(nombresEnOrden()).toEqual(['Andres Caro', 'Zinedine Barato'])
    })

    it('precio-asc ordena del más barato al más caro', async () => {
      mockMercado(catalogoDosJugadores())
      const user = userEvent.setup()

      renderMercado()
      await screen.findByText('Zinedine Barato')

      await user.selectOptions(screen.getByRole('combobox'), 'precio-asc')

      await waitFor(() =>
        expect(nombresEnOrden()).toEqual(['Zinedine Barato', 'Andres Caro']),
      )
    })

    it('suba ordena por mayor variación primero', async () => {
      mockMercado(catalogoDosJugadores())
      const user = userEvent.setup()

      renderMercado()
      await screen.findByText('Zinedine Barato')

      await user.selectOptions(screen.getByRole('combobox'), 'suba')

      await waitFor(() =>
        expect(nombresEnOrden()).toEqual(['Zinedine Barato', 'Andres Caro']),
      )
    })

    it('disponibles ordena por más tokens disponibles primero', async () => {
      mockMercado(catalogoDosJugadores())
      const user = userEvent.setup()

      renderMercado()
      await screen.findByText('Zinedine Barato')

      await user.selectOptions(screen.getByRole('combobox'), 'disponibles')

      await waitFor(() =>
        expect(nombresEnOrden()).toEqual(['Andres Caro', 'Zinedine Barato']),
      )
    })
  })
})
