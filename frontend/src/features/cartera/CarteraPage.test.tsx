import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CarteraPage } from './CarteraPage'
import * as portfolioApi from '../../api/portfolio'
import * as jugadoresApi from '../../api/jugadores'
import * as movimientosApi from '../../api/movimientos'
import * as ofertasApi from '../../api/ofertas'
import * as subastasApi from '../../api/subastas'
import * as authContext from '../auth/AuthContext'

vi.mock('../../api/portfolio')
vi.mock('../../api/jugadores')
vi.mock('../../api/movimientos')
vi.mock('../../api/ofertas')
vi.mock('../../api/subastas')
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
    vi.mocked(movimientosApi.listarMisMovimientos).mockResolvedValue([])
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([])
    vi.mocked(subastasApi.listarSubastas).mockResolvedValue([])
  })

  it('cartera vacía, muestra el mensaje con link al mercado', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '0.00',
      tenencias: [],
      valorEnVenta: '0.00',
      saldoEnPujas: '0.00',
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
      valorEnVenta: '0.00',
      saldoEnPujas: '0.00',
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
    expect(screen.getByText(/Inter Miami CF/)).toBeInTheDocument()
  })

  it('calcula el costo promedio de compra ponderado por cantidad, ignorando ventas/recargas/compensaciones', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '0.00',
      tenencias: [
        {
          jugadorId: 'j1',
          cantidad: 10,
          cotizacionVigente: '150.00',
          valor: '1500.00',
        },
      ],
      valorEnVenta: '0.00',
      saldoEnPujas: '0.00',
      valorTotal: '1500.00',
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
    vi.mocked(movimientosApi.listarMisMovimientos).mockResolvedValue([
      // (5 × 100) + (5 × 120) = 1100 / 10 = costo promedio $110,00 -- la venta, la recarga y
      // la compensación no cuentan para el promedio de compra.
      {
        id: 'm1',
        jugadorId: 'j1',
        tipo: 'COMPRA_SISTEMA',
        cantidad: 5,
        precioUnitario: '100.00',
        montoTotal: '500.00',
        contraparteUsuarioId: null,
        fecha: '2026-02-01T00:00:00Z',
      },
      {
        id: 'm2',
        jugadorId: 'j1',
        tipo: 'COMPRA_P2P',
        cantidad: 5,
        precioUnitario: '120.00',
        montoTotal: '600.00',
        contraparteUsuarioId: 'u2',
        fecha: '2026-02-02T00:00:00Z',
      },
      {
        id: 'm3',
        jugadorId: 'j1',
        tipo: 'VENTA_SISTEMA',
        cantidad: 2,
        precioUnitario: '140.00',
        montoTotal: '280.00',
        contraparteUsuarioId: null,
        fecha: '2026-02-03T00:00:00Z',
      },
      {
        id: 'm4',
        jugadorId: 'j1',
        tipo: 'RECARGA_SALDO',
        cantidad: 0,
        precioUnitario: '0',
        montoTotal: '1000.00',
        contraparteUsuarioId: null,
        fecha: '2026-02-04T00:00:00Z',
      },
    ])

    renderCartera()

    expect(await screen.findByText('$ 110,00')).toBeInTheDocument()
  })

  it('sin compras registradas para el jugador, muestra "—" en vez de un costo promedio', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '0.00',
      tenencias: [
        {
          jugadorId: 'j1',
          cantidad: 10,
          cotizacionVigente: '150.00',
          valor: '1500.00',
        },
      ],
      valorEnVenta: '0.00',
      saldoEnPujas: '0.00',
      valorTotal: '1500.00',
    })
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([])
    vi.mocked(movimientosApi.listarMisMovimientos).mockResolvedValue([])

    renderCartera()

    expect(
      await screen.findByText('Costo promedio de compra'),
    ).toBeInTheDocument()
    expect(screen.getByText('—')).toBeInTheDocument()
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
      valorEnVenta: '0.00',
      saldoEnPujas: '0.00',
      valorTotal: '100.00',
    })
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([])

    renderCartera()

    expect(await screen.findByText('Jugador no disponible')).toBeInTheDocument()
  })
})

