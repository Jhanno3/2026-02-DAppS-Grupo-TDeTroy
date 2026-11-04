import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AdminMetricasPage } from './AdminMetricasPage'
import * as metricasApi from '../../api/metricas'
import * as cotizacionesApi from '../../api/cotizaciones'
import * as authContext from '../auth/AuthContext'
import { ApiError } from '../../api/client'

vi.mock('../../api/metricas')
vi.mock('../../api/cotizaciones')
vi.mock('../auth/AuthContext')

const METRICAS_MOCK: metricasApi.ConfiguracionMetricaResponse[] = [
  {
    clave: 'goals',
    etiqueta: 'Goles',
    sentido: 'POSITIVO',
    activo: true,
    pesoGeneral: 0.5,
    pesosPorPosicion: {
      Delantero: 0.6,
      Centrocampista: 0.5,
      Defensor: 0.8,
      Arquero: 1.0,
    },
  },
  {
    clave: 'yellowCards',
    etiqueta: 'Tarjetas Amarillas',
    sentido: 'NEGATIVO',
    activo: false,
    pesoGeneral: 0.2,
    pesosPorPosicion: {
      Delantero: 0.2,
      Centrocampista: 0.2,
      Defensor: 0.2,
      Arquero: 0.2,
    },
  },
]

function renderPagina() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AdminMetricasPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AdminMetricasPage', () => {
  beforeEach(() => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: 'admin-jwt-token',
      user: { usuarioId: 'u-admin', email: 'admin@example.com', rol: 'ADMIN' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    })
    vi.mocked(metricasApi.obtenerConfiguracionMetricas).mockResolvedValue(
      METRICAS_MOCK,
    )
    vi.mocked(metricasApi.actualizarConfiguracionMetricas).mockResolvedValue(
      METRICAS_MOCK,
    )
    vi.mocked(cotizacionesApi.dispararRecalculoManual).mockResolvedValue(
      undefined,
    )
  })

  it('muestra estado de carga mientras obtiene las métricas', () => {
    vi.mocked(metricasApi.obtenerConfiguracionMetricas).mockReturnValue(
      new Promise(() => {}),
    )
    renderPagina()

    expect(
      screen.getByText('Cargando configuración de métricas…'),
    ).toBeInTheDocument()
  })

  it('muestra mensaje de error si falla la carga', async () => {
    vi.mocked(metricasApi.obtenerConfiguracionMetricas).mockRejectedValue(
      new Error('Network error'),
    )
    renderPagina()

    await waitFor(() => {
      expect(
        screen.getByText('No se pudo cargar la configuración de métricas.'),
      ).toBeInTheDocument()
    })
  })

  it('renderiza la tabla con métricas y sus ponderaciones', async () => {
    renderPagina()

    await waitFor(() => {
      expect(screen.getByText('Goles')).toBeInTheDocument()
      expect(screen.getByText('Tarjetas Amarillas')).toBeInTheDocument()
    })

    expect(screen.getByText('POSITIVO')).toBeInTheDocument()
    expect(screen.getByText('NEGATIVO')).toBeInTheDocument()

    const generalGolesInput = screen.getByLabelText('Peso general Goles')
    expect(generalGolesInput).toHaveValue(0.5)

    const defensorGolesInput = screen.getByLabelText('Peso Defensor Goles')
    expect(defensorGolesInput).toHaveValue(0.8)
  })

  it('guarda exitosamente los cambios de ponderación', async () => {
    const user = userEvent.setup()
    renderPagina()

    await waitFor(() => {
      expect(screen.getByText('Goles')).toBeInTheDocument()
    })

    const generalGolesInput = screen.getByLabelText('Peso general Goles')
    await user.clear(generalGolesInput)
    await user.type(generalGolesInput, '0.75')

    const activoGoles = screen.getByLabelText('Activo Goles')
    await user.click(activoGoles)

    const guardarBtns = screen.getAllByRole('button', {
      name: 'Guardar ponderaciones',
    })
    await user.click(guardarBtns[0])

    await waitFor(() => {
      expect(
        metricasApi.actualizarConfiguracionMetricas,
      ).toHaveBeenCalledWith(
        expect.objectContaining({
          metricas: expect.arrayContaining([
            expect.objectContaining({
              clave: 'goals',
              pesoGeneral: 0.75,
              activo: false,
            }),
          ]),
        }),
        'admin-jwt-token',
      )
    })

    expect(
      screen.getByText(
        'Configuración de métricas y ponderaciones por posición guardada con éxito.',
      ),
    ).toBeInTheDocument()
  })

  it('muestra error de validación si el peso ingresado es negativo o inválido', async () => {
    const user = userEvent.setup()
    renderPagina()

    await waitFor(() => {
      expect(screen.getByText('Goles')).toBeInTheDocument()
    })

    const generalGolesInput = screen.getByLabelText('Peso general Goles')
    await user.clear(generalGolesInput)
    await user.type(generalGolesInput, '-1')

    const guardarBtns = screen.getAllByRole('button', {
      name: 'Guardar ponderaciones',
    })
    await user.click(guardarBtns[0])

    expect(
      screen.getByText(
        'El peso general para "Goles" debe ser un número mayor o igual a 0.',
      ),
    ).toBeInTheDocument()
    expect(metricasApi.actualizarConfiguracionMetricas).not.toHaveBeenCalled()
  })

  it('muestra error si la mutación de guardar falla con ApiError', async () => {
    const user = userEvent.setup()
    vi.mocked(metricasApi.actualizarConfiguracionMetricas).mockRejectedValue(
      new ApiError(400, 'Error de validación del backend', undefined),
    )
    renderPagina()

    await waitFor(() => {
      expect(screen.getByText('Goles')).toBeInTheDocument()
    })

    const guardarBtns = screen.getAllByRole('button', {
      name: 'Guardar ponderaciones',
    })
    await user.click(guardarBtns[0])

    await waitFor(() => {
      expect(
        screen.getByText('Error de validación del backend'),
      ).toBeInTheDocument()
    })
  })

  it('dispara el recálculo manual exitosamente', async () => {
    const user = userEvent.setup()
    renderPagina()

    await waitFor(() => {
      expect(screen.getByText('Goles')).toBeInTheDocument()
    })

    const recalcularBtn = screen.getByRole('button', {
      name: /Disparar recálculo/i,
    })
    await user.click(recalcularBtn)

    await waitFor(() => {
      expect(cotizacionesApi.dispararRecalculoManual).toHaveBeenCalledWith(
        'admin-jwt-token',
      )
    })

    expect(
      screen.getByText(
        'Recálculo manual de cotizaciones ejecutado correctamente con las nuevas ponderaciones.',
      ),
    ).toBeInTheDocument()
  })

  it('muestra error si falla el disparo del recálculo', async () => {
    const user = userEvent.setup()
    vi.mocked(cotizacionesApi.dispararRecalculoManual).mockRejectedValue(
      new ApiError(500, 'Error interno al recalcular', undefined),
    )
    renderPagina()

    await waitFor(() => {
      expect(screen.getByText('Goles')).toBeInTheDocument()
    })

    const recalcularBtn = screen.getByRole('button', {
      name: /Disparar recálculo/i,
    })
    await user.click(recalcularBtn)

    await waitFor(() => {
      expect(screen.getByText('Error interno al recalcular')).toBeInTheDocument()
    })
  })
})

