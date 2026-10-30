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
  /** Tokens reservados en ofertas P2P abiertas, a cotización vigente — fuera de `tenencias`, pero
   * incluidos en `valorTotal` (si se venden se cobran; si vencen o se cancelan, vuelven). */
  valorEnVenta: string
  /** Saldo reservado como mejor postor de subastas activas — fuera de `saldoVirtual`, pero incluido
   * en `valorTotal` (UC-18). */
  saldoEnPujas: string
  valorTotal: string
}

/** UC-11: tenencias del usuario autenticado, valorizadas a la cotización vigente. */
export function obtenerPortfolio(token: string): Promise<PortfolioResponse> {
  return apiFetch<PortfolioResponse>('/portfolios/me', { token })
}
