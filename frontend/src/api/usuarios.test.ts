import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from './client'
import { cargarSaldoPropio, recargarSaldo } from './usuarios'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof client>()
  return { ...actual, apiFetch: vi.fn() }
})

describe('api/usuarios', () => {
  beforeEach(() => {
    vi.mocked(client.apiFetch).mockResolvedValue(undefined)
  })

  it('recargarSaldo hace POST /usuarios/{id}/saldo con monto, token y X-API-Key (UC-14)', async () => {
    await recargarSaldo('usuario-1', 500, 'admin-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/usuarios/usuario-1/saldo', {
      method: 'POST',
      body: { monto: 500 },
      token: 'admin-token',
      headers: { 'X-API-Key': 'pasarela-pagos-dev-api-key' },
    })
  })

  it('cargarSaldoPropio hace POST /usuarios/me/saldo con datos de tarjeta, token y X-API-Key', async () => {
    const datos = {
      monto: 250,
      numeroTarjeta: '4532123456789010',
      titular: 'Juan Perez',
      vencimiento: '12/30',
      codigoSeguridad: '123',
    }
    await cargarSaldoPropio(datos, 'user-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/usuarios/me/saldo', {
      method: 'POST',
      body: datos,
      token: 'user-token',
      headers: { 'X-API-Key': 'pasarela-pagos-dev-api-key' },
    })
  })
})
