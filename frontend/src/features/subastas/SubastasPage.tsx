import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { listarJugadores, type JugadorResponse } from '../../api/jugadores'
import { obtenerPortfolio } from '../../api/portfolio'
import { listarSubastas, type SubastaResponse } from '../../api/subastas'
import { useAuth } from '../auth/AuthContext'
import { Avatar } from '../../shared/components/Avatar'
import { Button } from '../../shared/components/Button'
import { SelectorJugadorPropio } from '../../shared/components/SelectorJugadorPropio'
import { jugadoresConTokensDe } from '../../shared/jugadoresConTokens'
import { formatMoney } from '../../shared/format'
import { formatRestante, useMsRestantes } from './cuentaRegresiva'
import styles from './Subastas.module.css'

// Por debajo de esto el reloj se marca como urgente.
const UMBRAL_URGENTE_MS = 2 * 60 * 1000

/**
 * UC-18: subastas activas de todo el catálogo, públicas. Se refresca sola cada pocos segundos para
 * que el precio y el reloj de cada subasta sigan las pujas de otros usuarios.
 */
export function SubastasPage() {
  const { token, user, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [selectorAbierto, setSelectorAbierto] = useState(false)

  const subastas = useQuery({
    queryKey: ['subastas'],
    queryFn: () => listarSubastas(),
    refetchInterval: 5000,
  })

  const jugadores = useQuery({
    queryKey: ['jugadores'],
    queryFn: listarJugadores,
  })

  const portfolio = useQuery({
    queryKey: ['portfolio', 'me'],
    queryFn: () => obtenerPortfolio(token!),
    enabled: isAuthenticated && selectorAbierto,
  })

  const jugadoresPorId = useMemo(
    () => new Map((jugadores.data ?? []).map((j) => [j.id, j])),
    [jugadores.data],
  )

  const jugadoresConTokens = useMemo(
    () => jugadoresConTokensDe(portfolio.data?.tenencias, jugadoresPorId),
    [portfolio.data, jugadoresPorId],
  )

  function handleCrearSubasta() {
    if (!isAuthenticated) {
      void navigate('/login')
      return
    }
    setSelectorAbierto(true)
  }

  return (
    <div className={styles.scope}>
      <main className={styles.container}>
        <div className={styles.toolbar}>
          <div>
            <h1 className={styles.titulo}>Subastas</h1>
            <p className={styles.bajada}>
              Cada puja sube el precio 5, 10 o 20%. Si pasan 2 minutos sin pujas
              nuevas, gana el mejor postor y se lleva todos los tokens de la
              subasta.
            </p>
          </div>
          <Button variant="primary" onClick={handleCrearSubasta}>
            Crear subasta
          </Button>
        </div>

        {subastas.isLoading && <p>Cargando subastas…</p>}
        {subastas.isError && (
          <p role="alert">No se pudieron cargar las subastas.</p>
        )}

        {subastas.data && subastas.data.length === 0 && (
          <p className={styles.hint}>
            No hay subastas activas en este momento.
          </p>
        )}

        {subastas.data && subastas.data.length > 0 && (
          <section className={styles.grid} aria-label="Subastas activas">
            {subastas.data.map((subasta) => (
              <TarjetaSubasta
                key={subasta.id}
                subasta={subasta}
                jugador={jugadoresPorId.get(subasta.jugadorId)}
                usuarioId={user?.usuarioId}
              />
            ))}
          </section>
        )}
      </main>

      {selectorAbierto && (
        <SelectorJugadorPropio
          jugadores={jugadoresConTokens}
          cargando={portfolio.isLoading}
          destinoDe={(jugadorId) => `/jugadores/${jugadorId}/subasta`}
          ariaLabel="Elegí un jugador para crear una subasta"
          onClose={() => setSelectorAbierto(false)}
        />
      )}
    </div>
  )
}

function TarjetaSubasta({
  subasta,
  jugador,
  usuarioId,
}: {
  subasta: SubastaResponse
  jugador: JugadorResponse | undefined
  usuarioId: string | undefined
}) {
  const restante = useMsRestantes(subasta.cierraEn)
  const precio = subasta.precioActual ?? subasta.precioInicial
  const etiqueta =
    subasta.vendedorId === usuarioId
      ? 'Tu subasta'
      : subasta.mejorPostorId === usuarioId
        ? 'Vas ganando'
        : null

  return (
    <Link to={`/subastas/${subasta.id}`} className={styles.card}>
      <div className={styles.cardJugador}>
        <Avatar
          src={jugador?.fotoUrl ?? null}
          nombre={jugador?.nombre ?? '?'}
        />
        <span>
          <span className={styles.cardNombre}>
            {jugador?.nombre ?? 'Jugador'}
          </span>
          <span className={styles.cardClub}>{jugador?.club}</span>
        </span>
      </div>

      <div className={styles.cardPrecio}>
        <small>
          {subasta.precioActual ? 'Mejor puja' : 'Precio inicial'} · por token
        </small>
        <strong>{formatMoney(precio)}</strong>
      </div>

      <div className={styles.cardPie}>
        <span>
          {subasta.cantidad} token(s) · {subasta.cantidadPujas}{' '}
          {subasta.cantidadPujas === 1 ? 'puja' : 'pujas'}
          {etiqueta && ` · ${etiqueta}`}
        </span>
        <span
          className={`${styles.reloj} ${
            restante < UMBRAL_URGENTE_MS ? styles.relojUrgente : ''
          }`}
          aria-label={`Cierra en ${formatRestante(restante)}`}
        >
          {restante > 0 ? formatRestante(restante) : 'Cerrando…'}
        </span>
      </div>
    </Link>
  )
}
