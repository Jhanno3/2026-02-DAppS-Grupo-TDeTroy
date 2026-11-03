import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { obtenerRanking } from '../../api/jugadores'
import { Avatar } from '../../shared/components/Avatar'
import { formatMoney } from '../../shared/format'
import styles from './RankingPage.module.css'

type EstrategiaKey = 'cotizacion' | 'variacion-semanal'

export function RankingPage() {
  const [estrategia, setEstrategia] = useState<EstrategiaKey>('cotizacion')

  const {
    data: ranking,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['ranking', estrategia],
    queryFn: () => obtenerRanking(estrategia),
  })

  function clasePosicion(index: number) {
    if (index === 0) return `${styles.posicionBadge} ${styles.posicion1}`
    if (index === 1) return `${styles.posicionBadge} ${styles.posicion2}`
    if (index === 2) return `${styles.posicionBadge} ${styles.posicion3}`
    return styles.posicionBadge
  }

  function renderVariacion(variacion: string | null) {
    if (!variacion) {
      return <span className={`${styles.variacion} ${styles.neutra}`}>-</span>
    }
    const num = parseFloat(variacion)
    if (isNaN(num) || num === 0) {
      return (
        <span className={`${styles.variacion} ${styles.neutra}`}>0.00%</span>
      )
    }
    const esPositivo = num > 0
    return (
      <span
        className={`${styles.variacion} ${esPositivo ? styles.sube : styles.baja}`}
      >
        {esPositivo ? '▲ +' : '▼ '}
        {Math.abs(num).toFixed(2)}%
      </span>
    )
  }

  return (
    <div className={styles.scope}>
      <main className={styles.wrap}>
        <div className={styles.header}>
          <h1>Ranking de Jugadores</h1>
          <p className={styles.lead}>
            Los mejores jugadores del mercado ordenados según la estrategia
            seleccionada.
          </p>
        </div>

        <div
          className={styles.tabs}
          role="tablist"
          aria-label="Estrategias de ranking"
        >
          <button
            type="button"
            role="tab"
            aria-selected={estrategia === 'cotizacion'}
            aria-pressed={estrategia === 'cotizacion'}
            className={styles.tab}
            onClick={() => setEstrategia('cotizacion')}
          >
            🏆 Mayor Cotización
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={estrategia === 'variacion-semanal'}
            aria-pressed={estrategia === 'variacion-semanal'}
            className={styles.tab}
            onClick={() => setEstrategia('variacion-semanal')}
          >
            📈 Mayor Crecimiento
          </button>
        </div>

        {isLoading && (
          <div className={styles.loading}>
            <p>Cargando ranking...</p>
          </div>
        )}

        {isError && (
          <div className={styles.error}>
            <p>No se pudo cargar el ranking. Intente nuevamente más tarde.</p>
          </div>
        )}

        {!isLoading && !isError && (!ranking || ranking.length === 0) && (
          <div className={styles.vacio}>
            <p>
              No hay jugadores disponibles para armar el ranking en este
              momento.
            </p>
          </div>
        )}

        {!isLoading && !isError && ranking && ranking.length > 0 && (
          <div className={styles.rankingList} role="list">
            {ranking.map((item, index) => {
              const jugador = item.jugador
              return (
                <article
                  key={jugador.id}
                  className={styles.rankingCard}
                  role="listitem"
                >
                  <div className={styles.leftGroup}>
                    <div className={clasePosicion(index)}>#{index + 1}</div>

                    <Avatar
                      src={jugador.fotoUrl}
                      nombre={jugador.nombre}
                      size="small"
                    />

                    <div className={styles.infoJugador}>
                      <div className={nombreFila}>
                        <Link
                          to={`/jugadores/${jugador.id}`}
                          className={styles.nombre}
                        >
                          {jugador.nombre}
                        </Link>
                        <span className={styles.tagPosicion}>
                          {jugador.posicion}
                        </span>
                      </div>
                      <span className={styles.subMeta}>
                        {jugador.club} • {jugador.liga}
                      </span>
                    </div>
                  </div>

                  <div className={styles.rightGroup}>
                    <div className={styles.valores}>
                      <span className={styles.precio}>
                        {item.precio ? formatMoney(item.precio) : '-'}
                      </span>
                      {renderVariacion(item.variacionPorcentual)}
                    </div>

                    <Link
                      to={`/jugadores/${jugador.id}`}
                      className={styles.botonVer}
                    >
                      Ver ficha
                    </Link>
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </main>
    </div>
  )
}

const nombreFila = styles.nombreFila
