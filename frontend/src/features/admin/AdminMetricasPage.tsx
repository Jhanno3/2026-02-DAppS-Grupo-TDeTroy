import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  actualizarConfiguracionMetricas,
  obtenerConfiguracionMetricas,
  type ConfiguracionMetricaResponse,
  type ItemActualizarMetrica,
} from '../../api/metricas'
import { dispararRecalculoManual } from '../../api/cotizaciones'
import { ApiError } from '../../api/client'
import { useAuth } from '../auth/AuthContext'
import { Button } from '../../shared/components/Button'
import { LinkButton } from '../../shared/components/LinkButton'
import styles from './AdminMetricasPage.module.css'

export const POSICIONES = ['Delantero', 'Centrocampista', 'Defensor', 'Arquero'] as const

export interface MetricaFormRow {
  clave: string
  etiqueta: string
  sentido: 'POSITIVO' | 'NEGATIVO'
  activo: boolean
  pesoGeneral: string
  pesosPorPosicion: Record<string, string>
}

function inicializarFilas(data: ConfiguracionMetricaResponse[]): MetricaFormRow[] {
  return data.map((item) => {
    const pesosPos: Record<string, string> = {}
    for (const pos of POSICIONES) {
      const val = item.pesosPorPosicion?.[pos]
      pesosPos[pos] = val !== undefined ? String(val) : String(item.pesoGeneral)
    }
    return {
      clave: item.clave,
      etiqueta: item.etiqueta,
      sentido: item.sentido,
      activo: item.activo,
      pesoGeneral: String(item.pesoGeneral),
      pesosPorPosicion: pesosPos,
    }
  })
}

interface AdminMetricasFormProps {
  metricasIniciales: ConfiguracionMetricaResponse[]
  onGuardar: (payload: ItemActualizarMetrica[]) => void
  guardando: boolean
  onRecalcular: () => void
  recalculando: boolean
}

