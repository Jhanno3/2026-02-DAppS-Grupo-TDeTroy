# Frontend — App de Valuación de Jugadores

React + TypeScript (`strict`) + Vite.

## Estructura

```
src/
  features/   # componentes, hooks y tipos por feature de negocio
  shared/     # components/hooks/utils reutilizables entre features
  api/        # capa centralizada de acceso a la API (nunca fetch suelto en componentes)
  app/        # routing, providers, configuración global
```

## Scripts

- `npm run dev` — servidor de desarrollo
- `npm run build` — type-check (`tsc -b`) + build de producción
- `npm run lint` — ESLint
- `npm run format` / `npm run format:check` — Prettier
