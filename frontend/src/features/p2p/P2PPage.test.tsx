import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { P2PPage } from './P2PPage'
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

const JUGADOR = {
  id: 'jugador-1',
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

const OFERTA_PROPIA = {
  id: 'o1',
  vendedorId: 'u1',
  jugadorId: 'jugador-1',
  cantidadPublicada: 5,
  cantidadDisponible: 5,
  precioUnitario: '60.00',
  estado: 'ABIERTA' as const,
  fechaCreacion: '2026-10-24T00:00:00Z',
  fechaVencimiento: '2026-10-25T00:00:00Z',
}

const OFERTA_AJENA = { ...OFERTA_PROPIA, id: 'o2', vendedorId: 'otro-usuario' }

function renderPagina() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/p2p']}>
        <Routes>
          <Route path="/p2p" element={<P2PPage />} />
          <Route path="/login" element={<p>Pantalla de login</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('P2PPage', () => {
  beforeEach(() => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: 'fake-token',
      user: { usuarioId: 'u1', email: 'user@example.com', rol: 'USER' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    })
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([JUGADOR])
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([])
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '1000.00',
      tenencias: [],
      valorTotal: '1000.00',
    })
  })

  it('muestra el título y la descripción', async () => {
    renderPagina()

    expect(
      await screen.findByRole('heading', { name: 'P2P' }),
    ).toBeInTheDocument()
  })

  it('sin ofertas abiertas, muestra el mensaje correspondiente', async () => {
    renderPagina()

    expect(
      await screen.findByText('No hay ofertas P2P abiertas en este momento.'),
    ).toBeInTheDocument()
  })

  it('si falla la carga, muestra el mensaje de error', async () => {
    vi.mocked(ofertasApi.listarOfertas).mockRejectedValue(new Error('boom'))

    renderPagina()

    expect(
      await screen.findByText(
        'No se pudieron cargar las ofertas. Intentá de nuevo.',
      ),
    ).toBeInTheDocument()
  })

  it('muestra una oferta con el jugador, precio, cantidades y vencimiento', async () => {
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([OFERTA_AJENA])

    renderPagina()

    expect(await screen.findByText('Erling Haaland')).toBeInTheDocument()
    expect(screen.getByText('Manchester City · Delantero')).toBeInTheDocument()
    expect(screen.getByText('$ 60,00')).toBeInTheDocument()
    expect(screen.getByText('5 / 5 disponibles')).toBeInTheDocument()
  })

  it('en una oferta propia, muestra "Tu oferta" y cancelarla llama a cancelarOferta', async () => {
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([OFERTA_PROPIA])
    vi.mocked(ofertasApi.cancelarOferta).mockResolvedValue(undefined)
    const user = userEvent.setup()
    renderPagina()

    await screen.findByText('Tu oferta')
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(ofertasApi.cancelarOferta).toHaveBeenCalledWith('o1', 'fake-token')
  })

  it('en una oferta ajena, comprar llama a comprarDeOferta con la cantidad elegida', async () => {
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([OFERTA_AJENA])
    vi.mocked(ofertasApi.comprarDeOferta).mockResolvedValue({
      id: 'm1',
      jugadorId: 'jugador-1',
      tipo: 'COMPRA_P2P',
      cantidad: 1,
      precioUnitario: '60.00',
      montoTotal: '60.00',
      contraparteUsuarioId: 'otro-usuario',
      fecha: '2026-10-24T00:00:00Z',
    })
    const user = userEvent.setup()
    renderPagina()

    await user.click(await screen.findByRole('button', { name: 'Comprar' }))

    expect(ofertasApi.comprarDeOferta).toHaveBeenCalledWith(
      'o2',
      1,
      'fake-token',
    )
  })

  it('sin sesión y en una oferta ajena, invita a iniciar sesión en vez de mostrar el form de compra', async () => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: null,
      user: null,
      isAuthenticated: false,
      login: vi.fn(),
      logout: vi.fn(),
    })
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([OFERTA_AJENA])

    renderPagina()

    expect(await screen.findByText(/Iniciá sesión/)).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Comprar' }),
    ).not.toBeInTheDocument()
  })

  it('sin sesión, "Crear nueva oferta" navega a /login', async () => {
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
      await screen.findByRole('button', { name: 'Crear nueva oferta' }),
    )

    expect(await screen.findByText('Pantalla de login')).toBeInTheDocument()
  })

  it('con sesión, "Crear nueva oferta" abre el selector de jugadores', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '1000.00',
      tenencias: [
        {
          jugadorId: 'jugador-1',
          cantidad: 8,
          cotizacionVigente: null,
          valor: '0.00',
        },
      ],
      valorTotal: '1000.00',
    })
    const user = userEvent.setup()
    renderPagina()

    await user.click(
      await screen.findByRole('button', { name: 'Crear nueva oferta' }),
    )

    expect(await screen.findByText('Elegí un jugador')).toBeInTheDocument()
    expect(screen.getByText('8 token(s)')).toBeInTheDocument()
  })

  it('sin tokens de ningún jugador, el selector invita a comprar en el Mercado', async () => {
    const user = userEvent.setup()
    renderPagina()

    await user.click(
      await screen.findByRole('button', { name: 'Crear nueva oferta' }),
    )

    expect(
      await screen.findByText(/No tenés tokens de ningún jugador todavía/),
    ).toBeInTheDocument()
  })

  it('el buscador del selector filtra por nombre, y muestra aviso sin coincidencias', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '1000.00',
      tenencias: [
        {
          jugadorId: 'jugador-1',
          cantidad: 8,
          cotizacionVigente: null,
          valor: '0.00',
        },
      ],
      valorTotal: '1000.00',
    })
    const user = userEvent.setup()
    renderPagina()

    await user.click(
      await screen.findByRole('button', { name: 'Crear nueva oferta' }),
    )
    await screen.findByText('Elegí un jugador')

    await user.type(
      screen.getByRole('searchbox', { name: 'Buscar jugador' }),
      'zzz-no-existe',
    )

    expect(
      await screen.findByText(/Ningún jugador tuyo coincide/),
    ).toBeInTheDocument()
  })

  it('el botón cerrar (X) cierra el selector', async () => {
    const user = userEvent.setup()
    renderPagina()

    await user.click(
      await screen.findByRole('button', { name: 'Crear nueva oferta' }),
    )
    await screen.findByText('Elegí un jugador')

    await user.click(screen.getByRole('button', { name: 'Cerrar' }))

    expect(screen.queryByText('Elegí un jugador')).not.toBeInTheDocument()
  })
})
