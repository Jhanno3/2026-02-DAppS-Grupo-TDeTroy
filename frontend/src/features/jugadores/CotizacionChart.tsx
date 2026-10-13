import type { CotizacionResponse } from '../../api/jugadores'
import { formatMoney } from '../../shared/format'
import styles from './CotizacionChart.module.css'

interface CotizacionChartProps {
  cotizaciones: CotizacionResponse[]
}

/** UC-08: evolución semanal de la cotización de un jugador. `cotizaciones` ya viene ordenado
 * cronológicamente (ver `CotizacionService#obtenerHistorial`). */
export function CotizacionChart({ cotizaciones }: CotizacionChartProps) {
  if (cotizaciones.length === 0) {
    return (
      <p className={styles.sinDatos}>
        Este jugador todavía no tiene cotización calculada.
      </p>
    )
  }

  const valores = cotizaciones.map((c) => Number(c.valor))
  const ultimo = valores.at(-1)!
  const primero = valores.at(0)!
  const variacion = primero === 0 ? 0 : ((ultimo - primero) / primero) * 100
  const sube = variacion >= 0

  return (
    <div>
      <div className={styles.cabecera}>
        <div>
          <small>Última cotización ({cotizaciones.at(-1)!.semana})</small>
          <div className={styles.valorActual}>{formatMoney(ultimo)}</div>
        </div>
        {cotizaciones.length > 1 && (
          <span className={sube ? styles.up : styles.down}>
            {sube ? '▲' : '▼'} {Math.abs(variacion).toFixed(1)}% desde{' '}
            {cotizaciones.at(0)!.semana}
          </span>
        )}
      </div>

      {cotizaciones.length > 1 && (
        <svg
          className={styles.chart}
          viewBox="0 0 800 200"
          preserveAspectRatio="none"
          role="img"
          aria-label={`Cotización de las últimas ${cotizaciones.length} semanas, ${
            sube ? 'en suba' : 'en baja'
          }`}
        >
          <polyline
            points={puntosDe(valores)}
            fill="none"
            stroke={sube ? 'currentColor' : 'var(--down-color, #c0392b)'}
            strokeWidth="3"
            strokeLinejoin="round"
            vectorEffect="non-scaling-stroke"
            className={sube ? styles.up : styles.down}
          />
        </svg>
      )}
    </div>
  )
}

function puntosDe(valores: number[]): string {
  const min = Math.min(...valores)
  const max = Math.max(...valores)
  const rango = max - min || 1
  const pasoX = valores.length > 1 ? 800 / (valores.length - 1) : 0
  return valores
    .map((v, i) => {
      const x = i * pasoX
      const y = 190 - ((v - min) / rango) * 180
      return `${x},${y}`
    })
    .join(' ')
}