describe('CarteraPage — En venta en P2P', () => {
  const OFERTA_PROPIA = {
    id: 'o1',
    vendedorId: 'u1',
    jugadorId: 'j1',
    cantidadPublicada: 2,
    cantidadDisponible: 1,
    precioUnitario: '120.00',
    estado: 'ABIERTA' as const,
    fechaCreacion: '2026-10-29T00:00:00Z',
    fechaVencimiento: '2026-10-30T00:00:00Z',
  }

  beforeEach(() => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: 'fake-token',
      user: { usuarioId: 'u1', email: 'user@example.com', rol: 'USER' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    })
    vi.mocked(movimientosApi.listarMisMovimientos).mockResolvedValue([])
    vi.mocked(subastasApi.listarSubastas).mockResolvedValue([])
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '500.00',
      tenencias: [],
      valorEnVenta: '104.39',
      saldoEnPujas: '0.00',
      valorTotal: '604.39',
    })
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([
      {
        id: 'j1',
        nombre: 'Matteo Politano',
        club: 'SSC Napoli',
        posicion: 'Delantero',
        fechaNacimiento: '1993-08-03',
        nacionalidad: 'Italy',
        liga: 'Serie A',
        estado: 'ACTIVO',
        tokensDisponibles: 99,
        fechaUltimaActualizacionRendimiento: null,
        fotoUrl: null,
      },
    ])
  })

  it('lista sólo mis ofertas abiertas y muestra el valor en venta como KPI', async () => {
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([
      OFERTA_PROPIA,
      { ...OFERTA_PROPIA, id: 'o2', vendedorId: 'otro' },
    ])

    renderCartera()

    const seccion = await screen.findByRole('region', {
      name: 'En venta en P2P',
    })
    const filas = within(seccion).getAllByRole('listitem')
    expect(filas).toHaveLength(1)
    expect(filas[0]).toHaveTextContent('Matteo Politano')
    expect(filas[0]).toHaveTextContent('1 / 2 token(s)')
    expect(filas[0]).toHaveTextContent('$ 120,00 / token')
    expect(screen.getByText('$ 104,39')).toBeInTheDocument()
    expect(screen.getByText('$ 604,39')).toBeInTheDocument()
    expect(
      screen.getByText(/todos los que tenés están en venta en P2P/),
    ).toBeInTheDocument()
  })

  it('cancelar una oferta llama a la API con su id', async () => {
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([OFERTA_PROPIA])
    vi.mocked(ofertasApi.cancelarOferta).mockResolvedValue(undefined)
    const user = userEvent.setup()

    renderCartera()

    await user.click(await screen.findByRole('button', { name: 'Cancelar' }))

    expect(ofertasApi.cancelarOferta).toHaveBeenCalledWith('o1', 'fake-token')
  })

  it('sin ofertas propias abiertas, no muestra la sección', async () => {
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([
      { ...OFERTA_PROPIA, vendedorId: 'otro' },
    ])

    renderCartera()

    expect(
      await screen.findByText(/Todavía no tenés tokens/),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('region', { name: 'En venta en P2P' }),
    ).not.toBeInTheDocument()
  })
})

describe('CarteraPage — Subastas en curso', () => {
  beforeEach(() => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: 'fake-token',
      user: { usuarioId: 'u1', email: 'user@example.com', rol: 'USER' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    })
    vi.mocked(movimientosApi.listarMisMovimientos).mockResolvedValue([])
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([])
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([])
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '780.00',
      tenencias: [],
      valorEnVenta: '0.00',
      saldoEnPujas: '220.00',
      valorTotal: '1000.00',
    })
  })

  it('muestra el saldo reservado en pujas y sólo las subastas que me involucran', async () => {
    const base = {
      jugadorId: 'j1',
      cantidad: 2,
      precioInicial: '100.00',
      precioActual: '110.00',
      cantidadPujas: 1,
      estado: 'ACTIVA' as const,
      fechaCreacion: '2026-10-29T00:00:00Z',
      fechaVencimiento: '2026-10-29T01:00:00Z',
      cierraEn: '2026-10-29T00:10:00Z',
      opcionesPuja: [],
      pujas: null,
    }
    vi.mocked(subastasApi.listarSubastas).mockResolvedValue([
      { ...base, id: 's1', vendedorId: 'otro', mejorPostorId: 'u1' },
      { ...base, id: 's2', vendedorId: 'otro', mejorPostorId: 'tercero' },
    ])

    renderCartera()

    const seccion = await screen.findByRole('region', {
      name: 'Subastas en curso',
    })
    const filas = within(seccion).getAllByRole('listitem')
    expect(filas).toHaveLength(1)
    expect(filas[0]).toHaveTextContent('Vas ganando')
    expect(filas[0]).toHaveTextContent('$ 110,00 / token')
    expect(screen.getByText('$ 220,00')).toBeInTheDocument()
  })
})
