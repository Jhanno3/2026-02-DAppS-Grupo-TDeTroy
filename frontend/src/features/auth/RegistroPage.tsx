import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { registrar } from '../../api/auth'
import { ApiError } from '../../api/client'
import { Button } from '../../shared/components/Button'
import styles from '../../shared/styles/Form.module.css'

/**
 * UC-01: alta de cuenta USER con saldo inicial cero. El backend no devuelve un JWT en el alta
 * (UC-01 y UC-02 son flujos separados) — tras registrarse con éxito, redirige a {@code /login}
 * para que el usuario inicie sesión con las credenciales recién creadas.
 */
export function RegistroPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const navigate = useNavigate()

  const mutation = useMutation({
    mutationFn: registrar,
    onSuccess: () => {
      navigate('/login', {
        state: { mensaje: 'Cuenta creada. Ya podés iniciar sesión.' },
      })
    },
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate({ email, password })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <h1>Crear cuenta</h1>

      <label className={styles.field}>
        Email
        <input
          type="email"
          name="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
          autoComplete="email"
        />
      </label>

      <label className={styles.field}>
        Contraseña
        <input
          type="password"
          name="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
          autoComplete="new-password"
        />
      </label>

      {mutation.isError && (
        <p className={styles.error} role="alert">
          {mutation.error instanceof ApiError
            ? mutation.error.message
            : 'No se pudo crear la cuenta.'}
        </p>
      )}

      <Button
        type="submit"
        className={styles.submit}
        disabled={mutation.isPending}
      >
        {mutation.isPending ? 'Creando cuenta…' : 'Crear cuenta'}
      </Button>

      <p>
        ¿Ya tenés cuenta? <Link to="/login">Iniciá sesión</Link>
      </p>
    </form>
  )
}
