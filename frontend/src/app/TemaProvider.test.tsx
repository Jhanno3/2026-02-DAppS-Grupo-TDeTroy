import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { TemaProvider } from './TemaProvider'
import { CLAVE_TEMA, useTema } from './tema'

/** matchMedia falso cuyo valor de `prefers-color-scheme: light` se puede cambiar en vivo. */
function simularSistema(claro: boolean) {
  const oyentes = new Set<() => void>()
  const consulta = {
    get matches() {
      return claro
    },
    addEventListener: (_: string, oyente: () => void) => oyentes.add(oyente),
    removeEventListener: (_: string, oyente: () => void) =>
      oyentes.delete(oyente),
  }
  vi.stubGlobal(
    'matchMedia',
    vi.fn(() => consulta),
  )
  return {
    cambiarA(nuevoClaro: boolean) {
      claro = nuevoClaro
      oyentes.forEach((oyente) => oyente())
    },
  }
}

function TemaActual() {
  return <span data-testid="tema">{useTema().tema}</span>
}

describe('TemaProvider', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sin elección guardada, sigue al sistema y se actualiza si el sistema cambia', () => {
    const sistema = simularSistema(true)
    render(
      <TemaProvider>
        <TemaActual />
      </TemaProvider>,
    )

    expect(screen.getByTestId('tema')).toHaveTextContent('claro')

    act(() => sistema.cambiarA(false))

    expect(screen.getByTestId('tema')).toHaveTextContent('oscuro')
  })

  it('con una elección guardada, la respeta aunque el sistema diga otra cosa', () => {
    simularSistema(true)
    localStorage.setItem(CLAVE_TEMA, 'oscuro')
    render(
      <TemaProvider>
        <TemaActual />
      </TemaProvider>,
    )

    expect(screen.getByTestId('tema')).toHaveTextContent('oscuro')
  })

  it('useTema fuera del provider avisa con un error claro', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})

    expect(() => render(<TemaActual />)).toThrow(
      'useTema tiene que usarse dentro de <TemaProvider>',
    )
  })
})
