import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { RootLayout } from './RootLayout'
import { TemaProvider } from './TemaProvider'
import { CLAVE_TEMA } from './tema'
import * as authContext from '../features/auth/AuthContext'

vi.mock('../features/auth/AuthContext', async (importOriginal) => {
  const actual = await importOriginal<typeof authContext>()
  return { ...actual, useAuth: vi.fn() }
})

function comoUsuario(rol: 'USER' | 'ADMIN' | null, logout = vi.fn()) {
  vi.mocked(authContext.useAuth).mockReturnValue({
    token: rol ? 'fake-token' : null,
    user: rol ? { usuarioId: 'u1', email: 'user@example.com', rol } : null,
    isAuthenticated: rol !== null,
    login: vi.fn(),
    logout,
  })
  return logout
}

function renderEn(ruta: string) {
  return render(
    <TemaProvider>
      <MemoryRouter initialEntries={[ruta]}>
        <Routes>
          <Route element={<RootLayout />}>
            <Route path="/mercado" element={<p>Pantalla de mercado</p>} />
            <Route path="/jugadores/:id" element={<p>Ficha</p>} />
            <Route path="/login" element={<p>Pantalla de login</p>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </TemaProvider>,
  )
}

/** El contenedor del layout, que es el que lleva `data-tema`. */
function layout() {
  return screen.getByRole('banner').parentElement!
}

describe('RootLayout', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('sin sesión, ofrece iniciar sesión o crear cuenta y oculta Mi cartera', () => {
    comoUsuario(null)
    renderEn('/mercado')

    expect(
      screen.getByRole('link', { name: 'Iniciar sesión' }),
    ).toHaveAttribute('href', '/login')
    expect(screen.getByRole('link', { name: 'Crear cuenta' })).toHaveAttribute(
      'href',
      '/registro',
    )
    expect(
      screen.queryByRole('link', { name: 'Mi cartera' }),
    ).not.toBeInTheDocument()
  })

  it('con sesión, muestra el email, Mi cartera y resalta la sección actual', () => {
    comoUsuario('USER')
    renderEn('/mercado')

    expect(screen.getByText('user@example.com')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Mi cartera' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Mercado' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(
      screen.queryByRole('link', { name: 'Admin' }),
    ).not.toBeInTheDocument()
  })

  it('como ADMIN, agrega el link de administración y lo marca en el usuario', () => {
    comoUsuario('ADMIN')
    renderEn('/mercado')

    expect(screen.getByRole('link', { name: 'Admin' })).toHaveAttribute(
      'href',
      '/admin/jugadores',
    )
    expect(screen.getByText('user@example.com (ADMIN)')).toBeInTheDocument()
  })

  it('Salir cierra la sesión y lleva al login', async () => {
    const logout = comoUsuario('USER')
    const user = userEvent.setup()
    renderEn('/mercado')

    await user.click(screen.getByRole('button', { name: 'Salir' }))

    expect(logout).toHaveBeenCalled()
    expect(await screen.findByText('Pantalla de login')).toBeInTheDocument()
  })

  it('el tema claro elegido sólo se aplica en la ficha del jugador; el resto sigue oscuro', () => {
    comoUsuario(null)
    localStorage.setItem(CLAVE_TEMA, 'claro')

    const { unmount } = renderEn('/jugadores/j1')
    expect(layout()).toHaveAttribute('data-tema', 'claro')
    unmount()

    renderEn('/mercado')
    expect(layout()).toHaveAttribute('data-tema', 'oscuro')
  })
})
