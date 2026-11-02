import { apiFetch } from './client'

export interface TenenciaPortfolio {
  jugadorId: string
  cantidad: number
  cotizacionVigente: string | null
  valor: string
  /** Lo que se pagó en promedio por token (costo promedio ponderado, lo calcula el backend). */
  precioPromedioCompra: string
  /** `valor` menos lo que costaron estos tokens: positivo = ganancia, negativo = pérdida. */
  gananciaPerdida: string
  /** `gananciaPerdida` en porcentaje sobre el costo (`"20.00"` = +20%). */
  rentabilidad: string
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
  /** Suma de la ganancia/pérdida de todas las `tenencias`. */
  gananciaPerdida: string
  valorTotal: string
}

/** UC-11: tenencias del usuario autenticado, valorizadas a la cotización vigente. */
export function obtenerPortfolio(token: string): Promise<PortfolioResponse> {
  return apiFetch<PortfolioResponse>('/portfolios/me', { token })
}
