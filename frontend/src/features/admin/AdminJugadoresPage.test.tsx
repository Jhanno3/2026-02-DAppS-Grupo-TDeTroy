import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AdminJugadoresPage } from './AdminJugadoresPage'
import * as jugadoresApi from '../../api/jugadores'
import * as cotizacionesApi from '../../api/cotizaciones'
import * as authContext from '../auth/AuthContext'
import { ApiError } from '../../api/client'

vi.mock('../../api/jugadores')
vi.mock('../../api/cotizaciones')
vi.mock('../auth/AuthContext')

const JUGADOR_ACTIVO: jugadoresApi.JugadorResponse = {
  id: 'j1',
  nombre: 'Lionel Messi',
  club: 'Inter Miami',
  posicion: 'Delantero',
  fechaNacimiento: '1987-06-24',
  nacionalidad: 'Argentina',
  liga: 'MLS',
  estado: 'ACTIVO',
  tokensDisponibles: 100,
  fechaUltimaActualizacionRendimiento: null,
  fotoUrl: null,
}

const JUGADOR_INACTIVO: jugadoresApi.JugadorResponse = {
  id: 'j2',
  nombre: 'Jugador Retirado',
  club: 'Sin Club',
  posicion: 'Mediocampista',
  fechaNacimiento: '1980-01-01',
  nacionalidad: 'Argentina',
  liga: 'MLS',
  estado: 'INACTIVO',
  tokensDisponibles: 0,
  fechaUltimaActualizacionRendimiento: null,
  fotoUrl: null,
}

const JUGADORES_MOCK: jugadoresApi.JugadorResponse[] = [
  JUGADOR_ACTIVO,
  JUGADOR_INACTIVO,
]

function renderPagina() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AdminJugadoresPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AdminJugadoresPage', () => {
  beforeEach(() => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: 'admin-jwt-token',
      user: { usuarioId: 'u-admin', email: 'admin@example.com', rol: 'ADMIN' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    })
    vi.mocked(jugadoresApi.listarJugadores).mockResolvedValue(JUGADORES_MOCK)
    vi.mocked(cotizacionesApi.dispararIngestaRendimiento).mockResolvedValue(
      undefined,
    )
    vi.mocked(cotizacionesApi.dispararRecalculoManual).mockResolvedValue(
      undefined,
    )
    vi.mocked(jugadoresApi.darDeBajaJugador).mockResolvedValue(JUGADOR_INACTIVO)
  })

  it('renderiza la lista de jugadores con datos y botones de acción', async () => {
    renderPagina()

    expect(await screen.findByText('Lionel Messi')).toBeInTheDocument()
    expect(screen.getByText('Jugador Retirado')).toBeInTheDocument()
    expect(screen.getByText('ACTIVO')).toBeInTheDocument()
    expect(screen.getByText('INACTIVO')).toBeInTheDocument()

    // Solo el jugador ACTIVO tiene botón "Dar de baja"
    const botonesBaja = screen.getAllByRole('button', { name: 'Dar de baja' })
    expect(botonesBaja).toHaveLength(1)
  })

  it('permite sincronizar WhoScored y muestra mensaje de éxito', async () => {
    const user = userEvent.setup()
    renderPagina()

    const botonSync = await screen.findByRole('button', {
      name: /Sincronizar WhoScored/i,
    })
    await user.click(botonSync)

    expect(cotizacionesApi.dispararIngestaRendimiento).toHaveBeenCalledWith(
      'admin-jwt-token',
    )
    expect(
      await screen.findByText(
        'Sincronización con WhoScored completada con éxito.',
      ),
    ).toBeInTheDocument()
  })

  it('muestra mensaje de error si falla la sincronización con WhoScored', async () => {
    vi.mocked(cotizacionesApi.dispararIngestaRendimiento).mockRejectedValueOnce(
      new ApiError(502, 'Error al contactar WhoScored', undefined),
    )
    const user = userEvent.setup()
    renderPagina()

    const botonSync = await screen.findByRole('button', {
      name: /Sincronizar WhoScored/i,
    })
    await user.click(botonSync)

    expect(
      await screen.findByText('Error al contactar WhoScored'),
    ).toBeInTheDocument()
  })

  it('permite disparar el recálculo manual y muestra confirmación', async () => {
    const user = userEvent.setup()
    renderPagina()

    const botonRecalculo = await screen.findByRole('button', {
      name: 'Disparar recálculo',
    })
    await user.click(botonRecalculo)

    expect(cotizacionesApi.dispararRecalculoManual).toHaveBeenCalledWith(
      'admin-jwt-token',
    )
    expect(
      await screen.findByText(
        'Recálculo de cotización disparado correctamente.',
      ),
    ).toBeInTheDocument()
  })

  it('muestra mensaje de error si falla el recálculo', async () => {
    vi.mocked(cotizacionesApi.dispararRecalculoManual).mockRejectedValueOnce(
      new ApiError(500, 'Fallo en el recálculo', undefined),
    )
    const user = userEvent.setup()
    renderPagina()

    const botonRecalculo = await screen.findByRole('button', {
      name: 'Disparar recálculo',
    })
    await user.click(botonRecalculo)

    expect(await screen.findByText('Fallo en el recálculo')).toBeInTheDocument()
  })

  it('permite dar de baja un jugador cuando se confirma el diálogo', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const user = userEvent.setup()
    renderPagina()

    const botonBaja = await screen.findByRole('button', {
      name: 'Dar de baja',
    })
    await user.click(botonBaja)

    expect(window.confirm).toHaveBeenCalled()
    expect(jugadoresApi.darDeBajaJugador).toHaveBeenCalledWith(
      'j1',
      'admin-jwt-token',
    )
  })

  it('no da de baja al jugador si se cancela el diálogo de confirmación', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const user = userEvent.setup()
    renderPagina()

    const botonBaja = await screen.findByRole('button', {
      name: 'Dar de baja',
    })
    await user.click(botonBaja)

    expect(window.confirm).toHaveBeenCalled()
    expect(jugadoresApi.darDeBajaJugador).not.toHaveBeenCalled()
  })

  it('muestra mensaje de error si la baja de un jugador falla', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(jugadoresApi.darDeBajaJugador).mockRejectedValueOnce(
      new ApiError(400, 'Error al procesar baja', undefined),
    )
    const user = userEvent.setup()
    renderPagina()

    const botonBaja = await screen.findByRole('button', {
      name: 'Dar de baja',
    })
    await user.click(botonBaja)

    expect(
      await screen.findByText('Error al procesar baja'),
    ).toBeInTheDocument()
  })

  it('muestra mensaje de error si la carga de jugadores falla', async () => {
    vi.mocked(jugadoresApi.listarJugadores).mockRejectedValueOnce(
      new Error('Failed to load'),
    )
    renderPagina()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cargar el catálogo.',
    )
  })
})
