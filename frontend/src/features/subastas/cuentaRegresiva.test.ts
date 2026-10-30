import { describe, expect, it } from 'vitest'
import { formatRestante } from './cuentaRegresiva'

describe('formatRestante', () => {
  it('por debajo de una hora muestra minutos:segundos', () => {
    expect(formatRestante(105_000)).toBe('1:45')
    expect(formatRestante(5_000)).toBe('0:05')
  })

  it('redondea hacia arriba los milisegundos sueltos (nunca muestra 0:00 antes de tiempo)', () => {
    expect(formatRestante(400)).toBe('0:01')
  })

  it('por debajo de un día muestra horas y minutos', () => {
    expect(formatRestante((5 * 3600 + 12 * 60) * 1000)).toBe('5 h 12 min')
  })

  it('arriba de un día muestra días y horas', () => {
    expect(formatRestante((2 * 86_400 + 3 * 3600) * 1000)).toBe('2 d 3 h')
  })
})
