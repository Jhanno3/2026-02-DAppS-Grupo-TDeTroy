import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { comprarTokens, venderTokens } from '../../api/tokens'
import { obtenerPortfolio } from '../../api/portfolio'
import { ApiError } from '../../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatMoney } from '../../shared/format'
import styles from './TradePanel.module.css'

interface TradePanelProps {
  jugadorId: string
  /** `null` cuando el jugador todavía no tiene ninguna cotización calculada (UC-08). */
  cotizacionVigente: number | null
  tokensDisponibles: number
  estaActivo: boolean
}

type Modo = 'comprar' | 'vender'

/**
 * UC-09/UC-10: compra/venta de tokens contra el sistema, a la cotización vigente (nunca un precio
 * que elija el usuario — `TokenServiceImpl` la vuelve a calcular en el backend igual, esto es sólo
 * para mostrarle el total esperado antes de confirmar).
 */
export function TradePanel({
  jugadorId,
  cotizacionVigente,
  tokensDisponibles,
  estaActivo,
}: TradePanelProps) {
  const { token, isAuthenticated } = useAuth()
  const queryClient = useQueryClient()
  const [modo, setModo] = useState<Modo>('comprar')
  const [cantidad, setCantidad] = useState(1)
  const [confirmacion, setConfirmacion] = useState<string | null>(null)

  const portfolio = useQuery({
    queryKey: ['portfolio', 'me'],
    queryFn: () => obtenerPortfolio(token!),
    enabled: isAuthenticated,
  })

  const misCopias =
    portfolio.data?.tenencias.find((t) => t.jugadorId === jugadorId)
      ?.cantidad ?? 0

  const mutation = useMutation({
    mutationFn: () =>
      modo === 'comprar'
        ? comprarTokens(jugadorId, cantidad, token!)
        : venderTokens(jugadorId, cantidad, token!),
    onSuccess: (movimiento) => {
      queryClient.invalidateQueries({ queryKey: ['portfolio', 'me'] })
      queryClient.invalidateQueries({ queryKey: ['jugadores', jugadorId] })
      setConfirmacion(
        `${modo === 'comprar' ? 'Compraste' : 'Vendiste'} ${movimiento.cantidad} token(s) por ${formatMoney(movimiento.montoTotal)}.`,
      )
      setCantidad(1)
    },
  })

  if (!isAuthenticated) {
    return (
      <div className={styles.panel}>
        <p>
          <Link to="/login">Iniciá sesión</Link> o{' '}
          <Link to="/registro">creá una cuenta</Link> para comprar o vender
          tokens de este jugador.
        </p>
      </div>
    )
  }

  if (!estaActivo) {
    return (
      <div className={styles.panel}>
        <p>Este jugador fue dado de baja y ya no admite operaciones.</p>
      </div>
    )
  }

  const max = modo === 'comprar' ? tokensDisponibles : misCopias
  const precioUnitario = cotizacionVigente ?? 100
  const total = precioUnitario * cantidad

  function cambiarModo(nuevoModo: Modo) {
    setModo(nuevoModo)
    setCantidad(1)
    setConfirmacion(null)
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setConfirmacion(null)
    mutation.mutate()
  }

  return (
    <div className={styles.panel}>
      <div
        className={styles.tabs}
        role="tablist"
        aria-label="Tipo de operación"
      >
        <button
          type="button"
          role="tab"
          aria-selected={modo === 'comprar'}
          onClick={() => cambiarModo('comprar')}
        >
          Comprar
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={modo === 'vender'}
          onClick={() => cambiarModo('vender')}
        >
          Vender
        </button>
      </div>

      {max === 0 ? (
        <p className={styles.hint}>
          {modo === 'comprar'
            ? 'No quedan tokens disponibles para comprar.'
            : 'No tenés tokens de este jugador para vender.'}
        </p>
      ) : (
        <form className={styles.form} onSubmit={handleSubmit}>
          <label className={styles.label} htmlFor="cantidad">
            Cantidad de tokens
          </label>
          <div className={styles.stepper}>
            <button
              type="button"
              aria-label="Restar"
              onClick={() => setCantidad((c) => Math.max(1, c - 1))}
            >
              −
            </button>
            <input
              id="cantidad"
              type="number"
              min={1}
              max={max}
              value={cantidad}
              onChange={(e) =>
                setCantidad(
                  Math.min(max, Math.max(1, parseInt(e.target.value, 10) || 1)),
                )
              }
            />
            <button
              type="button"
              aria-label="Sumar"
              onClick={() => setCantidad((c) => Math.min(max, c + 1))}
            >
              +
            </button>
          </div>
          <p className={styles.hint}>
            {modo === 'comprar'
              ? `Máximo ${max} disponibles para comprar`
              : `Tenés ${max} token(s) disponibles para vender`}
          </p>

          <div className={styles.kv}>
            <span>Precio por token</span>
            <span>{formatMoney(precioUnitario)}</span>
          </div>
          <div className={styles.total}>
            <span>Total</span>
            <strong>{formatMoney(total)}</strong>
          </div>

          {mutation.error != null && (
            <p role="alert" className={styles.error}>
              {mutation.error instanceof ApiError
                ? mutation.error.message
                : 'No se pudo completar la operación.'}
            </p>
          )}
          {confirmacion && <p className={styles.success}>{confirmacion}</p>}

          <button
            type="submit"
            className={styles.btnPrimary}
            disabled={mutation.isPending}
          >
            {mutation.isPending
              ? 'Procesando…'
              : modo === 'comprar'
                ? 'Confirmar compra'
                : 'Confirmar venta'}
          </button>
        </form>
      )}
    </div>
  )
}
