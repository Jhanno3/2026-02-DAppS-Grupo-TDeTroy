import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { OfertaP2PJugadorPage } from './OfertaP2PJugadorPage'
import { ApiError } from '../../api/client'
import * as jugadoresApi from '../../api/jugadores'
import * as ofertasApi from '../../api/ofertas'
import * as portfolioApi from '../../api/portfolio'
import * as authContext from '../auth/AuthContext'

vi.mock('../../api/jugadores')
vi.mock('../../api/ofertas')
vi.mock('../../api/portfolio')
vi.mock('../auth/AuthContext', async (importOriginal) => {
  const actual = await importOriginal<typeof authContext>()
  return { ...actual, useAuth: vi.fn() }
})

const JUGADOR_ID = 'jugador-1'

const JUGADOR = {
  id: JUGADOR_ID,
  nombre: 'Erling Haaland',
  club: 'Manchester City',
  posicion: 'Delantero',
  fechaNacimiento: '2000-07-21',
  nacionalidad: 'Noruega',
  liga: 'Premier League',
  estado: 'ACTIVO' as const,
  tokensDisponibles: 95,
  fechaUltimaActualizacionRendimiento: null,
  fotoUrl: null,
}

function renderPagina() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/jugadores/${JUGADOR_ID}/p2p`]}>
        <Routes>
          <Route path="/jugadores/:id/p2p" element={<OfertaP2PJugadorPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('OfertaP2PJugadorPage', () => {
  beforeEach(() => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: 'fake-token',
      user: { usuarioId: 'u1', email: 'user@example.com', rol: 'USER' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    })
    vi.mocked(jugadoresApi.obtenerJugador).mockResolvedValue(JUGADOR)
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([])
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '1000.00',
      tenencias: [],
      valorTotal: '1000.00',
    })
  })

  it('muestra el nombre y club del jugador', async () => {
    renderPagina()

    expect(await screen.findByText('Erling Haaland')).toBeInTheDocument()
    expect(screen.getByText('Manchester City · Delantero')).toBeInTheDocument()
  })

  it('sin ofertas abiertas, muestra el mensaje correspondiente', async () => {
    renderPagina()

    expect(
      await screen.findByText('No hay ofertas abiertas para este jugador.'),
    ).toBeInTheDocument()
  })

  it('sin sesión, invita a iniciar sesión o crear cuenta en vez del form de publicar', async () => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: null,
      user: null,
      isAuthenticated: false,
      login: vi.fn(),
      logout: vi.fn(),
    })

    renderPagina()

    expect(await screen.findByText(/Iniciá sesión/)).toBeInTheDocument()
    expect(
      screen.queryByText(/Publicar oferta \(tenés/),
    ).not.toBeInTheDocument()
  })

  it('sin tokens del jugador, muestra el aviso en vez del form de publicar', async () => {
    renderPagina()

    expect(
      await screen.findByText(
        'No tenés tokens disponibles de este jugador para publicar en una oferta.',
      ),
    ).toBeInTheDocument()
  })

  it('con tokens disponibles, publica una oferta con cantidad, precio y horas', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '1000.00',
      tenencias: [
        {
          jugadorId: JUGADOR_ID,
          cantidad: 10,
          cotizacionVigente: null,
          valor: '0.00',
        },
      ],
      valorTotal: '1000.00',
    })
    vi.mocked(ofertasApi.publicarOferta).mockResolvedValue({
      id: 'o1',
      vendedorId: 'u1',
      jugadorId: JUGADOR_ID,
      cantidadPublicada: 5,
      cantidadDisponible: 5,
      precioUnitario: '60.00',
      estado: 'ABIERTA',
      fechaCreacion: '2026-10-24T00:00:00Z',
      fechaVencimiento: '2026-10-25T00:00:00Z',
    })
    const user = userEvent.setup()
    renderPagina()

    await screen.findByText(/Publicar oferta \(tenés 10 disponibles\)/)
    await user.click(screen.getByRole('button', { name: 'Publicar oferta' }))

    expect(ofertasApi.publicarOferta).toHaveBeenCalledWith(
      JUGADOR_ID,
      1,
      1,
      24,
      'fake-token',
    )
  })

  it('lista una oferta abierta con su precio, cantidades y vencimiento', async () => {
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([
      {
        id: 'o1',
        vendedorId: 'otro-usuario',
        jugadorId: JUGADOR_ID,
        cantidadPublicada: 5,
        cantidadDisponible: 3,
        precioUnitario: '60.00',
        estado: 'ABIERTA',
        fechaCreacion: '2026-10-24T00:00:00Z',
        fechaVencimiento: '2026-10-25T00:00:00Z',
      },
    ])

    renderPagina()

    expect(await screen.findByText('3 / 5 disponibles')).toBeInTheDocument()
    expect(screen.getByText('$ 60,00 / token')).toBeInTheDocument()
  })

  it('en una oferta propia, muestra el botón Cancelar en vez del form de compra', async () => {
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([
      {
        id: 'o1',
        vendedorId: 'u1',
        jugadorId: JUGADOR_ID,
        cantidadPublicada: 5,
        cantidadDisponible: 5,
        precioUnitario: '60.00',
        estado: 'ABIERTA',
        fechaCreacion: '2026-10-24T00:00:00Z',
        fechaVencimiento: '2026-10-25T00:00:00Z',
      },
    ])
    vi.mocked(ofertasApi.cancelarOferta).mockResolvedValue(undefined)
    const user = userEvent.setup()
    renderPagina()

    await screen.findByText('Tu oferta')
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(ofertasApi.cancelarOferta).toHaveBeenCalledWith('o1', 'fake-token')
  })

  it('en una oferta de otro usuario, comprar llama a comprarDeOferta con la cantidad elegida', async () => {
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([
      {
        id: 'o1',
        vendedorId: 'otro-usuario',
        jugadorId: JUGADOR_ID,
        cantidadPublicada: 5,
        cantidadDisponible: 5,
        precioUnitario: '60.00',
        estado: 'ABIERTA',
        fechaCreacion: '2026-10-24T00:00:00Z',
        fechaVencimiento: '2026-10-25T00:00:00Z',
      },
    ])
    vi.mocked(ofertasApi.comprarDeOferta).mockResolvedValue({
      id: 'm1',
      jugadorId: JUGADOR_ID,
      tipo: 'COMPRA_P2P',
      cantidad: 2,
      precioUnitario: '60.00',
      montoTotal: '120.00',
      contraparteUsuarioId: 'otro-usuario',
      fecha: '2026-10-24T00:00:00Z',
    })
    const user = userEvent.setup()
    renderPagina()

    await user.click(
      await screen.findByRole('button', { name: 'Comprar de la oferta' }),
    )

    expect(ofertasApi.comprarDeOferta).toHaveBeenCalledWith(
      'o1',
      1,
      'fake-token',
    )
  })

  it('si falla publicar, muestra el mensaje de error de la API', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '1000.00',
      tenencias: [
        {
          jugadorId: JUGADOR_ID,
          cantidad: 10,
          cotizacionVigente: null,
          valor: '0.00',
        },
      ],
      valorTotal: '1000.00',
    })
    vi.mocked(ofertasApi.publicarOferta).mockRejectedValue(
      new ApiError(409, 'Tenencia insuficiente', 'Conflicto'),
    )
    const user = userEvent.setup()
    renderPagina()

    await screen.findByText(/Publicar oferta \(tenés 10 disponibles\)/)
    await user.click(screen.getByRole('button', { name: 'Publicar oferta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Tenencia insuficiente',
    )
  })

  it('jugador inexistente, muestra el mensaje de error', async () => {
    vi.mocked(jugadoresApi.obtenerJugador).mockRejectedValue(
      new ApiError(404, 'No existe', 'No encontrado'),
    )

    renderPagina()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cargar el jugador. Intentá de nuevo.',
    )
  })
})
