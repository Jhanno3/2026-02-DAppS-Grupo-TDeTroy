import { useState } from 'react'
import styles from './Avatar.module.css'

interface AvatarProps {
  src: string | null
  nombre: string
  /** `fill` ocupa todo el contenedor (sin recorte circular) — para la foto grande de una carta. */
  size?: 'small' | 'large' | 'fill'
}

function iniciales(nombre: string): string {
  const palabras = nombre.trim().split(/\s+/)
  const primera = palabras.at(0)?.[0] ?? ''
  const ultima = palabras.length > 1 ? (palabras.at(-1)?.[0] ?? '') : ''
  return (primera + ultima).toUpperCase()
}

/**
 * Foto de perfil de un jugador (scrapeada de WhoScored, ver backend
 * `RendimientoExternoRepository#obtenerFotoUrl`), con fallback a iniciales cuando `src` es nulo o
 * la imagen falla al cargar (dominio externo, puede caerse o cambiar de URL sin aviso).
 */
export function Avatar({ src, nombre, size = 'small' }: AvatarProps) {
  const [fallo, setFallo] = useState(false)
  const sizeClass =
    size === 'fill' ? styles.fill : size === 'large' ? styles.large : styles.small

  if (!src || fallo) {
    return (
      <div className={`${styles.avatar} ${styles.placeholder} ${sizeClass}`}>
        {iniciales(nombre)}
      </div>
    )
  }

  if (size === 'fill') {
    // La imagen completa tiene que verse, sin recortar (object-fit: contain) — si no es
    // cuadrada, el sobrante arriba/abajo o a los costados lo rellena un fondo difuminado de
    // la misma foto en vez de dejarlo vacío (misma técnica que Spotify/YouTube para portadas
    // que no calzan con el contenedor).
    return (
      <div className={`${styles.avatar} ${sizeClass} ${styles.fillWrapper}`}>
        <img src={src} alt="" aria-hidden="true" className={styles.fillBackdrop} />
        <img
          src={src}
          alt={`Foto de ${nombre}`}
          className={styles.fillForeground}
          loading="lazy"
          onError={() => setFallo(true)}
        />
      </div>
    )
  }

  return (
    <img
      src={src}
      alt={`Foto de ${nombre}`}
      className={`${styles.avatar} ${sizeClass}`}
      loading="lazy"
      onError={() => setFallo(true)}
    />
  )
}
