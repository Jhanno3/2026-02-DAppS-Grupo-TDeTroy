import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from './client'
import { recargarSaldo } from './usuarios'

vi.mock('./client', async (importOriginal) => {
  const actual = await importOriginal<typeof client>()
  return { ...actual, apiFetch: vi.fn() }
})

describe('api/usuarios', () => {
  beforeEach(() => {
    vi.mocked(client.apiFetch).mockResolvedValue(undefined)
  })

  it('recargarSaldo hace POST /usuarios/{id}/saldo con monto y token (UC-14)', async () => {
    await recargarSaldo('usuario-1', 500, 'admin-token')

    expect(client.apiFetch).toHaveBeenCalledWith('/usuarios/usuario-1/saldo', {
      method: 'POST',
      body: { monto: 500 },
      token: 'admin-token',
    })
  })
})
