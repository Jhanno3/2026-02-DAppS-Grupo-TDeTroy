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

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
