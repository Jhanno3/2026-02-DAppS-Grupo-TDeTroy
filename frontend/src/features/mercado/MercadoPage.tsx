import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  listarMercado,
  type JugadorConPrecioResponse,
  type JugadorResponse,
  type OrdenMercado,
} from '../../api/jugadores'
import { Avatar } from '../../shared/components/Avatar'
import { formatMoney } from '../../shared/format'
import styles from './MercadoPage.module.css'

const TAMANIO_PAGINA = 50

/**
 * Catálogo público (UC-07) con la cotización vigente de cada jugador (UC-08). Pide una única
 * página ya filtrada/ordenada/paginada por el backend (`GET /jugadores/mercado`) -- a diferencia
 * de la versión anterior, que traía el catálogo completo y pedía la cotización jugador por jugador
 * (cientos/miles de requests, lento con el catálogo real de ~2600 jugadores).
 */
export function MercadoPage() {
  const [pos, setPos] = useState('Todos')
  const [busquedaInput, setBusquedaInput] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [orden, setOrden] = useState<OrdenMercado>('precio-desc')
  const [pagina, setPagina] = useState(0)

  // Debounce de la búsqueda: evita pegarle al backend en cada tecla. Vuelve a la primera página
  // acá mismo (no en un efecto separado reaccionando a `busqueda`) para no encadenar renders.
  useEffect(() => {
    const id = setTimeout(() => {
      setBusqueda(busquedaInput.trim())
      setPagina(0)
    }, 300)
    return () => clearTimeout(id)
  }, [busquedaInput])

  const mercadoQuery = useQuery({
    queryKey: ['mercado', pagina, orden, pos, busqueda],
    queryFn: () =>
      listarMercado({
        pagina,
        tamanio: TAMANIO_PAGINA,
        orden,
        posicion: pos,
        busqueda,
      }),
  })

  const data = mercadoQuery.data
  const isLoading = mercadoQuery.isLoading
  const isError = mercadoQuery.isError
  const posiciones = ['Todos', ...(data?.posicionesDisponibles ?? [])]

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
                value={busquedaInput}
                onChange={(e) => setBusquedaInput(e.target.value)}
              />
            </label>
            <label className={styles.field}>
              Ordenar por
              <select
                value={orden}
                onChange={(e) => {
                  setOrden(e.target.value as OrdenMercado)
                  setPagina(0)
                }}
              >
                <option value="precio-desc">Precio: mayor a menor</option>
                <option value="precio-asc">Precio: menor a mayor</option>
                <option value="nombre">Nombre</option>
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
                onClick={() => {
                  setPos(p)
                  setPagina(0)
                }}
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

        {!isLoading && !isError && data && (
          <>
            <section className={styles.grid} aria-label="Jugadores">
              {data.contenido.length === 0 ? (
                <p className={styles.empty}>
                  No hay jugadores que coincidan con el filtro.
                </p>
              ) : (
                data.contenido.map((item) => (
                  <Carta key={item.jugador.id} item={item} />
                ))
              )}
            </section>

            {data.totalPaginas > 1 && (
              <div className={styles.pagination}>
                <button
                  type="button"
                  className={styles.pageBtn}
                  disabled={pagina === 0}
                  onClick={() => setPagina((p) => p - 1)}
                >
                  ← Anterior
                </button>
                <span className={styles.pageInfo}>
                  Página {data.pagina + 1} de {data.totalPaginas} (
                  {data.totalElementos} jugadores)
                </span>
                <button
                  type="button"
                  className={styles.pageBtn}
                  disabled={data.pagina + 1 >= data.totalPaginas}
                  onClick={() => setPagina((p) => p + 1)}
                >
                  Siguiente →
                </button>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  )
}

function Carta({ item }: { item: JugadorConPrecioResponse }) {
  const jugador: JugadorResponse = item.jugador
  const precio = item.precio === null ? null : Number(item.precio)
  const variacion =
    item.variacionPorcentual === null ? null : Number(item.variacionPorcentual)
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
