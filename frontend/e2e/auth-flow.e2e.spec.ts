import { test, expect } from '@playwright/test'

/**
 * E2E del flujo UC-01 (registro) + UC-02 (login): navegador real contra el frontend en :5173 y
 * el backend en :8080, sin mockear `fetch` ni la API — a diferencia de los tests de MockMvc del
 * backend (que no levantan un servidor HTTP real) esto ejercita la app tal como la usa un usuario.
 *
 * Prerrequisitos (ver `.claude/skills/run-valuacion-jugadores/SKILL.md`):
 *   docker compose up -d db
 *   ./mvnw spring-boot:run        (repo root, puerto 8080)
 *   npm run dev                   (frontend/, puerto 5173)
 *
 * Correr con: cd frontend && npx playwright test
 */

function emailUnico() {
  return `e2e-${Date.now()}-${Math.floor(Math.random() * 1e6)}@test.com`
}

const PASSWORD = 'e2e-password-123'

test('un usuario nuevo se registra, inicia sesión, navega el catálogo y cierra sesión', async ({
  page,
}) => {
  const email = emailUnico()

  await page.goto('/registro')
  await page.locator('input[name="email"]').fill(email)
  await page.locator('input[name="password"]').fill(PASSWORD)
  await page.getByRole('button', { name: 'Crear cuenta' }).click()

  // UC-01 no autentica: redirige a /login con el mensaje de éxito.
  await expect(page).toHaveURL(/\/login$/)
  await expect(
    page.getByText('Cuenta creada. Ya podés iniciar sesión.'),
  ).toBeVisible()

  await page.locator('input[name="email"]').fill(email)
  await page.locator('input[name="password"]').fill(PASSWORD)
  await page.getByRole('button', { name: 'Ingresar' }).click()

  // Login exitoso redirige a home y el header pasa a mostrar la sesión iniciada.
  await expect(page).toHaveURL('/')
  await expect(page.getByText(`Sesión iniciada como ${email}.`)).toBeVisible()
  await expect(page.getByText(`${email} (USER)`)).toBeVisible()

  // El catálogo es una ruta pública, pero navegable ya autenticado.
  await page.getByRole('link', { name: 'Catálogo' }).click()
  await expect(page).toHaveURL(/\/jugadores$/)
  await expect(
    page.getByRole('heading', { name: 'Catálogo de jugadores' }),
  ).toBeVisible()

  await page.getByRole('button', { name: 'Salir' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('link', { name: 'Iniciar sesión' })).toBeVisible()
})

test('login con credenciales inválidas muestra el error del backend', async ({
  page,
}) => {
  await page.goto('/login')
  await page.locator('input[name="email"]').fill(emailUnico())
  await page.locator('input[name="password"]').fill('cualquier-password')
  await page.getByRole('button', { name: 'Ingresar' }).click()

  await expect(page.getByRole('alert')).toBeVisible()
  await expect(page).toHaveURL(/\/login$/)
})
