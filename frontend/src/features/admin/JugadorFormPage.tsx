import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import {
  crearJugador,
  editarJugador,
  obtenerJugador,
  type DatosIdentificatoriosJugador,
} from '../../api/jugadores'
import { ApiError } from '../../api/client'
import { useAuth } from '../auth/AuthContext'
import { Button } from '../../shared/components/Button'
import styles from '../../shared/styles/Form.module.css'

interface JugadorFormProps {
  titulo: string
  valoresIniciales: DatosIdentificatoriosJugador
  onGuardar: (datos: DatosIdentificatoriosJugador) => void
  guardando: boolean
  error: unknown
}

/**
 * El formulario en sí, con su propio estado local inicializado una sola vez desde
 * {@code valoresIniciales} — nunca se re-sincroniza vía efecto (evita el antipatrón "setState
 * síncrono dentro de un efecto"; {@link JugadorFormPage} lo monta con {@code key} recién cuando
 * los datos ya están listos, así el estado inicial nace correcto sin necesidad de un efecto).
 */
function JugadorForm({
  titulo,
  valoresIniciales,
  onGuardar,
  guardando,
  error,
}: JugadorFormProps) {
  const [nombre, setNombre] = useState(valoresIniciales.nombre)
  const [club, setClub] = useState(valoresIniciales.club)
  const [posicion, setPosicion] = useState(valoresIniciales.posicion)
  const [fechaNacimiento, setFechaNacimiento] = useState(
    valoresIniciales.fechaNacimiento,
  )
  const [nacionalidad, setNacionalidad] = useState(
    valoresIniciales.nacionalidad,
  )

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    onGuardar({ nombre, club, posicion, fechaNacimiento, nacionalidad })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <h1>{titulo}</h1>

      <label className={styles.field}>
        Nombre
        <input
          type="text"
          name="nombre"
          value={nombre}
          onChange={(event) => setNombre(event.target.value)}
          required
        />
      </label>

      <label className={styles.field}>
        Club
        <input
          type="text"
          name="club"
          value={club}
          onChange={(event) => setClub(event.target.value)}
          required
        />
      </label>

      <label className={styles.field}>
        Posición
        <input
          type="text"
          name="posicion"
          value={posicion}
          onChange={(event) => setPosicion(event.target.value)}
          required
        />
      </label>

      <label className={styles.field}>
        Fecha de nacimiento
        <input
          type="date"
          name="fechaNacimiento"
          value={fechaNacimiento}
          onChange={(event) => setFechaNacimiento(event.target.value)}
          max={new Date().toISOString().slice(0, 10)}
          required
        />
      </label>

      <label className={styles.field}>
        Nacionalidad
        <input
          type="text"
          name="nacionalidad"
          value={nacionalidad}
          onChange={(event) => setNacionalidad(event.target.value)}
          required
        />
      </label>

      {error != null && (
        <p className={styles.error} role="alert">
          {error instanceof ApiError
            ? error.message
            : 'No se pudo guardar el jugador.'}
        </p>
      )}

      <Button type="submit" className={styles.submit} disabled={guardando}>
        {guardando ? 'Guardando…' : 'Guardar'}
      </Button>
    </form>
  )
}

const VALORES_VACIOS: DatosIdentificatoriosJugador = {
  nombre: '',
  club: '',
  posicion: '',
  fechaNacimiento: '',
  nacionalidad: '',
}

/**
 * Alta (UC-03) y edición (UC-04) de jugador — mismos campos, mismo shape que
 * {@code CrearJugadorRequest}/{@code EditarJugadorRequest} en el backend. El modo lo decide la
 * presencia de {@code :id} en la ruta (`/admin/jugadores/nuevo` vs. `/admin/jugadores/:id/editar`).
 */
export function JugadorFormPage() {
  const { id } = useParams<{ id?: string }>()
  const esEdicion = Boolean(id)
  const { token } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const jugadorActual = useQuery({
    queryKey: ['jugadores', id],
    queryFn: () => obtenerJugador(id!),
    enabled: esEdicion,
  })

  const mutation = useMutation({
    mutationFn: (datos: DatosIdentificatoriosJugador) =>
      esEdicion
        ? editarJugador(id!, datos, token!)
        : crearJugador(datos, token!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jugadores'] })
      navigate('/admin/jugadores')
    },
  })

  if (esEdicion && jugadorActual.isLoading) {
    return <p style={{ padding: '1.5rem' }}>Cargando…</p>
  }
  if (esEdicion && jugadorActual.isError) {
    return (
      <p role="alert" style={{ padding: '1.5rem' }}>
        No se pudo cargar el jugador a editar.
      </p>
    )
  }

  return (
    <JugadorForm
      key={id ?? 'nuevo'}
      titulo={esEdicion ? 'Editar jugador' : 'Nuevo jugador'}
      valoresIniciales={jugadorActual.data ?? VALORES_VACIOS}
      onGuardar={(datos) => mutation.mutate(datos)}
      guardando={mutation.isPending}
      error={mutation.error}
    />
  )
}
