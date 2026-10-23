import { apiFetch } from './client'

/** UC-13 (ADMIN): dispara manualmente el mismo recálculo que corre el job semanal (UC-06). */
export function dispararRecalculoManual(token: string): Promise<void> {
  return apiFetch<void>('/cotizaciones/recalculo', { method: 'POST', token })
}
