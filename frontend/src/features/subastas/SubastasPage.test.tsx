import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SubastasPage } from './SubastasPage'
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

const JUGADOR = {
  id: 'jugador-1',
  nombre: 'Matteo Politano',
  club: 'SSC Napoli',
  posicion: 'Delantero',
  fechaNacimiento: '1993-08-03',
  nacionalidad: 'Italy',
  liga: 'Serie A',
  estado: 'ACTIVO' as const,
  tokensDisponibles: 95,
  fechaUltimaActualizacionRendimiento: null,
  fotoUrl: null,
}

function renderPagina() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/subastas']}>
        <Routes>
          <Route path="/subastas" element={<SubastasPage />} />
          <Route path="/login" element={<p>Pantalla de login</p>} />
          <Route
            path="/jugadores/:id/subasta"
            element={<p>Formulario de subasta</p>}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('SubastasPage', () => {
  beforeEach(() => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: 'fake-token',
      user: { usuarioId: 'u1', email: 'user@example.com', rol: 'USER' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    })
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([JUGADOR])
    vi.mocked(subastasApi.listarSubastas).mockResolvedValue([])
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '1000.00',
      tenencias: [
        {
          jugadorId: 'jugador-1',
          cantidad: 3,
          cotizacionVigente: '104.39',
          valor: '313.17',
        },
      ],
      valorEnVenta: '0.00',
      saldoEnPujas: '0.00',
      valorTotal: '1313.17',
    })
  })

  it('sin subastas activas, muestra el mensaje correspondiente', async () => {
    renderPagina()

    expect(
      await screen.findByText('No hay subastas activas en este momento.'),
    ).toBeInTheDocument()
  })

  it('sin sesión, "Crear subasta" navega a /login', async () => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: null,
      user: null,
      isAuthenticated: false,
      login: vi.fn(),
      logout: vi.fn(),
    })
    const user = userEvent.setup()
    renderPagina()

    await user.click(
      await screen.findByRole('button', { name: 'Crear subasta' }),
    )

    expect(await screen.findByText('Pantalla de login')).toBeInTheDocument()
  })

  it('con sesión, "Crear subasta" abre el selector y elegir un jugador lleva al formulario de subasta', async () => {
    const user = userEvent.setup()
    renderPagina()

    await user.click(
      await screen.findByRole('button', { name: 'Crear subasta' }),
    )

    expect(
      await screen.findByRole('dialog', {
        name: 'Elegí un jugador para crear una subasta',
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('3 token(s)')).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: /Matteo Politano/ }))

    expect(await screen.findByText('Formulario de subasta')).toBeInTheDocument()
  })
  it('lista las subastas activas con su precio, etiqueta propia o "vas ganando"', async () => {
    const base = {
      jugadorId: 'jugador-1',
      cantidad: 2,
      precioInicial: '100.00',
      cantidadPujas: 0,
      estado: 'ACTIVA' as const,
      fechaCreacion: '2026-10-29T20:00:00Z',
      fechaVencimiento: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
      opcionesPuja: [],
      pujas: null,
    }
    vi.mocked(subastasApi.listarSubastas).mockResolvedValue([
      {
        ...base,
        id: 's1',
        vendedorId: 'u1',
        precioActual: null,
        mejorPostorId: null,
        cierraEn: base.fechaVencimiento,
      },
      {
        ...base,
        id: 's2',
        vendedorId: 'otro',
        precioActual: '110.00',
        mejorPostorId: 'u1',
        cantidadPujas: 1,
        // Cierra en menos de 2 minutos: el reloj se marca como urgente.
        cierraEn: new Date(Date.now() + 60 * 1000).toISOString(),
      },
    ])
    renderPagina()

    const tarjetas = await screen.findAllByRole('link', {
      name: /Matteo Politano/,
    })
    expect(tarjetas).toHaveLength(2)
    expect(tarjetas[0]).toHaveTextContent('Precio inicial')
    expect(tarjetas[0]).toHaveTextContent('Tu subasta')
    expect(tarjetas[1]).toHaveTextContent('$ 110,00')
    expect(tarjetas[1]).toHaveTextContent('1 puja · Vas ganando')
    expect(tarjetas[1]).toHaveAttribute('href', '/subastas/s2')
  })
})
