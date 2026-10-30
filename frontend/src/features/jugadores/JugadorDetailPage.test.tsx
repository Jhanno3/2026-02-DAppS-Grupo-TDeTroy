import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { JugadorDetailPage } from './JugadorDetailPage'
import { TemaProvider } from '../../app/TemaProvider'
import { CLAVE_TEMA, useTema } from '../../app/tema'
import * as jugadoresApi from '../../api/jugadores'
import * as ofertasApi from '../../api/ofertas'
import * as subastasApi from '../../api/subastas'
import type { CotizacionResponse } from '../../api/jugadores'
import * as authContext from '../auth/AuthContext'

vi.mock('../../api/jugadores')
vi.mock('../../api/ofertas')
vi.mock('../../api/subastas')
vi.mock('../auth/AuthContext', async (importOriginal) => {
  const actual = await importOriginal<typeof authContext>()
  return { ...actual, useAuth: vi.fn() }
})

const JUGADOR = {
  id: 'j1',
  nombre: 'Matteo Politano',
  club: 'SSC Napoli',
  posicion: 'Delantero',
  fechaNacimiento: '1993-08-03',
  nacionalidad: 'Italy',
  liga: 'Serie A',
  estado: 'ACTIVO' as const,
  tokensDisponibles: 75,
  fechaUltimaActualizacionRendimiento: '2026-10-29T19:43:00Z',
  fotoUrl: null,
}

function cotizacion(semana: string, valor: string): CotizacionResponse {
  return {
    id: semana,
    semana,
    valor,
    origen: 'AUTOMATICO',
    fechaCalculo: '2026-10-29T00:00:00Z',
  }
}

/** Muestra el tema vigente, para comprobar que el botón de la ficha lo cambia. */
function TemaActual() {
  return <span data-testid="tema">{useTema().tema}</span>
}

function renderFicha() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <TemaProvider>
        <TemaActual />
        <MemoryRouter initialEntries={['/jugadores/j1']}>
          <Routes>
            <Route path="/jugadores/:id" element={<JugadorDetailPage />} />
          </Routes>
        </MemoryRouter>
      </TemaProvider>
    </QueryClientProvider>,
  )
}

