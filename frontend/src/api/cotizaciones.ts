import { apiFetch } from './client'

/** UC-13 (ADMIN): dispara manualmente el mismo recálculo que corre el job semanal (UC-06). */
export function dispararRecalculoManual(token: string): Promise<void> {
  return apiFetch<void>('/cotizaciones/recalculo', { method: 'POST', token })
}

/** Ingesta administrativa (ADMIN): sincroniza métricas y fotos desde WhoScored. */
export function dispararIngestaRendimiento(token: string): Promise<void> {
  return apiFetch<void>('/cotizaciones/ingesta-rendimiento', {
    method: 'POST',
    token,
  })
}

/** Ingesta administrativa (ADMIN): sincroniza fixtures desde Football-Data.org. */
export function dispararIngestaFixtures(token: string): Promise<void> {
  return apiFetch<void>('/cotizaciones/ingesta-fixtures', {
    method: 'POST',
    token,
  })
}
