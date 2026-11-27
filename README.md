# 2026-02-DAppS-Grupo-TDeTroy

## Cómo levantar la aplicación

### Requisitos previos

- Docker Desktop (para la base de datos)
- JDK 21
- Node.js

### 1. Base de datos

```bash
docker compose up -d db
```

### 2. Backend (Spring Boot, puerto 8080)

Desde la raíz del repo:

```bash
./mvnw spring-boot:run
```

En PowerShell: `.\mvnw.cmd spring-boot:run`

### 3. Frontend (Vite/React, puerto 5173)

```bash
cd frontend
npm run dev
```

No se requieren variables de entorno: `application.properties` y la configuración de Vite usan valores por defecto para desarrollo local.

### 4. Métricas con Prometheus (opcional, puerto 9090)

Copiar `.env.example` a `.env`, definir `METRICS_TOKEN` y reiniciar el backend. Después:

```bash
docker compose up -d prometheus
```

El estado se ve en http://localhost:9090/targets y las alertas en http://localhost:9090/alerts.

### 5. Prueba de carga (opcional)

Ver [carga/README.md](carga/README.md): SLAs, cómo correrla con k6 (Docker) y resultados medidos.
