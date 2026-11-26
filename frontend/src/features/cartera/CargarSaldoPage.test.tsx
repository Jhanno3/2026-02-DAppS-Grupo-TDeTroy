import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CargarSaldoPage } from './CargarSaldoPage'
import * as usuariosApi from '../../api/usuarios'
import * as authContext from '../auth/AuthContext'
import { ApiError } from '../../api/client'

vi.mock('../../api/usuarios')
vi.mock('../auth/AuthContext')

function renderPagina() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <CargarSaldoPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('CargarSaldoPage', () => {
  beforeEach(() => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: 'user-jwt-token',
      usuario: {
        usuarioId: 'u-1',
        email: 'user@example.com',
        rol: 'USER',
      },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    })
  })

  it('procesa la carga de saldo con datos de tarjeta ficticios y muestra saldo actualizado', async () => {
    vi.mocked(usuariosApi.cargarSaldoPropio).mockResolvedValue({
      id: 'u-1',
      email: 'user@example.com',
      rol: 'USER',
      saldoVirtual: '350.00',
      fechaCreacion: '2026-11-26T00:00:00Z',
    })

    const user = userEvent.setup()
    renderPagina()

    await user.type(screen.getByLabelText(/Monto a cargar/i), '350')
    await user.type(
      screen.getByLabelText(/Número de tarjeta/i),
      '4532 1234 5678 9010',
    )
    await user.type(
      screen.getByLabelText(/Titular de la tarjeta/i),
      'Juan Perez',
    )
    await user.type(screen.getByLabelText(/Vencimiento/i), '12/30')
    await user.type(screen.getByLabelText(/Código de seguridad/i), '123')
    await user.click(screen.getByRole('button', { name: 'Cargar saldo' }))

    expect(usuariosApi.cargarSaldoPropio).toHaveBeenCalledWith(
      {
        monto: 350,
        numeroTarjeta: '4532123456789010',
        titular: 'Juan Perez',
        vencimiento: '12/30',
        codigoSeguridad: '123',
      },
      'user-jwt-token',
    )
    expect(await screen.findByRole('status')).toHaveTextContent(
      /Pago aprobado\. Saldo actualizado/i,
    )
  })

  it('muestra error de validación si el número de tarjeta no tiene 16 dígitos', async () => {
    const user = userEvent.setup()
    renderPagina()

    await user.type(screen.getByLabelText(/Monto a cargar/i), '100')
    await user.type(screen.getByLabelText(/Número de tarjeta/i), '1234')
    await user.type(
      screen.getByLabelText(/Titular de la tarjeta/i),
      'Juan Perez',
    )
    await user.type(screen.getByLabelText(/Vencimiento/i), '12/30')
    await user.type(screen.getByLabelText(/Código de seguridad/i), '123')
    await user.click(screen.getByRole('button', { name: 'Cargar saldo' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El número de tarjeta debe tener 16 dígitos.',
    )
    expect(usuariosApi.cargarSaldoPropio).not.toHaveBeenCalled()
  })

  it('muestra el mensaje de error si el backend rechaza el pago', async () => {
    vi.mocked(usuariosApi.cargarSaldoPropio).mockRejectedValueOnce(
      new ApiError(
        409,
        'La tarjeta ingresada se encuentra vencida.',
        undefined,
      ),
    )

    const user = userEvent.setup()
    renderPagina()

    await user.type(screen.getByLabelText(/Monto a cargar/i), '100')
    await user.type(
      screen.getByLabelText(/Número de tarjeta/i),
      '4532123456789010',
    )
    await user.type(
      screen.getByLabelText(/Titular de la tarjeta/i),
      'Juan Perez',
    )
    await user.type(screen.getByLabelText(/Vencimiento/i), '01/20')
    await user.type(screen.getByLabelText(/Código de seguridad/i), '123')
    await user.click(screen.getByRole('button', { name: 'Cargar saldo' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La tarjeta ingresada se encuentra vencida.',
    )
  })
})
