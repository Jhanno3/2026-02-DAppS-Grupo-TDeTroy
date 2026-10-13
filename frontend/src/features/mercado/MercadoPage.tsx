import { useMemo, useState } from 'react'
import { useQueries, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  listarCotizaciones,
  listarJugadores,
  type JugadorResponse,
} from '../../api/jugadores'
import { Avatar } from '../../shared/components/Avatar'
import { formatMoney } from '../../shared/format'
import styles from './MercadoPage.module.css'

type Orden = 'nombre' | 'precio-desc' | 'precio-asc' | 'suba' | 'disponibles'

interface JugadorConPrecio {
  jugador: JugadorResponse
  precio: number | null
  variacion: number | null
}

/**
 * Catálogo público (UC-07) con la cotización vigente de cada jugador (UC-08) — reemplaza a la
 * antigua pantalla de catálogo. `GET /jugadores` no trae cotización embebida (plan.md: "sin
 * entidad Portfolio propia" aplica el mismo criterio acá), así que se pide una vez por jugador;
 * para el tamaño de catálogo actual es liviano, y react-query cachea cada una por `jugadorId` —
 * el detalle del jugador reutiliza esa misma cache key.
 */
export function MercadoPage() {
  const [pos, setPos] = useState('Todos')
  const [busqueda, setBusqueda] = useState('')
  const [orden, setOrden] = useState<Orden>('nombre')

  const jugadoresQuery = useQuery({
    queryKey: ['jugadores'],
    queryFn: listarJugadores,
  })

  const cotizacionesQueries = useQueries({
    queries: (jugadoresQuery.data ?? []).map((j) => ({
      queryKey: ['cotizaciones', j.id],
      queryFn: () => listarCotizaciones(j.id),
    })),
  })

  const isLoading =
    jugadoresQuery.isLoading || cotizacionesQueries.some((q) => q.isLoading)
  const isError = jugadoresQuery.isError

  const jugadores: JugadorConPrecio[] = useMemo(() => {
    if (!jugadoresQuery.data) return []
    return jugadoresQuery.data.map((jugador, i) => {
      const cotizaciones = cotizacionesQueries[i]?.data ?? []
      const valores = cotizaciones.map((c) => Number(c.valor))
      const precio = valores.at(-1) ?? null
      const variacion =
        valores.length > 1
          ? ((valores.at(-1)! - valores.at(0)!) / valores.at(0)!) * 100
          : null
      return { jugador, precio, variacion }
    })
  }, [jugadoresQuery.data, cotizacionesQueries])

  const posiciones = useMemo(
    () => [
      'Todos',
      ...new Set((jugadoresQuery.data ?? []).map((j) => j.posicion)),
    ],
    [jugadoresQuery.data],
  )

  const visibles = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    const filtrados = jugadores.filter(
      ({ jugador }) =>
        (pos === 'Todos' || jugador.posicion === pos) &&
        (!q ||
          jugador.nombre.toLowerCase().includes(q) ||
          jugador.club.toLowerCase().includes(q)),
    )
    const sorts: Record<
      Orden,
      (a: JugadorConPrecio, b: JugadorConPrecio) => number
    > = {
      nombre: (a, b) => a.jugador.nombre.localeCompare(b.jugador.nombre),
      'precio-desc': (a, b) => (b.precio ?? 0) - (a.precio ?? 0),
      'precio-asc': (a, b) => (a.precio ?? 0) - (b.precio ?? 0),
      suba: (a, b) => (b.variacion ?? -Infinity) - (a.variacion ?? -Infinity),
      disponibles: (a, b) =>
        b.jugador.tokensDisponibles - a.jugador.tokensDisponibles,
    }
    return [...filtrados].sort(sorts[orden])
  }, [jugadores, pos, busqueda, orden])

  return (
    <div className={styles.scope}>
      <main className={styles.wrap}>
        <div className={styles.toolbar}>
          <div>
            <h1>Mercado</h1>
            <p className={styles.lead}>
              Todos los jugadores disponibles, con su cotización vigente.
            </p>
          </div>
          <div className={styles.controls}>
            <label className={styles.field}>
              Buscar
              <input
                type="search"
                placeholder="Jugador o club"
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
              />
            </label>
            <label className={styles.field}>
              Ordenar por
              <select
                value={orden}
                onChange={(e) => setOrden(e.target.value as Orden)}
              >
                <option value="nombre">Nombre</option>
                <option value="precio-desc">Precio: mayor a menor</option>
                <option value="precio-asc">Precio: menor a mayor</option>
                <option value="suba">Mayor suba</option>
                <option value="disponibles">Más disponibles</option>
              </select>
            </label>
          </div>
        </div>

        {posiciones.length > 1 && (
          <div
            className={styles.pills}
            role="group"
            aria-label="Filtrar por posición"
          >
            {posiciones.map((p) => (
              <button
                key={p}
                type="button"
                className={styles.pill}
                aria-pressed={p === pos}
                onClick={() => setPos(p)}
              >
                {p}
              </button>
            ))}
          </div>
        )}

        {isLoading && <p className={styles.lead}>Cargando mercado…</p>}
        {isError && (
          <p role="alert" className={styles.lead}>
            No se pudo cargar el mercado. Intentá de nuevo.
          </p>
        )}

        {!isLoading && !isError && (
          <section className={styles.grid} aria-label="Jugadores">
            {visibles.length === 0 ? (
              <p className={styles.empty}>
                No hay jugadores que coincidan con el filtro.
              </p>
            ) : (
              visibles.map((item) => (
                <Carta key={item.jugador.id} item={item} />
              ))
            )}
          </section>
        )}
      </main>
    </div>
  )
}

function Carta({ item }: { item: JugadorConPrecio }) {
  const { jugador, precio, variacion } = item
  const sube = (variacion ?? 0) >= 0
  const pct = Math.max(4, jugador.tokensDisponibles)

  return (
    <Link to={`/jugadores/${jugador.id}`} className={styles.card}>
      <div className={styles.cardPhoto}>
        <Avatar src={jugador.fotoUrl} nombre={jugador.nombre} size="fill" />
      </div>
      <div className={styles.cardBody}>
        <div>
          <div className={styles.cardNameRow}>
            <span className={styles.cardName}>{jugador.nombre}</span>
            {jugador.estado === 'INACTIVO' && (
              <span className={styles.badgeInactivo}>INACTIVO</span>
            )}
          </div>
          <div className={styles.cardClub}>
            {jugador.club} · {jugador.posicion}
          </div>
        </div>
        <div className={styles.cardPrice}>
          <strong>
            {precio !== null ? formatMoney(precio) : 'Sin cotización'}
          </strong>
          {variacion !== null && (
            <span className={sube ? styles.up : styles.down}>
              {sube ? '▲' : '▼'} {Math.abs(variacion).toFixed(1)}%
            </span>
          )}
        </div>
        <div>
          <div className={styles.supplyLabels}>
            <span>{jugador.tokensDisponibles} disponibles</span>
            <span>100 emitidos (máx.)</span>
          </div>
          <div className={styles.supplyBar}>
            <div style={{ width: `${pct}%` }} />
          </div>
        </div>
        <span className={styles.cta}>Ver y operar</span>
      </div>
    </Link>
  )
}
