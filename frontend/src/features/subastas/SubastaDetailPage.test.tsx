import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SubastaDetailPage } from './SubastaDetailPage'
import { ApiError } from '../../api/client'
import * as jugadoresApi from '../../api/jugadores'
import * as subastasApi from '../../api/subastas'
import type { SubastaResponse } from '../../api/subastas'
import * as authContext from '../auth/AuthContext'

vi.mock('../../api/jugadores')
vi.mock('../../api/subastas')
vi.mock('../auth/AuthContext', async (importOriginal) => {
  const actual = await importOriginal<typeof authContext>()
  return { ...actual, useAuth: vi.fn() }
})

const EN_10_MINUTOS = new Date(Date.now() + 10 * 60 * 1000).toISOString()

function subasta(overrides: Partial<SubastaResponse> = {}): SubastaResponse {
  return {
    id: 's1',
    vendedorId: 'vendedor',
    jugadorId: 'j1',
    cantidad: 2,
    precioInicial: '100.00',
    precioActual: null,
    mejorPostorId: null,
    cantidadPujas: 0,
    estado: 'ACTIVA',
    fechaCreacion: '2026-10-29T20:00:00Z',
    fechaVencimiento: EN_10_MINUTOS,
    cierraEn: EN_10_MINUTOS,
    opcionesPuja: [
      { porcentaje: 0, precioUnitario: '100.00', montoTotal: '200.00' },
      { porcentaje: 5, precioUnitario: '105.00', montoTotal: '210.00' },
      { porcentaje: 10, precioUnitario: '110.00', montoTotal: '220.00' },
      { porcentaje: 20, precioUnitario: '120.00', montoTotal: '240.00' },
    ],
    pujas: [],
    ...overrides,
  }
}

function comoUsuario(usuarioId: string | null) {
  vi.mocked(authContext.useAuth).mockReturnValue({
    token: usuarioId ? 'fake-token' : null,
    user: usuarioId
      ? { usuarioId, email: 'user@example.com', rol: 'USER' }
      : null,
    isAuthenticated: usuarioId !== null,
    login: vi.fn(),
    logout: vi.fn(),
  })
}

