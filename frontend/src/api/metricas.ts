import { apiFetch } from './client'

export type SentidoMetrica = 'POSITIVO' | 'NEGATIVO'

export interface ConfiguracionMetricaResponse {
  clave: string
  etiqueta: string
  sentido: SentidoMetrica
  activo: boolean
  pesoGeneral: number
  pesosPorPosicion: Record<string, number>
}

export interface ItemActualizarMetrica {
  clave: string
  pesoGeneral: number
  activo: boolean
  pesosPorPosicion?: Record<string, number>
}

export interface ActualizarMetricasRequest {
  metricas: ItemActualizarMetrica[]
}

export function obtenerConfiguracionMetricas(
  token: string,
): Promise<ConfiguracionMetricaResponse[]> {
  return apiFetch<ConfiguracionMetricaResponse[]>('/configuracion/metricas', {
    method: 'GET',
    token,
  })
}

export function actualizarConfiguracionMetricas(
  request: ActualizarMetricasRequest,
  token: string,
): Promise<ConfiguracionMetricaResponse[]> {
  return apiFetch<ConfiguracionMetricaResponse[]>('/configuracion/metricas', {
    method: 'PUT',
    token,
    body: request,
  })
}
