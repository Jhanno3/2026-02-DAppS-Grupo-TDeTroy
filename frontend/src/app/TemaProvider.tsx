import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  CLAVE_TEMA,
  TemaContext,
  temaDelSistema,
  temaGuardado,
  type Tema,
} from './tema'

/**
 * Arranca con el tema que el usuario eligió la última vez; si nunca eligió, sigue al del sistema
 * (y lo sigue en vivo si el sistema cambia). Elegir con {@code alternar} lo deja fijo.
 */
export function TemaProvider({ children }: { children: ReactNode }) {
  const [elegido, setElegido] = useState<Tema | null>(temaGuardado)
  const [sistema, setSistema] = useState<Tema>(temaDelSistema)

  useEffect(() => {
    const consulta = window.matchMedia?.('(prefers-color-scheme: light)')
    if (!consulta) return
    const alCambiar = () => setSistema(consulta.matches ? 'claro' : 'oscuro')
    consulta.addEventListener('change', alCambiar)
    return () => consulta.removeEventListener('change', alCambiar)
  }, [])

  const tema = elegido ?? sistema

  const alternar = useCallback(() => {
    const nuevo: Tema = tema === 'claro' ? 'oscuro' : 'claro'
    localStorage.setItem(CLAVE_TEMA, nuevo)
    setElegido(nuevo)
  }, [tema])

  const valor = useMemo(() => ({ tema, alternar }), [tema, alternar])

  return <TemaContext.Provider value={valor}>{children}</TemaContext.Provider>
}
