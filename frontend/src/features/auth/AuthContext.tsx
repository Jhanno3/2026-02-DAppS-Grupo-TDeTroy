import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
} from 'react'
import type { ReactNode } from 'react'
import { decodeJwt } from './jwt'

const TOKEN_STORAGE_KEY = 'valuacion.token'

export interface AuthUser {
  usuarioId: string
  email: string
  rol: 'USER' | 'ADMIN'
}

interface AuthContextValue {
  token: string | null
  user: AuthUser | null
  isAuthenticated: boolean
  login: (token: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function usuarioDesdeToken(token: string): AuthUser | null {
  try {
    const claims = decodeJwt(token)
    if (claims.exp * 1000 < Date.now()) {
      return null
    }
    return { usuarioId: claims.usuarioId, email: claims.sub, rol: claims.rol }
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem(TOKEN_STORAGE_KEY),
  )

  const login = useCallback((nuevoToken: string) => {
    localStorage.setItem(TOKEN_STORAGE_KEY, nuevoToken)
    setToken(nuevoToken)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    setToken(null)
  }, [])

  const user = useMemo(() => (token ? usuarioDesdeToken(token) : null), [token])

  const value = useMemo<AuthContextValue>(
    () => ({ token, user, isAuthenticated: user !== null, login, logout }),
    [token, user, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  }
  return context
}
