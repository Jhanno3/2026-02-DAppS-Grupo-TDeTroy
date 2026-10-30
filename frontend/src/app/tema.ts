import { createContext, useContext } from 'react'

export type Tema = 'claro' | 'oscuro'

export interface TemaContextValue {
  tema: Tema
  alternar: () => void
}

export const TemaContext = createContext<TemaContextValue | null>(null)

export const CLAVE_TEMA = 'valuacion.tema'

/** Tema guardado por el usuario; `null` si nunca eligió (se sigue al sistema). */
export function temaGuardado(): Tema | null {
  const valor = localStorage.getItem(CLAVE_TEMA)
  return valor === 'claro' || valor === 'oscuro' ? valor : null
}

export function temaDelSistema(): Tema {
  return window.matchMedia?.('(prefers-color-scheme: light)').matches
    ? 'claro'
    : 'oscuro'
}

export function useTema(): TemaContextValue {
  const contexto = useContext(TemaContext)
  if (!contexto) {
    throw new Error('useTema tiene que usarse dentro de <TemaProvider>')
  }
  return contexto
}
