/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    globals: true,
    css: true,
    // Sin esto, Vitest también recoge e2e/*.e2e.spec.ts (Playwright) por el patrón
    // default `**/*.spec.ts` y pelea con sus propios `test`/`describe` globales.
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
      reportsDirectory: './coverage',
      // DTOs/tipos sin lógica (sólo interfaces) y el entrypoint de Vite — nada que testear ahí,
      // igual criterio que JaCoCo en el backend (pom.xml: sólo mide services/model).
      exclude: ['src/main.tsx', 'src/vite-env.d.ts', '**/*.d.ts'],
    },
  },
})
