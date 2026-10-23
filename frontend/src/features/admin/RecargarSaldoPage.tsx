import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { recargarSaldo, type UsuarioResponse } from '../../api/usuarios'
import { ApiError } from '../../api/client'
import { useAuth } from '../auth/AuthContext'
import { Button } from '../../shared/components/Button'
import { formatMoney } from '../../shared/format'
import styles from '../../shared/styles/Form.module.css'

/**
 * UC-14 (ADMIN): acredita saldo virtual en la cuenta de un usuario existente. El backend sólo
 * identifica al usuario por {@code id} (no hay búsqueda por email, tasks.md T1.6) — el admin tiene
 * que conocer de antemano el id exacto del usuario a recargar.
 */
export function RecargarSaldoPage() {
  const { token } = useAuth()
  const [usuarioId, setUsuarioId] = useState('')
  const [monto, setMonto] = useState('')
  const [resultado, setResultado] = useState<UsuarioResponse | null>(null)

  const mutation = useMutation({
    mutationFn: () => recargarSaldo(usuarioId.trim(), Number(monto), token!),
    onSuccess: (usuario) => {
      setResultado(usuario)
      setMonto('')
    },
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setResultado(null)
    mutation.mutate()
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <h1>Recargar saldo</h1>

      <label className={styles.field}>
        ID del usuario
        <input
          type="text"
          name="usuarioId"
          value={usuarioId}
          onChange={(event) => setUsuarioId(event.target.value)}
          placeholder="UUID del usuario"
          required
        />
      </label>

      <label className={styles.field}>
        Monto a acreditar
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

      {mutation.isError && (
        <p className={styles.error} role="alert">
          {mutation.error instanceof ApiError
            ? mutation.error.message
            : 'No se pudo recargar el saldo.'}
        </p>
      )}

      {resultado && (
        <p className={styles.info} role="status">
          Saldo actualizado: {formatMoney(resultado.saldoVirtual)} (
          {resultado.email})
        </p>
      )}

      <Button
        type="submit"
        className={styles.submit}
        disabled={mutation.isPending}
      >
        {mutation.isPending ? 'Recargando…' : 'Recargar'}
      </Button>

      <p>
        <Link to="/admin/jugadores">← Volver a administrar jugadores</Link>
      </p>
    </form>
  )
}
