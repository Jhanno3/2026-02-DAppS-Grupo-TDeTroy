import { defineConfig, devices } from '@playwright/test'

/**
 * Config de e2e (Playwright) contra la app real: frontend (Vite, :5173) + backend
 * (Spring Boot, :8080) + Postgres, ya levantados — ver `.claude/skills/run-valuacion-jugadores/SKILL.md`
 * (sección "Run (human path)") para arrancarlos. No se administran acá los servidores porque el
 * backend depende de Docker/Postgres, fuera del alcance de `webServer` de Playwright.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: 'list',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
