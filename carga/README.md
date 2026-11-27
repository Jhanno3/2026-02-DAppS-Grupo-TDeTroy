# Prueba de carga y SLAs

## SLAs

| Operación | Objetivo |
|---|---|
| Consultas (ranking, detalle, historial, cotización por semana, portfolio) | p95 < 300 ms |
| Login | p95 < 500 ms |
| Errores | < 1 % de las requests |

Están como `thresholds` en [prueba-carga.js](prueba-carga.js): si no se cumplen, k6 termina con error.
En operación los vigila la alerta `LatenciaAlta` de Prometheus ([alertas.yml](../monitoreo/alertas.yml)).

## Cómo correrla

Con la base de dev con datos (`DevDataSeeder`) y el backend levantado:

```powershell
docker compose up -d --wait db
.\mvnw.cmd spring-boot:run
```

En otra terminal, desde la raíz del repo:

```powershell
docker run --rm -v "${PWD}/carga:/carga" grafana/k6:1.8.3 run /carga/prueba-carga.js
```

Variables opcionales (`-e NOMBRE=valor`): `VUS` (usuarios simultáneos, 50), `DURACION` (1m),
`BASE_URL` (http://host.docker.internal:8080), `USUARIO` y `PASSWORD` (user@example.com).

Cada usuario virtual navega en loop: ranking, detalle de un jugador, su historial, la cotización de
una semana y su portfolio, con 1 s de pausa; 5 % de las iteraciones además hace login. Solo lee: no
modifica datos de la base. Con Prometheus levantado, la carga se ve en vivo en
`http_server_requests_seconds`.

## Índice de V24

[indice-cotizaciones.sql](indice-cotizaciones.sql) carga 249.600 cotizaciones sintéticas, mide la
consulta "cotización del jugador X en la semana Y" sin y con el índice, y deshace todo (ROLLBACK):

```powershell
docker compose up -d --wait db-test
Get-Content carga\indice-cotizaciones.sql | docker exec -i valuacion-db-test psql -U valuacion_test -d valuacion_test -q
```

Requiere el esquema migrado en la base de test (cualquier `mvnw verify` previo lo deja).

| | Sin índice | Con índice |
|---|---|---|
| Plan | `idx_..._jugador_fecha` + filtro por semana | `idx_..._jugador_semana_fecha` |
| Filas leídas y descartadas | 1.236 | 0 |
| Bloques leídos | 92 | 4 |
| Tiempo de ejecución | 0,112 ms | 0,063 ms |

Sin el índice, el costo crece con el historial del jugador (una fila más por recálculo); con el
índice se mantiene constante.

## Resultados medidos (2026-11-27)

Notebook de desarrollo (Windows 11, Docker Desktop), backend y Postgres locales, base de dev con
2.652 jugadores y ~8.000 cotizaciones.

| | 50 usuarios, 1 min | 200 usuarios, 1 min |
|---|---|---|
| Requests | 18.015 (185/s) | 72.671 (846/s) |
| p95 consultas | 3,7 ms | 3,6 ms |
| p95 por endpoint | ranking 2,1 · jugador 2,8 · historial 3,4 · semana 3,3 · portfolio 4,0 ms | ranking 1,7 · jugador 2,6 · historial 3,3 · semana 3,3 · portfolio 4,0 ms |
| p95 login | 56 ms | 56 ms |
| Errores | 0,01 % (2) | 0 % |
| SLAs | ✅ | ✅ |

Los 2 errores de la primera corrida no llegaron al backend (la auditoría no registró fallas): fueron
cortes de conexión de la red de Docker Desktop del lado de k6.

El ranking es el más rápido por la caché (`CacheConfig`). Estos números son de una máquina local
sin red de por medio: sirven para comparar versiones, no como capacidad de producción.
