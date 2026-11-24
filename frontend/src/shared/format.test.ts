import { describe, expect, it } from 'vitest'
import { formatearMoneda } from './format'

describe('formatearMoneda', () => {
  it('formatea un número con separador de miles y 2 decimales', () => {
    expect(formatearMoneda(1234.5)).toBe('$ 1.234,50')
  })

  it('formatea un string (BigDecimal serializado por el backend)', () => {
    expect(formatearMoneda('91.36')).toBe('$ 91,36')
  })

  it('formatea cero', () => {
    expect(formatearMoneda(0)).toBe('$ 0,00')
  })

  it('redondea a 2 decimales', () => {
    expect(formatearMoneda(10.005)).toBe('$ 10,01')
  })
})