describe('JugadorDetailPage', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: null,
      user: null,
      isAuthenticated: false,
      login: vi.fn(),
      logout: vi.fn(),
    })
    vi.mocked(jugadoresApi.obtenerJugador).mockResolvedValue(JUGADOR)
    vi.mocked(jugadoresApi.listarCotizaciones).mockResolvedValue([
      cotizacion('2026-W43', '100.00'),
      cotizacion('2026-W44', '104.39'),
    ])
    vi.mocked(jugadoresApi.obtenerEstadisticas).mockResolvedValue({
      partidosJugados: 9,
      minutosJugados: 503,
      goles: 2,
      asistencias: 3,
      figuraDelPartido: 0,
      tarjetasAmarillas: 1,
      tarjetasRojas: 0,
      fechaActualizacion: '2026-10-29T19:43:00Z',
    })
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([])
    vi.mocked(subastasApi.listarSubastas).mockResolvedValue([])
  })

  it('el encabezado muestra identidad, cotización vigente con su variación semanal y tokens disponibles', async () => {
    renderFicha()

    expect(
      await screen.findByRole('heading', { name: 'Matteo Politano' }),
    ).toBeInTheDocument()
    expect(screen.getByText('● ACTIVO')).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: /Serie A \/ Politano/ }),
    ).toHaveAttribute('href', '/mercado')

    const cotizacionBloque = await screen.findByLabelText('Cotización')
    expect(cotizacionBloque).toHaveTextContent('$ 104,39')
    // (104,39 − 100) / 100 = +4,39% → redondeado a un decimal
    expect(cotizacionBloque).toHaveTextContent(
      '2026-W44 · var. +4,4% vs 2026-W43',
    )
    expect(screen.getByText('75')).toBeInTheDocument()
  })

  it('con una sola semana, avisa que es la primera', async () => {
    vi.mocked(jugadoresApi.listarCotizaciones).mockResolvedValue([
      cotizacion('2026-W44', '104.39'),
    ])
    renderFicha()

    expect(
      await screen.findByText('2026-W44 · var. — (1.ª semana)'),
    ).toBeInTheDocument()
  })

  it('el rendimiento muestra las 7 estadísticas y las derivadas', async () => {
    renderFicha()

    const rendimiento = await screen.findByRole('region', {
      name: 'Rendimiento de temporada',
    })
    expect(await within(rendimiento).findByText('503')).toBeInTheDocument()
    // 503 / 9 = 55,9 → 56 ; G+A = 5 ; 503 / 5 = 100,6 → 101
    expect(rendimiento).toHaveTextContent('Min / partido 56')
    expect(rendimiento).toHaveTextContent('Goles + asist. 5')
    expect(rendimiento).toHaveTextContent('Min por G+A 101')
  })

  it('el selector de rango acota el historial a las últimas semanas', async () => {
    vi.mocked(jugadoresApi.listarCotizaciones).mockResolvedValue(
      Array.from({ length: 6 }, (_, i) =>
        cotizacion(`2026-W${39 + i}`, String(100 + i)),
      ),
    )
    const user = userEvent.setup()
    renderFicha()

    expect(
      await screen.findByRole('img', { name: /de 6 semanas/ }),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '4S' }))

    expect(screen.getByRole('button', { name: '4S' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
    expect(
      screen.getByRole('img', { name: /de 4 semanas/ }),
    ).toBeInTheDocument()
  })

  it('sin ofertas ni subastas, las tarjetas laterales lo dicen y ofrecen operar', async () => {
    renderFicha()

    expect(
      await screen.findByText(
        'Todavía no hay ofertas entre usuarios para este jugador.',
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Publicar oferta →' }),
    ).toHaveAttribute('href', '/jugadores/j1/p2p')
    expect(screen.getByRole('link', { name: 'Subastar →' })).toHaveAttribute(
      'href',
      '/jugadores/j1/subasta',
    )
  })

  it('el botón de tema alterna entre claro y oscuro y recuerda la elección', async () => {
    const user = userEvent.setup()
    renderFicha()

    // jsdom no informa preferencia de sistema: arranca en oscuro.
    expect(screen.getByTestId('tema')).toHaveTextContent('oscuro')

    await user.click(
      await screen.findByRole('button', { name: 'Cambiar a modo claro' }),
    )

    expect(screen.getByTestId('tema')).toHaveTextContent('claro')
    expect(localStorage.getItem(CLAVE_TEMA)).toBe('claro')
    expect(
      screen.getByRole('button', { name: 'Cambiar a modo oscuro' }),
    ).toBeInTheDocument()
  })
  it('con ofertas y subastas en curso, resume la mejor oferta y lista las subastas', async () => {
    const oferta = {
      vendedorId: 'v1',
      jugadorId: 'j1',
      cantidadPublicada: 2,
      cantidadDisponible: 2,
      estado: 'ABIERTA' as const,
      fechaCreacion: '2026-10-29T00:00:00Z',
      fechaVencimiento: '2026-10-30T00:00:00Z',
    }
    vi.mocked(ofertasApi.listarOfertas).mockResolvedValue([
      { ...oferta, id: 'o1', precioUnitario: '120.00' },
      { ...oferta, id: 'o2', precioUnitario: '99.50' },
    ])
    vi.mocked(subastasApi.listarSubastas).mockResolvedValue([
      {
        id: 's1',
        vendedorId: 'v1',
        jugadorId: 'j1',
        cantidad: 3,
        precioInicial: '100.00',
        precioActual: '115.00',
        mejorPostorId: 'p1',
        cantidadPujas: 2,
        estado: 'ACTIVA',
        fechaCreacion: '2026-10-29T00:00:00Z',
        fechaVencimiento: '2026-10-29T01:00:00Z',
        cierraEn: '2026-10-29T00:10:00Z',
        opcionesPuja: [],
        pujas: null,
      },
    ])
    renderFicha()

    expect(
      await screen.findByText('2 ofertas abiertas · desde $ 99,50 por token.'),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Ver ofertas →' }),
    ).toBeInTheDocument()
    expect(
      await screen.findByRole('link', { name: /3 token\(s\) · 2 pujas/ }),
    ).toHaveAttribute('href', '/subastas/s1')
  })

  it('un jugador dado de baja no ofrece publicar ni subastar', async () => {
    vi.mocked(jugadoresApi.obtenerJugador).mockResolvedValue({
      ...JUGADOR,
      estado: 'INACTIVO',
    })
    renderFicha()

    expect(await screen.findByText('● INACTIVO')).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: 'Subastar →' }),
    ).not.toBeInTheDocument()
  })
})
