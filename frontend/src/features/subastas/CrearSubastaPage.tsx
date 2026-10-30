import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { listarCotizaciones, obtenerJugador } from '../../api/jugadores'
import { obtenerPortfolio } from '../../api/portfolio'
import { crearSubasta } from '../../api/subastas'
import { useAuth } from '../auth/AuthContext'
import { Avatar } from '../../shared/components/Avatar'
import { formatMoney } from '../../shared/format'
import styles from './Subastas.module.css'

const DURACIONES = [
  { minutos: 15, etiqueta: '15 minutos' },
  { minutos: 30, etiqueta: '30 minutos' },
  { minutos: 60, etiqueta: '1 hora' },
  { minutos: 6 * 60, etiqueta: '6 horas' },
  { minutos: 24 * 60, etiqueta: '24 horas' },
  { minutos: 3 * 24 * 60, etiqueta: '3 días' },
]

/**
 * UC-18: el vendedor subasta tokens que posee de un jugador. El precio inicial arranca con la
 * cotización vigente sólo como referencia de mercado: el vendedor puede fijar cualquier otro.
 */
export function CrearSubastaPage() {
  const { id } = useParams<{ id: string }>()
  const { token } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [cantidad, setCantidad] = useState(1)
  // null = el usuario no tocó el campo todavía: se muestra la cotización vigente.
  const [precioEditado, setPrecioEditado] = useState<number | null>(null)
  const [duracionMinutos, setDuracionMinutos] = useState(30)

  const jugador = useQuery({
    queryKey: ['jugadores', id],
    queryFn: () => obtenerJugador(id!),
    enabled: Boolean(id),
  })

  const cotizaciones = useQuery({
    queryKey: ['cotizaciones', id],
    queryFn: () => listarCotizaciones(id!),
    enabled: Boolean(id),
  })

  const portfolio = useQuery({
    queryKey: ['portfolio', 'me'],
    queryFn: () => obtenerPortfolio(token!),
    enabled: Boolean(token),
  })

  const cotizacionVigente = cotizaciones.data?.at(-1)
    ? Number(cotizaciones.data.at(-1)!.valor)
    : null
  const precioInicial = precioEditado ?? cotizacionVigente ?? 1
  const disponibles =
    portfolio.data?.tenencias.find((t) => t.jugadorId === id)?.cantidad ?? 0

  const crear = useMutation({
    mutationFn: () =>
      crearSubasta(
        { jugadorId: id!, cantidad, precioInicial, duracionMinutos },
        token!,
      ),
    onSuccess: async (subasta) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['subastas'] }),
        queryClient.invalidateQueries({ queryKey: ['portfolio', 'me'] }),
      ])
      void navigate(`/subastas/${subasta.id}`, { state: { creada: true } })
    },
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    crear.mutate()
  }

  return (
    <div className={styles.scope}>
      <main className={styles.containerAngosto}>
        {id && (
          <Link to={`/jugadores/${id}`} className={styles.back}>
            ← Volver al jugador
          </Link>
        )}

        {jugador.data && (
          <div className={styles.header}>
            <div className={styles.headerJugador}>
              <Avatar
                src={jugador.data.fotoUrl}
                nombre={jugador.data.nombre}
                size="large"
              />
              <div>
                <h1>Subastar a {jugador.data.nombre}</h1>
                <p>
                  {jugador.data.club} · {jugador.data.posicion}
                </p>
              </div>
            </div>
          </div>
        )}

        <section className={styles.panel}>
          <h2>Cómo funciona</h2>
          <ul className={styles.reglas}>
            <li>
              Los tokens que subastás quedan reservados mientras la subasta esté
              activa.
            </li>
            <li>
              Cada puja sube el precio 5, 10 o 20%. La primera puede igualar el
              precio inicial.
            </li>
            <li>
              Desde la primera puja, si pasan <strong>2 minutos</strong> sin
              pujas nuevas, gana el mejor postor y se lleva todos los tokens.
            </li>
            <li>
              La duración es el plazo para que llegue la primera puja: si vence
              sin pujas, los tokens vuelven a tu cartera.
            </li>
            <li>Podés cancelarla en cualquier momento.</li>
          </ul>
        </section>

        <section className={`${styles.panel} ${styles.separado}`}>
          {portfolio.isLoading && <p>Cargando…</p>}
          {portfolio.data && disponibles === 0 && (
            <p className={styles.hint}>
              No tenés tokens disponibles de este jugador para subastar.
            </p>
          )}
          {disponibles > 0 && (
            <form className={styles.form} onSubmit={handleSubmit}>
              <h2>Nueva subasta (tenés {disponibles} disponibles)</h2>

              {crear.error && (
                <p role="alert" className={styles.error}>
                  {crear.error instanceof ApiError
                    ? crear.error.message
                    : 'No se pudo crear la subasta.'}
                </p>
              )}

              <div className={styles.formGrid}>
                <label className={styles.label}>
                  Cantidad de tokens
                  <input
                    type="number"
                    name="cantidad"
                    min={1}
                    max={disponibles}
                    value={cantidad}
                    onChange={(e) =>
                      setCantidad(
                        Math.min(
                          disponibles,
                          Math.max(1, Number.parseInt(e.target.value, 10) || 1),
                        ),
                      )
                    }
                  />
                </label>
                <label className={styles.label}>
                  Precio inicial por token
                  <input
                    type="number"
                    name="precioInicial"
                    min={0.01}
                    step={0.01}
                    value={precioInicial}
                    onChange={(e) =>
                      setPrecioEditado(
                        Math.max(
                          0.01,
                          Number.parseFloat(e.target.value) || 0.01,
                        ),
                      )
                    }
                  />
                  {cotizacionVigente !== null && (
                    <span className={styles.referencia}>
                      Cotización actual: {formatMoney(cotizacionVigente)}
                    </span>
                  )}
                </label>
                <label className={styles.label}>
                  Duración
                  <select
                    name="duracion"
                    value={duracionMinutos}
                    onChange={(e) => setDuracionMinutos(Number(e.target.value))}
                  >
                    {DURACIONES.map((d) => (
                      <option key={d.minutos} value={d.minutos}>
                        {d.etiqueta}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <p className={styles.hint}>
                Precio de arranque total:{' '}
                {formatMoney(precioInicial * cantidad)}
              </p>

              <button
                type="submit"
                className={styles.btnPrimario}
                disabled={crear.isPending}
              >
                {crear.isPending ? 'Creando…' : 'Iniciar subasta'}
              </button>
            </form>
          )}
        </section>
      </main>
    </div>
  )
}
