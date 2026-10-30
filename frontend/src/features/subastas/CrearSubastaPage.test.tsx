import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CrearSubastaPage } from './CrearSubastaPage'
import * as jugadoresApi from '../../api/jugadores'
import * as portfolioApi from '../../api/portfolio'
import * as subastasApi from '../../api/subastas'
import * as authContext from '../auth/AuthContext'

vi.mock('../../api/jugadores')
vi.mock('../../api/portfolio')
vi.mock('../../api/subastas')
vi.mock('../auth/AuthContext', async (importOriginal) => {
  const actual = await importOriginal<typeof authContext>()
  return { ...actual, useAuth: vi.fn() }
})

function renderPagina() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/jugadores/j1/subasta']}>
        <Routes>
          <Route path="/jugadores/:id/subasta" element={<CrearSubastaPage />} />
          <Route path="/subastas/:id" element={<p>Detalle de la subasta</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('CrearSubastaPage', () => {
  beforeEach(() => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: 'fake-token',
      user: { usuarioId: 'u1', email: 'user@example.com', rol: 'USER' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    })
    vi.mocked(jugadoresApi.obtenerJugador).mockResolvedValue({
      id: 'j1',
      nombre: 'Matteo Politano',
      club: 'SSC Napoli',
      posicion: 'Delantero',
      fechaNacimiento: '1993-08-03',
      nacionalidad: 'Italy',
      liga: 'Serie A',
      estado: 'ACTIVO',
      tokensDisponibles: 90,
      fechaUltimaActualizacionRendimiento: null,
      fotoUrl: null,
    })
    vi.mocked(jugadoresApi.listarCotizaciones).mockResolvedValue([
      {
        id: 'c1',
        semana: '2026-W44',
        valor: '104.39',
        origen: 'AUTOMATICO',
        fechaCalculo: '2026-10-29T00:00:00Z',
      },
    ])
  })

  it('precarga el precio inicial con la cotización y crea la subasta con los valores elegidos', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '0.00',
      tenencias: [
        {
          jugadorId: 'j1',
          cantidad: 3,
          cotizacionVigente: '104.39',
          valor: '313.17',
        },
      ],
      valorEnVenta: '0.00',
      saldoEnPujas: '0.00',
      valorTotal: '313.17',
    })
    vi.mocked(subastasApi.crearSubasta).mockResolvedValue({
      id: 's1',
    } as subastasApi.SubastaResponse)
    const user = userEvent.setup()
    renderPagina()

    expect(
      await screen.findByText('Cotización actual: $ 104,39'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText(/Precio inicial por token/)).toHaveValue(
      104.39,
    )

    await user.selectOptions(screen.getByLabelText('Duración'), '60')
    await user.click(screen.getByRole('button', { name: 'Iniciar subasta' }))

    expect(subastasApi.crearSubasta).toHaveBeenCalledWith(
      {
        jugadorId: 'j1',
        cantidad: 1,
        precioInicial: 104.39,
        duracionMinutos: 60,
      },
      'fake-token',
    )
    expect(await screen.findByText('Detalle de la subasta')).toBeInTheDocument()
  })

  it('sin tokens disponibles del jugador, no muestra el formulario', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '0.00',
      tenencias: [],
      valorEnVenta: '0.00',
      saldoEnPujas: '0.00',
      valorTotal: '0.00',
    })
    renderPagina()

    expect(
      await screen.findByText(
        'No tenés tokens disponibles de este jugador para subastar.',
      ),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Iniciar subasta' }),
    ).not.toBeInTheDocument()
  })
})
