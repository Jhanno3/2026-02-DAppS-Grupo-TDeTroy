import { apiFetch } from './client'

export interface TenenciaPortfolio {
  jugadorId: string
  cantidad: number
  cotizacionVigente: string | null
  valor: string
}

export interface PortfolioResponse {
  saldoVirtual: string
  tenencias: TenenciaPortfolio[]
  valorTotal: string
}

/** UC-11: tenencias del usuario autenticado, valorizadas a la cotización vigente. */
export function obtenerPortfolio(token: string): Promise<PortfolioResponse> {
  return apiFetch<PortfolioResponse>('/portfolios/me', { token })
}
