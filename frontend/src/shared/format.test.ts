import { describe, expect, it } from 'vitest'
import { formatMoney } from './format'

describe('formatMoney', () => {
  it('formatea un número con separador de miles y 2 decimales', () => {
    expect(formatMoney(1234.5)).toBe('$ 1.234,50')
  })

  it('formatea un string (BigDecimal serializado por el backend)', () => {
    expect(formatMoney('91.36')).toBe('$ 91,36')
  })

  it('formatea cero', () => {
    expect(formatMoney(0)).toBe('$ 0,00')
  })

  it('redondea a 2 decimales', () => {
    expect(formatMoney(10.005)).toBe('$ 10,01')
  })
})
