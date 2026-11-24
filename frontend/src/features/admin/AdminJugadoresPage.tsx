import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { darDeBajaJugador, listarJugadores } from '../../api/jugadores'
import {
  dispararIngestaRendimiento,
  dispararRecalculoManual,
} from '../../api/cotizaciones'
import { ApiError } from '../../api/client'
import { useAuth } from '../auth/AuthContext'
import { Button } from '../../shared/components/Button'
import { LinkButton } from '../../shared/components/LinkButton'
import styles from './AdminJugadoresPage.module.css'

/**
 * Panel ADMIN: catálogo con acciones de alta/edición/baja (UC-03/UC-04/UC-15), recarga de saldo
 * (UC-14, página propia), sincronización de WhoScored y disparo manual del recálculo de cotización
 * (UC-13).
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

  const mutacionIngestaRendimiento = useMutation({
    mutationFn: () => dispararIngestaRendimiento(token!),
    onSuccess: async () => {
      setMensaje('Sincronización con WhoScored completada con éxito.')
      await queryClient.invalidateQueries({ queryKey: ['jugadores'] })
      await queryClient.invalidateQueries({ queryKey: ['ranking'] })
    },
    onError: (err) =>
      setMensaje(
        err instanceof ApiError
          ? err.message
          : 'No se pudo sincronizar con WhoScored.',
      ),
  })

  const mutacionRecalculo = useMutation({
    mutationFn: () => dispararRecalculoManual(token!),
    onSuccess: async () => {
      setMensaje('Recálculo de cotización disparado correctamente.')
      await queryClient.invalidateQueries({ queryKey: ['jugadores'] })
      await queryClient.invalidateQueries({ queryKey: ['ranking'] })
    },
    onError: (err) =>
      setMensaje(
        err instanceof ApiError
          ? err.message
          : 'No se pudo disparar el recálculo.',
      ),
  })

  const mutacionBaja = useMutation({
    mutationFn: (jugadorId: string) => darDeBajaJugador(jugadorId, token!),
    onMutate: (jugadorId) => setJugadorEnBaja(jugadorId),
    onSuccess: async () => {
      setMensaje(null)
      await queryClient.invalidateQueries({ queryKey: ['jugadores'] })
      await queryClient.invalidateQueries({ queryKey: ['ranking'] })
    },
    onError: (err) =>
      setMensaje(
        err instanceof ApiError ? err.message : 'No se pudo dar de baja.',
      ),
    onSettled: () => setJugadorEnBaja(null),
  })

  function manejarBaja(jugadorId: string, nombre: string) {
    const confirmado = window.confirm(
      `¿Dar de baja a "${nombre}"? Sus tokens se retiran de circulación de forma permanente y cada usuario afectado recibe una compensación. Esta acción no se puede deshacer.`,
    )
    if (confirmado) {
      mutacionBaja.mutate(jugadorId)
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
            onClick={() => mutacionIngestaRendimiento.mutate()}
            disabled={mutacionIngestaRendimiento.isPending}
          >
            {mutacionIngestaRendimiento.isPending
              ? 'Sincronizando WhoScored…'
              : '⚡ Sincronizar WhoScored'}
          </Button>
          <Button
            variant="secondary"
            small
            onClick={() => mutacionRecalculo.mutate()}
            disabled={mutacionRecalculo.isPending}
          >
            {mutacionRecalculo.isPending ? 'Disparando…' : 'Disparar recálculo'}
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
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {data.map((jugador) => (
              <tr key={jugador.id}>
                <td>{jugador.nombre}</td>
                <td>{jugador.club}</td>
                <td>{jugador.liga}</td>
                <td>{jugador.posicion}</td>
                <td>{jugador.tokensDisponibles}</td>
                <td>
                  <span
                    className={
                      jugador.estado === 'ACTIVO'
                        ? styles.estadoActivo
                        : styles.estadoBaja
                    }
                  >
                    {jugador.estado}
                  </span>
                </td>
                <td className={styles.acciones}>
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
                      onClick={() => manejarBaja(jugador.id, jugador.nombre)}
                      disabled={jugadorEnBaja === jugador.id}
                    >
                      {jugadorEnBaja === jugador.id
                        ? 'Dando de baja…'
                        : 'Dar de baja'}
                    </Button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
