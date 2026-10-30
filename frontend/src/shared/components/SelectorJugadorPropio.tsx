import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import type { JugadorConTenencia } from '../jugadoresConTokens'
import { Avatar } from './Avatar'
import styles from './SelectorJugadorPropio.module.css'

interface SelectorJugadorPropioProps {
  /** Jugadores de los que el usuario tiene tokens disponibles. */
  jugadores: JugadorConTenencia[]
  cargando: boolean
  /** A dónde lleva elegir un jugador (ej. su página de oferta P2P o de subasta). */
  destinoDe: (jugadorId: string) => string
  ariaLabel: string
  onClose: () => void
}

/**
 * Modal para elegir uno de los jugadores propios antes de operar con sus tokens (publicar una
 * oferta P2P, crear una subasta). Usa las variables de color de la página que lo contiene, así que
 * tiene que renderizarse dentro de su `.scope`.
 */
export function SelectorJugadorPropio({
  jugadores,
  cargando,
  destinoDe,
  ariaLabel,
  onClose,
}: SelectorJugadorPropioProps) {
  const [busqueda, setBusqueda] = useState('')

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase()
    if (!q) return jugadores
    return jugadores.filter(
      ({ jugador }) =>
        jugador.nombre.toLowerCase().includes(q) ||
        jugador.club.toLowerCase().includes(q),
    )
  }, [jugadores, busqueda])

  return (
    <div
      className={styles.overlay}
      role="dialog"
      aria-modal="true"
      aria-label={ariaLabel}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className={styles.selector}>
        <div className={styles.selectorHeader}>
          <h2>Elegí un jugador</h2>
          <input
            type="search"
            name="buscarJugador"
            className={styles.selectorBuscador}
            placeholder="Buscar jugador o club"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            aria-label="Buscar jugador"
            autoFocus
          />
          <button
            type="button"
            className={styles.selectorClose}
            aria-label="Cerrar"
            onClick={onClose}
          >
            ✕
          </button>
        </div>

        {cargando && <p className={styles.hint}>Cargando…</p>}

        {!cargando && jugadores.length === 0 && (
          <p className={styles.hint}>
            No tenés tokens de ningún jugador todavía. Comprá primero en el{' '}
            <Link to="/mercado" onClick={onClose}>
              Mercado
            </Link>
            .
          </p>
        )}

        {jugadores.length > 0 && filtrados.length === 0 && (
          <p className={styles.hint}>
            Ningún jugador tuyo coincide con "{busqueda}".
          </p>
        )}

        {filtrados.length > 0 && (
          <ul className={styles.selectorLista}>
            {filtrados.map(({ tenencia, jugador }) => (
              <li key={tenencia.jugadorId}>
                <Link
                  to={destinoDe(tenencia.jugadorId)}
                  className={styles.selectorItem}
                  onClick={onClose}
                >
                  <Avatar src={jugador.fotoUrl} nombre={jugador.nombre} />
                  <div>
                    <div className={styles.nombre}>{jugador.nombre}</div>
                    <div className={styles.club}>
                      {jugador.club} · {jugador.posicion}
                    </div>
                  </div>
                  <span className={styles.selectorCantidad}>
                    {tenencia.cantidad} token(s)
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
