import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { TradePanel } from './TradePanel'
import { ApiError } from '../../api/client'
import * as tokensApi from '../../api/tokens'
import * as portfolioApi from '../../api/portfolio'
import * as authContext from '../auth/AuthContext'

vi.mock('../../api/tokens')
vi.mock('../../api/portfolio')
vi.mock('../auth/AuthContext', async (importOriginal) => {
  const actual = await importOriginal<typeof authContext>()
  return { ...actual, useAuth: vi.fn() }
})

const JUGADOR_ID = 'jugador-1'

function renderTradePanel(
  props: Partial<React.ComponentProps<typeof TradePanel>> = {},
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <TradePanel
          jugadorId={JUGADOR_ID}
          cotizacionVigente={100}
          tokensDisponibles={50}
          estaActivo
          {...props}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('TradePanel', () => {
  beforeEach(() => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: 'fake-token',
      user: { usuarioId: 'u1', email: 'user@example.com', rol: 'USER' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    })
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '1000.00',
      tenencias: [],
      valorTotal: '0.00',
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('sin sesión, invita a iniciar sesión o crear cuenta en vez de mostrar el form', () => {
    vi.mocked(authContext.useAuth).mockReturnValue({
      token: null,
      user: null,
      isAuthenticated: false,
      login: vi.fn(),
      logout: vi.fn(),
    })

    renderTradePanel()

    expect(screen.getByText(/Iniciá sesión/)).toBeInTheDocument()
    expect(
      screen.queryByRole('tab', { name: 'Comprar' }),
    ).not.toBeInTheDocument()
  })

  it('jugador dado de baja, muestra el aviso y no el form', () => {
    renderTradePanel({ estaActivo: false })

    expect(
      screen.getByText(
        'Este jugador fue dado de baja y ya no admite operaciones.',
      ),
    ).toBeInTheDocument()
  })

  it('sin tokens disponibles para comprar, muestra el mensaje en vez del form', () => {
    renderTradePanel({ tokensDisponibles: 0 })

    expect(
      screen.getByText('No quedan tokens disponibles para comprar.'),
    ).toBeInTheDocument()
  })

  it('calcula el total como precio x cantidad, y lo actualiza con el stepper', async () => {
    const user = userEvent.setup()
    renderTradePanel({ cotizacionVigente: 91.36 })

    // A cantidad 1, precio unitario y total coinciden — confirma que ambos arrancan en $91,36.
    expect(screen.getAllByText('$ 91,36')).toHaveLength(2)

    await user.click(screen.getByRole('button', { name: 'Sumar' }))

    expect(screen.getByText('$ 91,36')).toBeInTheDocument() // precio unitario, sin cambios
    expect(screen.getByText('$ 182,72')).toBeInTheDocument() // total, x2
  })

  it('el botón "Restar" nunca baja de 1', async () => {
    const user = userEvent.setup()
    renderTradePanel({ cotizacionVigente: 91.36 })

    await user.click(screen.getByRole('button', { name: 'Restar' }))

    expect(screen.getByRole('spinbutton')).toHaveValue(1)
    expect(screen.getAllByText('$ 91,36')).toHaveLength(2)
  })

  it('escribir una cantidad en el input recalcula el total, acotado al máximo', () => {
    renderTradePanel({ cotizacionVigente: 91.36, tokensDisponibles: 5 })

    const input = screen.getByRole('spinbutton')
    fireEvent.change(input, { target: { value: '3' } })

    expect(screen.getByText('$ 274,08')).toBeInTheDocument()

    fireEvent.change(input, { target: { value: '999' } })

    expect(input).toHaveValue(5)
  })

  it('hacer click en la pestaña ya activa ("Comprar") no rompe nada', async () => {
    const user = userEvent.setup()
    renderTradePanel({ cotizacionVigente: 91.36 })

    await user.click(screen.getByRole('tab', { name: 'Comprar' }))

    expect(screen.getByRole('tab', { name: 'Comprar' })).toHaveAttribute(
      'aria-selected',
      'true',
    )
  })

  it('sin cotización todavía, usa el valor inicial de 100 para el cálculo (igual que el backend)', () => {
    renderTradePanel({ cotizacionVigente: null })

    const precios = screen.getAllByText('$ 100,00')
    expect(precios.length).toBeGreaterThan(0)
  })

  it('al confirmar una compra, llama a comprarTokens y muestra la confirmación', async () => {
    vi.mocked(tokensApi.comprarTokens).mockResolvedValue({
      id: 'm1',
      jugadorId: JUGADOR_ID,
      tipo: 'COMPRA_SISTEMA',
      cantidad: 1,
      precioUnitario: '91.36',
      montoTotal: '91.36',
      contraparteUsuarioId: null,
      fecha: '2026-02-16T00:00:00Z',
    })
    const user = userEvent.setup()
    renderTradePanel({ cotizacionVigente: 91.36 })

    await user.click(screen.getByRole('button', { name: 'Confirmar compra' }))

    await waitFor(() =>
      expect(tokensApi.comprarTokens).toHaveBeenCalledWith(
        JUGADOR_ID,
        1,
        'fake-token',
      ),
    )
    expect(
      await screen.findByText('Compraste 1 token(s) por $ 91,36.'),
    ).toBeInTheDocument()
  })

  it('cambiando a la pestaña Vender, llama a venderTokens al confirmar', async () => {
    vi.mocked(portfolioApi.obtenerPortfolio).mockResolvedValue({
      saldoVirtual: '1000.00',
      tenencias: [
        {
          jugadorId: JUGADOR_ID,
          cantidad: 3,
          cotizacionVigente: '91.36',
          valor: '274.08',
        },
      ],
      valorTotal: '274.08',
    })
    vi.mocked(tokensApi.venderTokens).mockResolvedValue({
      id: 'm2',
      jugadorId: JUGADOR_ID,
      tipo: 'VENTA_SISTEMA',
      cantidad: 1,
      precioUnitario: '91.36',
      montoTotal: '91.36',
      contraparteUsuarioId: null,
      fecha: '2026-02-16T00:00:00Z',
    })
    const user = userEvent.setup()
    renderTradePanel({ cotizacionVigente: 91.36 })

    await user.click(screen.getByRole('tab', { name: 'Vender' }))
    await screen.findByText('Tenés 3 token(s) disponibles para vender')
    await user.click(screen.getByRole('button', { name: 'Confirmar venta' }))

    await waitFor(() =>
      expect(tokensApi.venderTokens).toHaveBeenCalledWith(
        JUGADOR_ID,
        1,
        'fake-token',
      ),
    )
  })

  it('si la compra falla, muestra el mensaje de error de la API', async () => {
    vi.mocked(tokensApi.comprarTokens).mockRejectedValue(
      new ApiError(409, 'Saldo insuficiente', 'Conflicto'),
    )
    const user = userEvent.setup()
    renderTradePanel()

    await user.click(screen.getByRole('button', { name: 'Confirmar compra' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Saldo insuficiente',
    )
  })
})
