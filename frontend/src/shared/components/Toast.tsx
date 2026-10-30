import { useEffect } from 'react'
import styles from './Toast.module.css'

interface ToastProps {
  mensaje: string
  onClose: () => void
  /** Milisegundos hasta cerrarse solo. */
  duracion?: number
}

/**
 * Aviso flotante de éxito (tilde verde + mensaje) que se cierra solo a los `duracion` ms o con la
 * ✕. `role="status"` para que los lectores de pantalla lo anuncien sin robar el foco.
 */
export function Toast({ mensaje, onClose, duracion = 3500 }: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(onClose, duracion)
    return () => clearTimeout(timer)
  }, [onClose, duracion])

  return (
    <div className={styles.toast} role="status">
      <svg
        className={styles.icono}
        viewBox="0 0 24 24"
        aria-hidden="true"
        focusable="false"
      >
        <circle cx="12" cy="12" r="11" />
        <path d="M7 12.5l3.2 3.2L17 9" />
      </svg>
      <span className={styles.mensaje}>{mensaje}</span>
      <button
        type="button"
        className={styles.cerrar}
        onClick={onClose}
        aria-label="Cerrar aviso"
      >
        ✕
      </button>
    </div>
  )
}
