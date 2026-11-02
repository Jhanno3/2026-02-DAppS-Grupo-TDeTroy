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
      gananciaPerdida: '0.00',
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
          precioPromedioCompra: '100.00',
          gananciaPerdida: '0.00',
          rentabilidad: '0.00',
        },
      ],
      valorEnVenta: '0.00',
      saldoEnPujas: '0.00',
      gananciaPerdida: '0.00',
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

  it('muestra el precio promedio y la ganancia/pérdida que calcula el backend', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '0.00',
      tenencias: [
        {
          jugadorId: 'j1',
          cantidad: 10,
          cotizacionVigente: '120.00',
          valor: '1200.00',
          precioPromedioCompra: '100.00',
          gananciaPerdida: '200.00',
          rentabilidad: '20.00',
        },
        {
          jugadorId: 'j2',
          cantidad: 10,
          cotizacionVigente: '90.00',
          valor: '900.00',
          precioPromedioCompra: '100.00',
          gananciaPerdida: '-100.00',
          rentabilidad: '-10.00',
        },
      ],
      valorEnVenta: '0.00',
      saldoEnPujas: '0.00',
      gananciaPerdida: '100.00',
      valorTotal: '2100.00',
    })
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([])

    renderCartera()

    expect(await screen.findByText('+ $ 100,00')).toBeInTheDocument()
    expect(screen.getAllByText('$ 100,00')).toHaveLength(2)
    expect(screen.getByText(/\+ \$ 200,00 \(\+20,00 %\)/)).toBeInTheDocument()
    expect(screen.getByText(/- \$ 100,00 \(-10,00 %\)/)).toBeInTheDocument()
  })

  it('acepta los montos como números JSON, que es como los serializa el backend', async () => {
    // Jackson manda BigDecimal como número (`0.00` → 0), no como string.
    const n = (valor: number) => valor as unknown as string
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: n(0),
      tenencias: [
        {
          jugadorId: 'j1',
          cantidad: 10,
          cotizacionVigente: n(90),
          valor: n(900),
          precioPromedioCompra: n(100),
          gananciaPerdida: n(-100),
          rentabilidad: n(-10),
        },
      ],
      valorEnVenta: n(0),
      saldoEnPujas: n(0),
      gananciaPerdida: n(-100),
      valorTotal: n(900),
    })
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([])

    renderCartera()

    expect(await screen.findByText('- $ 100,00')).toBeInTheDocument()
    expect(screen.getByText(/- \$ 100,00 \(-10,00 %\)/)).toBeInTheDocument()
  })

  it('no pide el historial de movimientos para calcular nada', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '0.00',
      tenencias: [],
      valorEnVenta: '0.00',
      saldoEnPujas: '0.00',
      gananciaPerdida: '0.00',
      valorTotal: '0.00',
    })
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue([])

    renderCartera()

    await screen.findByText(/Todavía no tenés tokens/)
    expect(movimientosApi.listarMisMovimientos).not.toHaveBeenCalled()
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
          precioPromedioCompra: '100.00',
          gananciaPerdida: '0.00',
          rentabilidad: '0.00',
        },
      ],
      valorEnVenta: '0.00',
      saldoEnPujas: '0.00',
      gananciaPerdida: '0.00',
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
      gananciaPerdida: '0.00',
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
      gananciaPerdida: '0.00',
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