function AdminMetricasForm({
  metricasIniciales,
  onGuardar,
  guardando,
  onRecalcular,
  recalculando,
}: AdminMetricasFormProps) {
  const [filas, setFilas] = useState<MetricaFormRow[]>(() =>
    inicializarFilas(metricasIniciales),
  )
  const [errorValidacion, setErrorValidacion] = useState<string | null>(null)

  function handleActivoChange(index: number, checked: boolean) {
    setFilas((prev) => {
      const copy = [...prev]
      copy[index] = { ...copy[index], activo: checked }
      return copy
    })
  }

  function handleGeneralChange(index: number, valor: string) {
    setFilas((prev) => {
      const copy = [...prev]
      copy[index] = { ...copy[index], pesoGeneral: valor }
      return copy
    })
  }

  function handlePosicionChange(index: number, posicion: string, valor: string) {
    setFilas((prev) => {
      const copy = [...prev]
      copy[index] = {
        ...copy[index],
        pesosPorPosicion: {
          ...copy[index].pesosPorPosicion,
          [posicion]: valor,
        },
      }
      return copy
    })
  }

  function handleSubmit() {
    setErrorValidacion(null)
    const metricasPayload: ItemActualizarMetrica[] = []

    for (const fila of filas) {
      const numGeneral = parseFloat(fila.pesoGeneral)
      if (isNaN(numGeneral) || numGeneral < 0) {
        setErrorValidacion(
          `El peso general para "${fila.etiqueta}" debe ser un número mayor o igual a 0.`,
        )
        return
      }

      const pesosPosNum: Record<string, number> = {}
      for (const pos of POSICIONES) {
        const valStr = fila.pesosPorPosicion[pos]
        const valNum = parseFloat(valStr)
        if (isNaN(valNum) || valNum < 0) {
          setErrorValidacion(
            `El peso de ${pos} para "${fila.etiqueta}" debe ser un número mayor o igual a 0.`,
          )
          return
        }
        pesosPosNum[pos] = valNum
      }

      metricasPayload.push({
        clave: fila.clave,
        pesoGeneral: numGeneral,
        activo: fila.activo,
        pesosPorPosicion: pesosPosNum,
      })
    }

    onGuardar(metricasPayload)
  }

  return (
    <>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>Ponderación de Métricas</h1>
          <p className={styles.subtitle}>
            Configura los pesos dinámicos y la ponderación por posición (Delantero, Centrocampista, Defensor, Arquero).
          </p>
        </div>
        <div className={styles.headerAcciones}>
          <LinkButton to="/admin/jugadores" variant="secondary" small>
            ← Volver a jugadores
          </LinkButton>
          <Button
            variant="secondary"
            small
            onClick={onRecalcular}
            disabled={recalculando}
          >
            {recalculando ? 'Recalculando…' : '⚡ Disparar recálculo'}
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            disabled={guardando || filas.length === 0}
          >
            {guardando ? 'Guardando…' : 'Guardar ponderaciones'}
          </Button>
        </div>
      </div>

      {errorValidacion && (
        <div className={`${styles.mensaje} ${styles.mensajeError}`} role="alert">
          {errorValidacion}
        </div>
      )}

      {filas.length > 0 && (
        <div className={styles.tableWrapper}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Métrica</th>
                <th>Sentido</th>
                <th>Activo</th>
                <th>General</th>
                <th>Delantero</th>
                <th>Centrocampista</th>
                <th>Defensor</th>
                <th>Arquero</th>
              </tr>
            </thead>
            <tbody>
              {filas.map((fila, idx) => (
                <tr key={fila.clave}>
                  <td>
                    <div className={styles.metricaNombre}>{fila.etiqueta}</div>
                    <div className={styles.metricaClave}>{fila.clave}</div>
                  </td>
                  <td>
                    <span
                      className={`${styles.badge} ${
                        fila.sentido === 'POSITIVO'
                          ? styles.badgePositivo
                          : styles.badgeNegativo
                      }`}
                    >
                      {fila.sentido}
                    </span>
                  </td>
                  <td>
                    <input
                      type="checkbox"
                      aria-label={`Activo ${fila.etiqueta}`}
                      checked={fila.activo}
                      onChange={(e) => handleActivoChange(idx, e.target.checked)}
                    />
                  </td>
                  <td>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      className={styles.inputNumber}
                      aria-label={`Peso general ${fila.etiqueta}`}
                      value={fila.pesoGeneral}
                      onChange={(e) => handleGeneralChange(idx, e.target.value)}
                    />
                  </td>
                  {POSICIONES.map((pos) => (
                    <td key={pos}>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        className={styles.inputNumber}
                        aria-label={`Peso ${pos} ${fila.etiqueta}`}
                        value={fila.pesosPorPosicion[pos] ?? ''}
                        onChange={(e) =>
                          handlePosicionChange(idx, pos, e.target.value)
                        }
                      />
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className={styles.footerAcciones}>
        <Button
          variant="primary"
          onClick={handleSubmit}
          disabled={guardando || filas.length === 0}
        >
          {guardando ? 'Guardando…' : 'Guardar ponderaciones'}
        </Button>
      </div>
    </>
  )
}

export function AdminMetricasPage() {
  const { token } = useAuth()
  const queryClient = useQueryClient()
  const [mensaje, setMensaje] = useState<{ texto: string; tipo: 'exito' | 'error' } | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['configuracion-metricas'],
    queryFn: () => obtenerConfiguracionMetricas(token!),
    enabled: Boolean(token),
  })

  const guardarMutation = useMutation({
    mutationFn: (metricas: ItemActualizarMetrica[]) =>
      actualizarConfiguracionMetricas({ metricas }, token!),
    onSuccess: async () => {
      setMensaje({
        texto: 'Configuración de métricas y ponderaciones por posición guardada con éxito.',
        tipo: 'exito',
      })
      await queryClient.invalidateQueries({ queryKey: ['configuracion-metricas'] })
    },
    onError: (err) => {
      setMensaje({
        texto:
          err instanceof ApiError
            ? err.message
            : 'Error al actualizar las métricas.',
        tipo: 'error',
      })
    },
  })

  const recalculoMutation = useMutation({
    mutationFn: () => dispararRecalculoManual(token!),
    onSuccess: async () => {
      setMensaje({
        texto: 'Recálculo manual de cotizaciones ejecutado correctamente con las nuevas ponderaciones.',
        tipo: 'exito',
      })
      await queryClient.invalidateQueries({ queryKey: ['jugadores'] })
      await queryClient.invalidateQueries({ queryKey: ['ranking'] })
    },
    onError: (err) => {
      setMensaje({
        texto:
          err instanceof ApiError
            ? err.message
            : 'No se pudo disparar el recálculo.',
        tipo: 'error',
      })
    },
  })

  return (
    <div className={styles.container}>
      {mensaje && (
        <div
          className={`${styles.mensaje} ${
            mensaje.tipo === 'exito' ? styles.mensajeExito : styles.mensajeError
          }`}
          role={mensaje.tipo === 'error' ? 'alert' : 'status'}
        >
          {mensaje.texto}
        </div>
      )}

      {isLoading && <p>Cargando configuración de métricas…</p>}
      {isError && <p role="alert">No se pudo cargar la configuración de métricas.</p>}

      {data && (
        <AdminMetricasForm
          key={data.map((d) => `${d.clave}-${d.pesoGeneral}`).join(',')}
          metricasIniciales={data}
          onGuardar={(payload) => {
            setMensaje(null)
            guardarMutation.mutate(payload)
          }}
          guardando={guardarMutation.isPending}
          onRecalcular={() => recalculoMutation.mutate()}
          recalculando={recalculoMutation.isPending}
        />
      )}
    </div>
  )
}
