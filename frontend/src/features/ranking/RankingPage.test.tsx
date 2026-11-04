import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RankingPage } from './RankingPage'
import * as jugadoresApi from '../../api/jugadores'
import { formatMoney } from '../../shared/format'

vi.mock('../../api/jugadores')

const JUGADOR_1 = {
  id: 'j1',
  nombre: 'Lionel Messi',
  club: 'Inter Miami',
  posicion: 'Delantero',
  fechaNacimiento: '1987-06-24',
  nacionalidad: 'Argentina',
  liga: 'MLS',
  estado: 'ACTIVO' as const,
  tokensDisponibles: 100,
  fechaUltimaActualizacionRendimiento: null,
  fotoUrl: null,
}

const JUGADOR_2 = {
  id: 'j2',
  nombre: 'Erling Haaland',
  club: 'Manchester City',
  posicion: 'Delantero',
  fechaNacimiento: '2000-07-21',
  nacionalidad: 'Noruega',
  liga: 'Premier League',
  estado: 'ACTIVO' as const,
  tokensDisponibles: 90,
  fechaUltimaActualizacionRendimiento: null,
  fotoUrl: null,
}

const JUGADOR_3 = {
  id: 'j3',
  nombre: 'Kylian Mbappé',
  club: 'Real Madrid',
  posicion: 'Delantero',
  fechaNacimiento: '1998-12-20',
  nacionalidad: 'Francia',
  liga: 'La Liga',
  estado: 'ACTIVO' as const,
  tokensDisponibles: 85,
  fechaUltimaActualizacionRendimiento: null,
  fotoUrl: null,
}

const JUGADOR_4 = {
  id: 'j4',
  nombre: 'Vinicius Jr',
  club: 'Real Madrid',
  posicion: 'Delantero',
  fechaNacimiento: '2000-07-12',
  nacionalidad: 'Brasil',
  liga: 'La Liga',
  estado: 'ACTIVO' as const,
  tokensDisponibles: 80,
  fechaUltimaActualizacionRendimiento: null,
  fotoUrl: null,
}

const RANKING_MOCK: jugadoresApi.JugadorConPrecioResponse[] = [
  {
    jugador: JUGADOR_1,
    precio: '150.00',
    variacionPorcentual: '12.50',
  },
  {
    jugador: JUGADOR_2,
    precio: '120.00',
    variacionPorcentual: '-2.30',
  },
  {
    jugador: JUGADOR_3,
    precio: null,
    variacionPorcentual: '0.00',
  },
  {
    jugador: JUGADOR_4,
    precio: '90.00',
    variacionPorcentual: null,
  },
]

function renderPagina() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/ranking']}>
        <Routes>
          <Route path="/ranking" element={<RankingPage />} />
          <Route path="/jugadores/:id" element={<p>Ficha de jugador</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('RankingPage', () => {
  beforeEach(() => {
    vi.mocked(jugadoresApi.obtenerRanking).mockResolvedValue(RANKING_MOCK)
  })

  it('muestra el título y los botones de estrategia de ranking', async () => {
    renderPagina()

    expect(
      screen.getByRole('heading', { name: 'Ranking de Jugadores' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('tab', { name: /Mayor Cotización/i }),
    ).toHaveAttribute('aria-selected', 'true')
    expect(
      screen.getByRole('tab', { name: /Mayor Crecimiento/i }),
    ).toHaveAttribute('aria-selected', 'false')
  })

  it('renderiza la lista de jugadores ordenados con precios y variaciones para distintas posiciones', async () => {
    renderPagina()

    expect(await screen.findByText('Lionel Messi')).toBeInTheDocument()
    expect(screen.getByText('Erling Haaland')).toBeInTheDocument()
    expect(screen.getByText('Kylian Mbappé')).toBeInTheDocument()
    expect(screen.getByText('Vinicius Jr')).toBeInTheDocument()

    // Puestos #1, #2, #3 y #4
    expect(screen.getByText('#1')).toBeInTheDocument()
    expect(screen.getByText('#2')).toBeInTheDocument()
    expect(screen.getByText('#3')).toBeInTheDocument()
    expect(screen.getByText('#4')).toBeInTheDocument()

    // Precios formateados y precio nulo '-'
    expect(screen.getByText(formatMoney('150.00'))).toBeInTheDocument()
    expect(screen.getByText(formatMoney('120.00'))).toBeInTheDocument()
    expect(screen.getAllByText('-').length).toBeGreaterThanOrEqual(1)

    // Variaciones: positiva, negativa, cero y nula
    expect(screen.getByText(/12\.50%/)).toBeInTheDocument()
    expect(screen.getByText(/2\.30%/)).toBeInTheDocument()
    expect(screen.getByText('0.00%')).toBeInTheDocument()
  })

  it('permite cambiar la estrategia de ranking y volver a la anterior', async () => {
    const user = userEvent.setup()
    renderPagina()

    const tabCrecimiento = screen.getByRole('tab', {
      name: /Mayor Crecimiento/i,
    })
    const tabCotizacion = screen.getByRole('tab', {
      name: /Mayor Cotización/i,
    })

    // Cambiar a Mayor Crecimiento
    await user.click(tabCrecimiento)
    expect(jugadoresApi.obtenerRanking).toHaveBeenCalledWith(
      'variacion-semanal',
    )
    expect(tabCrecimiento).toHaveAttribute('aria-selected', 'true')

    // Volver a Mayor Cotización
    await user.click(tabCotizacion)
    expect(jugadoresApi.obtenerRanking).toHaveBeenCalledWith('cotizacion')
    expect(tabCotizacion).toHaveAttribute('aria-selected', 'true')
  })

  it('muestra estado vacío si el ranking no tiene elementos', async () => {
    vi.mocked(jugadoresApi.obtenerRanking).mockResolvedValue([])
    renderPagina()

    expect(
      await screen.findByText(
        'No hay jugadores disponibles para armar el ranking en este momento.',
      ),
    ).toBeInTheDocument()
  })

  it('muestra mensaje de error si la carga del ranking falla', async () => {
    vi.mocked(jugadoresApi.obtenerRanking).mockRejectedValue(
      new Error('Network error'),
    )
    renderPagina()

    expect(
      await screen.findByText(
        'No se pudo cargar el ranking. Intente nuevamente más tarde.',
      ),
    ).toBeInTheDocument()
  })
})
