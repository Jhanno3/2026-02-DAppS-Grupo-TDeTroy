const formatoMoneda = new Intl.NumberFormat('es-AR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** Formatea un BigDecimal del backend (llega como string, ver api/client.ts) como "$ 1.234,56". */
export function formatMoney(valor: string | number): string {
  return `$ ${formatoMoneda.format(Number(valor))}`
}
