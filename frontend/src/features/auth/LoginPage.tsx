import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { login as loginRequest } from '../../api/auth'
import { ApiError } from '../../api/client'
import { useAuth } from './AuthContext'
import { Button } from '../../shared/components/Button'
import styles from '../../shared/styles/Form.module.css'

interface LocationState {
  mensaje?: string
}

/** UC-02: login. Error genérico del backend (sin indicar qué dato falló) se muestra tal cual. */
export function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const mensajeInicial = (location.state as LocationState | null)?.mensaje

  const mutation = useMutation({
    mutationFn: loginRequest,
    onSuccess: (data) => {
      login(data.token)
      navigate('/')
    },
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate({ email, password })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <h1>Iniciar sesión</h1>

      {mensajeInicial && !mutation.isError && (
        <p className={styles.info}>{mensajeInicial}</p>
      )}

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
          autoComplete="current-password"
        />
      </label>

      {mutation.isError && (
        <p className={styles.error} role="alert">
          {mutation.error instanceof ApiError
            ? mutation.error.message
            : 'No se pudo iniciar sesión.'}
        </p>
      )}

      <Button
        type="submit"
        className={styles.submit}
        disabled={mutation.isPending}
      >
        {mutation.isPending ? 'Ingresando…' : 'Ingresar'}
      </Button>

      <p>
        ¿No tenés cuenta? <Link to="/registro">Registrate</Link>
      </p>
    </form>
  )
}
