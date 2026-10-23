const BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export class ApiError extends Error {
  readonly status: number
  readonly title: string | undefined

  constructor(status: number, message: string, title: string | undefined) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.title = title
  }
}

interface ProblemDetail {
  title?: string
  detail?: string
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  token?: string | null
}

let manejadorNoAutenticado: (() => void) | null = null

/**
 * `AuthContext` se registra acá al montar para enterarse, sin que este módulo dependa de React,
 * de cualquier 401 que vuelva de una request ya autenticada (token vencido o invalidado) — así se
 * puede cerrar la sesión local automáticamente en vez de dejar al usuario viendo el mensaje crudo
 * del backend ("Se requiere un token válido...") contra un formulario que ya no puede completar.
 * Un 401 de `POST /auth/login` (credenciales inválidas, sin sesión previa) también dispara esto,
 * pero ahí es un no-op inofensivo (no hay token que limpiar) — `LoginPage` sigue mostrando su
 * propio mensaje de error igual, el `ApiError` se sigue lanzando en ambos casos.
 */
export function registrarManejadorNoAutenticado(handler: (() => void) | null) {
  manejadorNoAutenticado = handler
}

/**
 * Único punto de acceso HTTP al backend (constitution.md: prohibido `fetch`/`axios` sueltos en
 * componentes). Traduce las respuestas `application/problem+json` del `GlobalExceptionHandler`
 * del backend a {@link ApiError}, para que cada pantalla sólo maneje un tipo de error conocido.
 */
export async function apiFetch<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { method = 'GET', body, token } = options

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!response.ok) {
    if (response.status === 401) {
      manejadorNoAutenticado?.()
    }
    const problem: ProblemDetail | null = await response
      .json()
      .catch(() => null)
    throw new ApiError(
      response.status,
      problem?.detail ??
        'Ocurrió un error inesperado. Intentá de nuevo en unos minutos.',
      problem?.title,
    )
  }

  // 204 (DELETE /ofertas/{id}) y 202 sin cuerpo (POST /cotizaciones/recalculo, "dispara pero no
  // devuelve nada") no tienen body JSON que parsear — un `.json()` directo sobre texto vacío
  // tira SyntaxError. Se lee como texto primero y sólo se parsea si no está vacío.
  const texto = await response.text()
  return (texto === '' ? undefined : JSON.parse(texto)) as T
}
