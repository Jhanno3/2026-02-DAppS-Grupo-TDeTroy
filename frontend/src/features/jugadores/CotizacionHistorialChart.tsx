import { useState, type KeyboardEvent, type PointerEvent } from 'react'
import type { CotizacionResponse } from '../../api/jugadores'
import { formatMoney } from '../../shared/format'
import styles from './CotizacionHistorialChart.module.css'

interface CotizacionHistorialChartProps {
  cotizaciones: CotizacionResponse[]
}

// Coordenadas del viewBox — el SVG escala al ancho del panel conservando la proporción, así el
// texto de los ejes nunca se deforma.
const ANCHO = 640
const ALTO = 175
const MARGEN = { izquierda: 76, derecha: 16, arriba: 14, abajo: 28 }
const ANCHO_PLOT = ANCHO - MARGEN.izquierda - MARGEN.derecha
const ALTO_PLOT = ALTO - MARGEN.arriba - MARGEN.abajo
const LINEAS_GRILLA = 4
// Con muchas semanas los marcadores se vuelven ruido: se dibujan sólo hasta este umbral (el
// último punto y el resaltado por hover se dibujan siempre).
const MAX_MARCADORES = 24

/**
 * UC-08: historial semanal de cotización de un jugador, con crosshair + tooltip por semana (mouse
 * o flechas del teclado) y una vista de tabla equivalente. `cotizaciones` ya viene ordenado
 * cronológicamente (ver `CotizacionService#obtenerHistorial`).
 */
