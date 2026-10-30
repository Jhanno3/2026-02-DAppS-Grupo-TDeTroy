import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { CotizacionHistorialChart } from './CotizacionHistorialChart'
import type { CotizacionResponse } from '../../api/jugadores'

function cotizacion(
  semana: string,
  valor: string,
  overrides: Partial<CotizacionResponse> = {},
): CotizacionResponse {
  return {
    id: semana,
    semana,
    valor,
    origen: 'AUTOMATICO',
    fechaCalculo: '2026-02-16T00:00:00Z',
    ...overrides,
  }
}

describe('CotizacionHistorialChart', () => {
  it('sin cotizaciones, muestra el mensaje de "sin historial"', () => {
    render(<CotizacionHistorialChart cotizaciones={[]} />)

    expect(
      screen.getByText(
        'Este jugador todavía no tiene historial de cotización.',
      ),
    ).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })

  it('con una sola semana, dibuja el punto y avisa que la evolución llega con la segunda', () => {
    render(
      <CotizacionHistorialChart
        cotizaciones={[cotizacion('2026-W44', '104.39')]}
      />,
    )

    expect(screen.getByRole('img')).toHaveAccessibleName(
      'Historial de cotización de 1 semana, de $ 104,39 a $ 104,39',
    )
    expect(
      screen.getByText(/La curva arranca con la segunda semana/),
    ).toBeInTheDocument()
  })

  it('con varias semanas, la vista de tabla lista cada semana en orden', () => {
    render(
      <CotizacionHistorialChart
        cotizaciones={[
          cotizacion('2026-W42', '100.00'),
          cotizacion('2026-W43', '110.00', { origen: 'MANUAL' }),
          cotizacion('2026-W44', '105.50'),
        ]}
      />,
    )

    const filas = screen.getAllByRole('row').slice(1)
    expect(filas.map((f) => f.textContent)).toEqual([
      '2026-W42$ 100,00Automático',
      '2026-W43$ 110,00Manual',
      '2026-W44$ 105,50Automático',
    ])
    expect(
      screen.queryByText(/La curva arranca con la segunda semana/),
    ).not.toBeInTheDocument()
  })

  it('con foco y flechas, el tooltip recorre las semanas', () => {
    render(
      <CotizacionHistorialChart
        cotizaciones={[
          cotizacion('2026-W42', '100.00'),
          cotizacion('2026-W43', '110.00', { origen: 'MANUAL' }),
          cotizacion('2026-W44', '105.50'),
        ]}
      />,
    )
    const grafico = screen.getByRole('img')

    fireEvent.focus(grafico)
    expect(screen.getByRole('status')).toHaveTextContent('2026-W44$ 105,50')

    fireEvent.keyDown(grafico, { key: 'ArrowLeft' })
    expect(screen.getByRole('status')).toHaveTextContent(
      '2026-W43$ 110,00Ajuste manual',
    )

    fireEvent.keyDown(grafico, { key: 'Escape' })
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})
