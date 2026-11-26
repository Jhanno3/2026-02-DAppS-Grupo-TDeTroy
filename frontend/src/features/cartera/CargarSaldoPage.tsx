import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { cargarSaldoPropio, type UsuarioResponse } from '../../api/usuarios'
import { ApiError } from '../../api/client'
import { useAuth } from '../auth/AuthContext'
import { Button } from '../../shared/components/Button'
import { formatearMoneda } from '../../shared/format'
import styles from '../../shared/styles/Form.module.css'

export function CargarSaldoPage() {
  const { token } = useAuth()
  const queryClient = useQueryClient()
  const [monto, setMonto] = useState('')
  const [numeroTarjeta, setNumeroTarjeta] = useState('')
  const [titular, setTitular] = useState('')
  const [vencimiento, setVencimiento] = useState('')
  const [codigoSeguridad, setCodigoSeguridad] = useState('')
  const [errorValidacion, setErrorValidacion] = useState<string | null>(null)
  const [resultado, setResultado] = useState<UsuarioResponse | null>(null)

  const mutacion = useMutation({
    mutationFn: (tarjetaLimpia: string) =>
      cargarSaldoPropio(
        {
          monto: Number(monto),
          numeroTarjeta: tarjetaLimpia,
          titular: titular.trim(),
          vencimiento: vencimiento.trim(),
          codigoSeguridad: codigoSeguridad.trim(),
        },
        token!,
      ),
    onSuccess: async (usuario) => {
      setResultado(usuario)
      setMonto('')
      setNumeroTarjeta('')
      setTitular('')
      setVencimiento('')
      setCodigoSeguridad('')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['portfolio', 'me'] }),
        queryClient.invalidateQueries({ queryKey: ['movimientos', 'me'] }),
      ])
    },
  })

  function manejarEnvio(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setResultado(null)
    setErrorValidacion(null)

    const tarjetaLimpia = numeroTarjeta.replace(/\s+/g, '')
    if (!/^\d{16}$/.test(tarjetaLimpia)) {
      setErrorValidacion('El número de tarjeta debe tener 16 dígitos.')
      return
    }
    if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(vencimiento.trim())) {
      setErrorValidacion('El vencimiento debe tener formato MM/AA.')
      return
    }
    if (!/^\d{3,4}$/.test(codigoSeguridad.trim())) {
      setErrorValidacion('El código de seguridad debe tener 3 o 4 dígitos.')
      return
    }

    mutacion.mutate(tarjetaLimpia)
  }

  return (
    <form className={styles.form} onSubmit={manejarEnvio}>
      <h1>Cargar saldo</h1>
      <p className={styles.info}>
        Pasarela de pagos simulada: ingresá datos ficticios de tarjeta para
        acreditar saldo en tu cuenta.
      </p>

      <label className={styles.field}>
        Monto a cargar
        <input
          type="number"
          name="monto"
          value={monto}
          onChange={(event) => setMonto(event.target.value)}
          min="0.01"
          step="0.01"
          required
        />
      </label>

      <label className={styles.field}>
        Número de tarjeta
        <input
          type="text"
          name="numeroTarjeta"
          value={numeroTarjeta}
          onChange={(event) => setNumeroTarjeta(event.target.value)}
          placeholder="4532 1234 5678 9010"
          maxLength={19}
          required
        />
      </label>

      <label className={styles.field}>
        Titular de la tarjeta
        <input
          type="text"
          name="titular"
          value={titular}
          onChange={(event) => setTitular(event.target.value)}
          placeholder="Nombre y apellido"
          minLength={2}
          maxLength={100}
          required
        />
      </label>

      <label className={styles.field}>
        Vencimiento (MM/AA)
        <input
          type="text"
          name="vencimiento"
          value={vencimiento}
          onChange={(event) => setVencimiento(event.target.value)}
          placeholder="12/28"
          maxLength={5}
          required
        />
      </label>

      <label className={styles.field}>
        Código de seguridad (CVV)
        <input
          type="password"
          name="codigoSeguridad"
          value={codigoSeguridad}
          onChange={(event) => setCodigoSeguridad(event.target.value)}
          placeholder="123"
          maxLength={4}
          required
        />
      </label>

      {errorValidacion && (
        <p className={styles.error} role="alert">
          {errorValidacion}
        </p>
      )}

      {mutacion.isError && (
        <p className={styles.error} role="alert">
          {mutacion.error instanceof ApiError
            ? mutacion.error.message
            : 'No se pudo procesar el pago.'}
        </p>
      )}

      {resultado && (
        <p className={styles.info} role="status">
          Pago aprobado. Saldo actualizado:{' '}
          {formatearMoneda(resultado.saldoVirtual)}
        </p>
      )}

      <Button
        type="submit"
        className={styles.submit}
        disabled={mutacion.isPending}
      >
        {mutacion.isPending ? 'Procesando pago…' : 'Cargar saldo'}
      </Button>

      <p>
        <Link to="/cartera">← Volver a mi cartera</Link>
      </p>
    </form>
  )
}