export function CotizacionHistorialChart({
  cotizaciones,
}: CotizacionHistorialChartProps) {
  const [activo, setActivo] = useState<number | null>(null)

  if (cotizaciones.length === 0) {
    return (
      <p className={styles.sinDatos}>
        Este jugador todavía no tiene historial de cotización.
      </p>
    )
  }

  const valores = cotizaciones.map((c) => Number(c.valor))
  const [yMin, yMax] = dominioY(valores)
  // Con una sola semana no hay escala que leer: sin etiquetas de eje, el punto contra el borde
  // derecho y el mensaje a la izquierda (como en modeloHTML/ficha-jugador-*.html).
  const unaSemana = cotizaciones.length === 1
  const x = (i: number) =>
    unaSemana
      ? ANCHO - MARGEN.derecha - 48
      : MARGEN.izquierda + (i / (cotizaciones.length - 1)) * ANCHO_PLOT
  const y = (v: number) =>
    MARGEN.arriba + ALTO_PLOT - ((v - yMin) / (yMax - yMin)) * ALTO_PLOT
  const base = MARGEN.arriba + ALTO_PLOT

  const puntos = valores.map((v, i) => `${x(i)},${y(v)}`).join(' ')
  const area = `M${x(0)},${base} L${puntos.replaceAll(' ', ' L')} L${x(
    valores.length - 1,
  )},${base} Z`
  const grilla = Array.from(
    { length: LINEAS_GRILLA + 1 },
    (_, i) => yMin + ((yMax - yMin) * i) / LINEAS_GRILLA,
  )
  const etiquetasX = indicesEtiquetaX(cotizaciones.length)
  const ultimo = cotizaciones.length - 1

  function indiceMasCercano(event: PointerEvent<SVGSVGElement>): number {
    const rect = event.currentTarget.getBoundingClientRect()
    const xViewBox = ((event.clientX - rect.left) / rect.width) * ANCHO
    if (cotizaciones.length === 1) return 0
    const relativo = (xViewBox - MARGEN.izquierda) / ANCHO_PLOT
    return Math.min(
      ultimo,
      Math.max(0, Math.round(relativo * (cotizaciones.length - 1))),
    )
  }

  function alTeclear(event: KeyboardEvent<SVGSVGElement>) {
    if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') {
      event.preventDefault()
      const paso = event.key === 'ArrowRight' ? 1 : -1
      setActivo((actual) =>
        Math.min(ultimo, Math.max(0, (actual ?? ultimo) + paso)),
      )
    } else if (event.key === 'Escape') {
      setActivo(null)
    }
  }

  const seleccionado = activo !== null ? cotizaciones[activo] : null

  return (
    <div>
      <div className={styles.contenedor}>
        <svg
          className={styles.chart}
          viewBox={`0 0 ${ANCHO} ${ALTO}`}
          role="img"
          aria-label={`Historial de cotización de ${cotizaciones.length} ${
            cotizaciones.length === 1 ? 'semana' : 'semanas'
          }, de ${formatMoney(valores[0])} a ${formatMoney(valores[ultimo])}`}
          tabIndex={0}
          onPointerMove={(e) => setActivo(indiceMasCercano(e))}
          onPointerLeave={() => setActivo(null)}
          onFocus={() => setActivo((actual) => actual ?? ultimo)}
          onBlur={() => setActivo(null)}
          onKeyDown={alTeclear}
        >
          <defs>
            <linearGradient id="cotizacionArea" x1="0" y1="0" x2="0" y2="1">
              <stop
                offset="0%"
                stopColor="var(--t-accent)"
                stopOpacity="0.22"
              />
              <stop offset="100%" stopColor="var(--t-accent)" stopOpacity="0" />
            </linearGradient>
          </defs>

          {grilla.map((v, i) => (
            <g key={v}>
              <line
                className={
                  i === LINEAS_GRILLA / 2 ? styles.grillaMedia : styles.grilla
                }
                x1={unaSemana ? 0 : MARGEN.izquierda}
                x2={unaSemana ? ANCHO : ANCHO - MARGEN.derecha}
                y1={y(v)}
                y2={y(v)}
              />
              {!unaSemana && (
                <text
                  className={styles.etiquetaEje}
                  x={MARGEN.izquierda - 8}
                  y={y(v)}
                  textAnchor="end"
                  dominantBaseline="middle"
                >
                  {formatMoney(v)}
                </text>
              )}
            </g>
          ))}

          {unaSemana && (
            <line
              className={styles.guia}
              x1={x(0)}
              x2={x(0)}
              y1={MARGEN.arriba}
              y2={ALTO - MARGEN.abajo / 2}
            />
          )}

          {!unaSemana &&
            etiquetasX.map((i) => (
              <text
                key={i}
                className={styles.etiquetaEje}
                x={x(i)}
                y={ALTO - 8}
                textAnchor={i === 0 ? 'start' : i === ultimo ? 'end' : 'middle'}
              >
                {cotizaciones[i].semana}
              </text>
            ))}

          {cotizaciones.length > 1 && (
            <>
              <path d={area} fill="url(#cotizacionArea)" />
              <polyline className={styles.linea} points={puntos} />
            </>
          )}

          {activo !== null && (
            <line
              className={styles.crosshair}
              x1={x(activo)}
              x2={x(activo)}
              y1={MARGEN.arriba}
              y2={base}
            />
          )}

          <circle
            className={styles.halo}
            cx={x(ultimo)}
            cy={y(valores[ultimo])}
            r={13}
          />

          {valores.map((v, i) =>
            i === activo ||
            i === ultimo ||
            cotizaciones.length <= MAX_MARCADORES ? (
              <circle
                key={cotizaciones[i].id}
                className={
                  i === activo ? styles.marcadorActivo : styles.marcador
                }
                cx={x(i)}
                cy={y(v)}
                r={i === activo ? 7 : i === ultimo ? 6 : 4}
              />
            ) : null,
          )}
        </svg>

        {activo === null && (
          <div
            className={styles.etiquetaUltimo}
            style={{
              left: `${(x(ultimo) / ANCHO) * 100}%`,
              top: `${(y(valores[ultimo]) / ALTO) * 100}%`,
            }}
          >
            {semanaCorta(cotizaciones[ultimo].semana)} ·{' '}
            {formatMoney(valores[ultimo])}
          </div>
        )}

        {cotizaciones.length === 1 && (
          <p className={styles.unaSemana}>
            La curva arranca con la segunda semana de cotización. Mientras
            tanto, mirá el rendimiento que mueve el precio.
          </p>
        )}

        {seleccionado && activo !== null && (
          <div
            className={styles.tooltip}
            role="status"
            style={{
              left: `${(x(activo) / ANCHO) * 100}%`,
              top: `${(y(valores[activo]) / ALTO) * 100}%`,
            }}
          >
            <span className={styles.tooltipSemana}>{seleccionado.semana}</span>
            <span className={styles.tooltipValor}>
              {formatMoney(seleccionado.valor)}
            </span>
            {seleccionado.origen === 'MANUAL' && (
              <span className={styles.tooltipOrigen}>Ajuste manual</span>
            )}
          </div>
        )}
      </div>

      <details className={styles.tabla}>
        <summary>Ver como tabla</summary>
        <table>
          <thead>
            <tr>
              <th scope="col">Semana</th>
              <th scope="col">Cotización</th>
              <th scope="col">Origen</th>
            </tr>
          </thead>
          <tbody>
            {cotizaciones.map((c) => (
              <tr key={c.id}>
                <td>{c.semana}</td>
                <td>{formatMoney(c.valor)}</td>
                <td>{c.origen === 'MANUAL' ? 'Manual' : 'Automático'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </div>
  )
}

/** "2026-W44" → "W44". */
function semanaCorta(semana: string): string {
  return semana.split('-').at(-1) ?? semana
}

/** Dominio del eje Y con aire arriba y abajo; con un único valor (o todos iguales) abre ±10%. */
function dominioY(valores: number[]): [number, number] {
  const min = Math.min(...valores)
  const max = Math.max(...valores)
  if (max === min) {
    const margen = Math.abs(min) * 0.1 || 1
    return [min - margen, max + margen]
  }
  const margen = (max - min) * 0.1
  return [min - margen, max + margen]
}

/** Primera, última y (si entra) una del medio — nunca una etiqueta por semana. */
function indicesEtiquetaX(cantidad: number): number[] {
  if (cantidad === 1) return [0]
  if (cantidad < 5) return [0, cantidad - 1]
  return [0, Math.floor((cantidad - 1) / 2), cantidad - 1]
}
