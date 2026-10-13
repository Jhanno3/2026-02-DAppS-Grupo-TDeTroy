import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { CotizacionChart } from './CotizacionChart'
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

describe('CotizacionChart', () => {
  it('sin cotizaciones, muestra el mensaje de "sin historial" (spec UC-08: no es un error)', () => {
    render(<CotizacionChart cotizaciones={[]} />)

    expect(
      screen.getByText('Este jugador todavía no tiene cotización calculada.'),
    ).toBeInTheDocument()
  })

  it('con una sola cotización, muestra el valor pero no la variación ni el gráfico', () => {
    render(
      <CotizacionChart cotizaciones={[cotizacion('2026-W07', '100.00')]} />,
    )

    expect(screen.getByText('$ 100,00')).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })

  it('con cotización en suba, muestra el signo ▲ y el porcentaje correcto', () => {
    render(
      <CotizacionChart
        cotizaciones={[
          cotizacion('2026-W07', '100.00'),
          cotizacion('2026-W08', '110.00'),
        ]}
      />,
    )

    expect(screen.getByText(/▲ 10.0% desde 2026-W07/)).toBeInTheDocument()
  })

  it('con cotización en baja, muestra el signo ▼ y el porcentaje absoluto', () => {
    render(
      <CotizacionChart
        cotizaciones={[
          cotizacion('2026-W41', '100.00'),
          cotizacion('2026-W42', '92.00'),
        ]}
      />,
    )

    expect(screen.getByText(/▼ 8.0% desde 2026-W41/)).toBeInTheDocument()
  })

  it('muestra la semana de la última cotización en el encabezado', () => {
    render(
      <CotizacionChart
        cotizaciones={[
          cotizacion('2026-W07', '100.00'),
          cotizacion('2026-W08', '105.00'),
        ]}
      />,
    )

    expect(screen.getByText('Última cotización (2026-W08)')).toBeInTheDocument()
  })
})
