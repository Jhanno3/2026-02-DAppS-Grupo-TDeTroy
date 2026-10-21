import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CarteraPage } from './CarteraPage'
import * as portfolioApi from '../../api/portfolio'
import * as jugadoresApi from '../../api/jugadores'
import * as authContext from '../auth/AuthContext'

vi.mock('../../api/portfolio')
vi.mock('../../api/jugadores')
vi.mock('../auth/AuthContext', async (importOriginal) => {
  const actual = await importOriginal<typeof authContext>()
  return { ...actual, useAuth: vi.fn() }
})

function renderCartera() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <CarteraPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('CarteraPage', () => {
  beforeEach(() => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: 'fake-token',
      user: { usuarioId: 'u1', email: 'user@example.com', rol: 'USER' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    })
  })

  it('cartera vacía, muestra el mensaje con link al mercado', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '0.00',
      tenencias: [],
      valorTotal: '0.00',
    })
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([])

    renderCartera()

    expect(
      await screen.findByText(/Todavía no tenés tokens/),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Mirá el mercado' }),
    ).toHaveAttribute('href', '/mercado')
  })

  it('muestra las KPIs (valor total, saldo, cantidad de jugadores)', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '4908.64',
      tenencias: [
        {
          jugadorId: 'j1',
          cantidad: 1,
          cotizacionVigente: '91.36',
          valor: '91.36',
        },
      ],
      valorTotal: '5000.00',
    })
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([
      {
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
      },
    ])

    renderCartera()

    expect(await screen.findByText('$ 5.000,00')).toBeInTheDocument()
    expect(screen.getByText('$ 4.908,64')).toBeInTheDocument()
    expect(screen.getByText('Leonel Messi')).toBeInTheDocument()
    expect(screen.getByText('Inter Miami CF')).toBeInTheDocument()
  })

  it('una tenencia de un jugador que ya no está en el catálogo, no revienta la página', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '0.00',
      tenencias: [
        {
          jugadorId: 'desconocido',
          cantidad: 1,
          cotizacionVigente: '100.00',
          valor: '100.00',
        },
      ],
      valorTotal: '100.00',
    })
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([])

    renderCartera()

    expect(await screen.findByText('Jugador no disponible')).toBeInTheDocument()
  })
})
