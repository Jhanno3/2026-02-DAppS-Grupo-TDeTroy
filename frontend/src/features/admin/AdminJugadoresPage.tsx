import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { darDeBajaJugador, listarJugadores } from '../../api/jugadores'
import { dispararRecalculoManual } from '../../api/cotizaciones'
import { ApiError } from '../../api/client'
import { useAuth } from '../auth/AuthContext'
import { Button } from '../../shared/components/Button'
import { LinkButton } from '../../shared/components/LinkButton'
import styles from './AdminJugadoresPage.module.css'

/**
 * Panel ADMIN: catálogo con acciones de alta/edición/baja (UC-03/UC-04/UC-15), recarga de saldo
 * (UC-14, página propia) y disparo manual del recálculo de cotización (UC-13).
 */
export function AdminJugadoresPage() {
  const { token } = useAuth()
  const queryClient = useQueryClient()
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [jugadorEnBaja, setJugadorEnBaja] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['jugadores'],
    queryFn: listarJugadores,
  })

  const recalculoMutation = useMutation({
    mutationFn: () => dispararRecalculoManual(token!),
    onSuccess: async () => {
      setMensaje('Recálculo de cotización disparado correctamente.')
      await queryClient.invalidateQueries({ queryKey: ['jugadores'] })
    },
    onError: (err) =>
      setMensaje(
        err instanceof ApiError
          ? err.message
          : 'No se pudo disparar el recálculo.',
      ),
  })

  const bajaMutation = useMutation({
    mutationFn: (jugadorId: string) => darDeBajaJugador(jugadorId, token!),
    onMutate: (jugadorId) => setJugadorEnBaja(jugadorId),
    onSuccess: async () => {
      setMensaje(null)
      await queryClient.invalidateQueries({ queryKey: ['jugadores'] })
    },
    onError: (err) =>
      setMensaje(
        err instanceof ApiError ? err.message : 'No se pudo dar de baja.',
      ),
    onSettled: () => setJugadorEnBaja(null),
  })

  function handleBaja(jugadorId: string, nombre: string) {
    const confirmado = window.confirm(
      `¿Dar de baja a "${nombre}"? Sus tokens se retiran de circulación de forma permanente y cada usuario afectado recibe una compensación. Esta acción no se puede deshacer.`,
    )
    if (confirmado) {
      bajaMutation.mutate(jugadorId)
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1 className={styles.title}>Administrar jugadores</h1>
        <div className={styles.headerAcciones}>
          <LinkButton to="/admin/saldo" variant="secondary" small>
            Recargar saldo
          </LinkButton>
          <Button
            variant="secondary"
            small
            onClick={() => recalculoMutation.mutate()}
            disabled={recalculoMutation.isPending}
          >
            {recalculoMutation.isPending ? 'Disparando…' : 'Disparar recálculo'}
          </Button>
          <LinkButton to="/admin/jugadores/nuevo">+ Nuevo jugador</LinkButton>
        </div>
      </div>

      {mensaje != null && (
        <p className={styles.mensaje} role="status">
          {mensaje}
        </p>
      )}

      {isLoading && <p>Cargando…</p>}
      {isError && <p role="alert">No se pudo cargar el catálogo.</p>}

      {data && (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Club</th>
              <th>Liga</th>
              <th>Posición</th>
              <th>Tokens disponibles</th>
              <th>Estado</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {data.map((jugador) => (
              <tr key={jugador.id}>
                <td>{jugador.nombre}</td>
                <td>{jugador.club}</td>
                <td>{jugador.liga}</td>
                <td>{jugador.posicion}</td>
                <td>{jugador.tokensDisponibles} / 100</td>
                <td>
                  <span
                    className={`${styles.badge} ${
                      jugador.estado === 'ACTIVO'
                        ? styles.badgeActivo
                        : styles.badgeInactivo
                    }`}
                  >
                    {jugador.estado}
                  </span>
                </td>
                <td>
                  <div className={styles.filaAcciones}>
                    <LinkButton
                      to={`/admin/jugadores/${jugador.id}/editar`}
                      variant="secondary"
                      small
                    >
                      Editar
                    </LinkButton>
                    {jugador.estado === 'ACTIVO' && (
                      <Button
                        variant="ghost"
                        small
                        onClick={() => handleBaja(jugador.id, jugador.nombre)}
                        disabled={jugadorEnBaja === jugador.id}
                      >
                        {jugadorEnBaja === jugador.id
                          ? 'Dando de baja…'
                          : 'Dar de baja'}
                      </Button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
