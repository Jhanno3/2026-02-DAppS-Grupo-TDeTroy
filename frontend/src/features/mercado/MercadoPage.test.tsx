import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MercadoPage } from './MercadoPage'
import * as jugadoresApi from '../../api/jugadores'
import type { JugadorResponse, CotizacionResponse } from '../../api/jugadores'

vi.mock('../../api/jugadores')

function jugador(overrides: Partial<JugadorResponse> = {}): JugadorResponse {
  return {
    id: 'j1',
    nombre: 'Leonel Messi',
    club: 'Inter Miami CF',
    posicion: 'Delantero',
    fechaNacimiento: '1987-06-24',
    nacionalidad: 'Argentina',
    estado: 'ACTIVO',
    tokensDisponibles: 57,
    fechaUltimaActualizacionRendimiento: null,
    fotoUrl: null,
    ...overrides,
  }
}

function cotizaciones(...valores: string[]): CotizacionResponse[] {
  return valores.map((valor, i) => ({
    id: `c${i}`,
    semana: `2026-W0${i + 1}`,
    valor,
    origen: 'AUTOMATICO',
    fechaCalculo: '2026-02-16T00:00:00Z',
  }))
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
    vi.mocked(jugadoresApi.listarCotizaciones).mockResolvedValue([])
  })

  it('catálogo vacío, muestra el estado vacío sin romper', async () => {
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([])

    renderMercado()

    expect(
      await screen.findByText('No hay jugadores que coincidan con el filtro.'),
    ).toBeInTheDocument()
  })

  it('si falla el catálogo, muestra el mensaje de error', async () => {
    vi.mocked(jugadoresApi.listarJugadores).mockRejectedValue(new Error('500'))

    renderMercado()

    expect(
      await screen.findByText(
        'No se pudo cargar el mercado. Intentá de nuevo.',
      ),
    ).toBeInTheDocument()
  })

  it('muestra el precio como la última cotización del jugador', async () => {
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([jugador()])
    vi.mocked(jugadoresApi.listarCotizaciones).mockResolvedValue(
      cotizaciones('85.00', '91.36'),
    )

    renderMercado()

    expect(await screen.findByText('$ 91,36')).toBeInTheDocument()
  })

  it('un jugador sin cotización todavía, muestra "Sin cotización" en vez de un precio', async () => {
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([jugador()])
    vi.mocked(jugadoresApi.listarCotizaciones).mockResolvedValue([])

    renderMercado()

    expect(await screen.findByText('Sin cotización')).toBeInTheDocument()
  })

  it('un jugador INACTIVO, muestra el badge correspondiente', async () => {
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([
      jugador({ estado: 'INACTIVO' }),
    ])

    renderMercado()

    expect(await screen.findByText('INACTIVO')).toBeInTheDocument()
  })

  it('buscar por nombre filtra la grilla', async () => {
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([
      jugador({ id: 'j1', nombre: 'Leonel Messi' }),
      jugador({ id: 'j2', nombre: 'Kylian Mbappe', club: 'Real Madrid CF' }),
    ])
    const user = userEvent.setup()

    renderMercado()
    await screen.findByText('Kylian Mbappe')

    await user.type(screen.getByPlaceholderText('Jugador o club'), 'Messi')

    expect(screen.getByText('Leonel Messi')).toBeInTheDocument()
    expect(screen.queryByText('Kylian Mbappe')).not.toBeInTheDocument()
  })

  it('las posiciones del filtro se arman dinámicamente a partir del catálogo real', async () => {
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([
      jugador({ id: 'j1', posicion: 'Delantero' }),
      jugador({ id: 'j2', posicion: 'Arquero' }),
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
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([
      jugador({ id: 'j1', nombre: 'Leonel Messi', posicion: 'Delantero' }),
      jugador({ id: 'j2', nombre: 'Emiliano Martinez', posicion: 'Arquero' }),
    ])
    const user = userEvent.setup()

    renderMercado()
    await screen.findByText('Emiliano Martinez')

    await user.click(screen.getByRole('button', { name: 'Arquero' }))

    expect(screen.getByText('Emiliano Martinez')).toBeInTheDocument()
    expect(screen.queryByText('Leonel Messi')).not.toBeInTheDocument()
  })

  it('cada carta linkea al detalle real del jugador', async () => {
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([
      jugador({ id: 'abc-123' }),
    ])

    renderMercado()
    await screen.findByText('Leonel Messi')

    expect(screen.getByRole('link', { name: /Leonel Messi/ })).toHaveAttribute(
      'href',
      '/jugadores/abc-123',
    )
  })

  describe('ordenar por', () => {
    async function setUpDosJugadores() {
      vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([
        jugador({
          id: 'barato',
          nombre: 'Zinedine Barato',
          tokensDisponibles: 10,
        }),
        jugador({
          id: 'caro',
          nombre: 'Andres Caro',
          tokensDisponibles: 90,
        }),
      ])
      vi.mocked(jugadoresApi.listarCotizaciones).mockImplementation((id) =>
        Promise.resolve(
          id === 'barato'
            ? cotizaciones('50.00', '60.00')
            : cotizaciones('200.00', '150.00'),
        ),
      )
      renderMercado()
      await screen.findByText('Zinedine Barato')
      await screen.findByText('Andres Caro')
    }

    function nombresEnOrden() {
      return screen
        .getAllByRole('link')
        .map((el) => el.querySelector('span')?.textContent)
    }

    it('precio-desc ordena del más caro al más barato', async () => {
      const user = userEvent.setup()
      await setUpDosJugadores()

      await user.selectOptions(screen.getByRole('combobox'), 'precio-desc')

      expect(nombresEnOrden()).toEqual(['Andres Caro', 'Zinedine Barato'])
    })

    it('precio-asc ordena del más barato al más caro', async () => {
      const user = userEvent.setup()
      await setUpDosJugadores()

      await user.selectOptions(screen.getByRole('combobox'), 'precio-asc')

      expect(nombresEnOrden()).toEqual(['Zinedine Barato', 'Andres Caro'])
    })

    it('suba ordena por mayor variación primero (Caro baja, Barato sube)', async () => {
      const user = userEvent.setup()
      await setUpDosJugadores()

      await user.selectOptions(screen.getByRole('combobox'), 'suba')

      expect(nombresEnOrden()).toEqual(['Zinedine Barato', 'Andres Caro'])
    })

    it('disponibles ordena por más tokens disponibles primero', async () => {
      const user = userEvent.setup()
      await setUpDosJugadores()

      await user.selectOptions(screen.getByRole('combobox'), 'disponibles')

      expect(nombresEnOrden()).toEqual(['Andres Caro', 'Zinedine Barato'])
    })
  })
})