function renderPagina() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/subastas/s1']}>
        <Routes>
          <Route path="/subastas/:id" element={<SubastaDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('SubastaDetailPage', () => {
  beforeEach(() => {
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
  })

  it('un postor ve las opciones con el precio que calcula el backend y puja con el porcentaje elegido', async () => {
    comoUsuario('postor')
    vi.mocked(subastasApi.obtenerSubasta).mockResolvedValue(subasta())
    vi.mocked(subastasApi.pujar).mockResolvedValue(subasta())
    const user = userEvent.setup()
    renderPagina()

    const opcion = await screen.findByRole('button', { name: /\+10%/ })
    expect(opcion).toHaveTextContent('$ 110,00')
    expect(opcion).toHaveTextContent('Total $ 220,00')
    expect(
      screen.getByRole('button', { name: /Precio inicial/ }),
    ).toBeInTheDocument()

    await user.click(opcion)

    expect(subastasApi.pujar).toHaveBeenCalledWith('s1', 10, 'fake-token')
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Puja registrada',
    )
  })

  it('el vendedor no ve botones de puja, sólo la opción de cancelar', async () => {
    comoUsuario('vendedor')
    vi.mocked(subastasApi.obtenerSubasta).mockResolvedValue(subasta())
    renderPagina()

    expect(
      await screen.findByRole('button', { name: 'Cancelar subasta' }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /\+5%/ }),
    ).not.toBeInTheDocument()
  })

  it('el mejor postor ve que va ganando y no puede volver a pujar', async () => {
    comoUsuario('postor')
    vi.mocked(subastasApi.obtenerSubasta).mockResolvedValue(
      subasta({
        precioActual: '105.00',
        mejorPostorId: 'postor',
        cantidadPujas: 1,
        pujas: [
          {
            postorId: 'postor',
            precioUnitario: '105.00',
            fecha: '2026-10-29T20:01:00Z',
          },
        ],
      }),
    )
    renderPagina()

    expect(await screen.findByText(/Vas ganando/)).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /\+5%/ }),
    ).not.toBeInTheDocument()
    // El historial muestra "Vos" para las pujas propias.
    expect(screen.getAllByText('Vos').length).toBeGreaterThan(0)
  })

  it('sin sesión, invita a iniciar sesión para pujar', async () => {
    comoUsuario(null)
    vi.mocked(subastasApi.obtenerSubasta).mockResolvedValue(subasta())
    renderPagina()

    expect(
      await screen.findByRole('link', { name: 'Iniciá sesión' }),
    ).toBeInTheDocument()
  })

  it('adjudicada al usuario, muestra que ganó con el total pagado', async () => {
    comoUsuario('postor')
    vi.mocked(subastasApi.obtenerSubasta).mockResolvedValue(
      subasta({
        estado: 'ADJUDICADA',
        precioActual: '120.00',
        mejorPostorId: 'postor',
        cantidadPujas: 3,
        opcionesPuja: [],
      }),
    )
    renderPagina()

    expect(
      await screen.findByText(
        '¡Ganaste la subasta! Recibiste 2 token(s) por $ 240,00.',
      ),
    ).toBeInTheDocument()
  })
  it('el vendedor cancela tras confirmar y ve el aviso', async () => {
    comoUsuario('vendedor')
    vi.mocked(subastasApi.obtenerSubasta).mockResolvedValue(subasta())
    vi.mocked(subastasApi.cancelarSubasta).mockResolvedValue(undefined)
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const user = userEvent.setup()
    renderPagina()

    await user.click(
      await screen.findByRole('button', { name: 'Cancelar subasta' }),
    )

    expect(subastasApi.cancelarSubasta).toHaveBeenCalledWith('s1', 'fake-token')
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Subasta cancelada',
    )
  })

  it('si el vendedor no confirma, no se cancela nada', async () => {
    comoUsuario('vendedor')
    vi.mocked(subastasApi.obtenerSubasta).mockResolvedValue(subasta())
    vi.mocked(subastasApi.cancelarSubasta).mockClear()
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const user = userEvent.setup()
    renderPagina()

    await user.click(
      await screen.findByRole('button', { name: 'Cancelar subasta' }),
    )

    expect(subastasApi.cancelarSubasta).not.toHaveBeenCalled()
  })

  it.each([
    [
      'vendedor',
      {
        estado: 'ADJUDICADA' as const,
        precioActual: '110.00',
        mejorPostorId: 'postor',
      },
      'Vendiste 2 token(s) por $ 220,00.',
    ],
    [
      'tercero',
      {
        estado: 'ADJUDICADA' as const,
        precioActual: '110.00',
        mejorPostorId: 'postor',
      },
      'Adjudicada a $ 110,00 por token.',
    ],
    [
      'tercero',
      { estado: 'DESIERTA' as const },
      'Venció sin pujas: los tokens volvieron al vendedor.',
    ],
    [
      'tercero',
      { estado: 'CANCELADA' as const },
      'El vendedor canceló la subasta.',
    ],
  ])(
    'cerrada, %s ve el resultado que le corresponde',
    async (usuario, cambios, mensaje) => {
      comoUsuario(usuario)
      vi.mocked(subastasApi.obtenerSubasta).mockResolvedValue(
        subasta({ ...cambios, opcionesPuja: [] }),
      )
      renderPagina()

      expect(await screen.findByText(mensaje)).toBeInTheDocument()
    },
  )

  it('si la subasta no existe, lo dice', async () => {
    comoUsuario(null)
    vi.mocked(subastasApi.obtenerSubasta).mockRejectedValue(
      new ApiError(404, 'No existe', 'Not Found'),
    )
    renderPagina()

    expect(
      await screen.findByText('No existe esa subasta.'),
    ).toBeInTheDocument()
  })

  it('si la puja falla, muestra el mensaje del backend', async () => {
    comoUsuario('postor')
    vi.mocked(subastasApi.obtenerSubasta).mockResolvedValue(subasta())
    vi.mocked(subastasApi.pujar).mockRejectedValue(
      new ApiError(409, 'Saldo insuficiente', 'Conflict'),
    )
    const user = userEvent.setup()
    renderPagina()

    await user.click(await screen.findByRole('button', { name: /\+5%/ }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Saldo insuficiente',
    )
  })
})
