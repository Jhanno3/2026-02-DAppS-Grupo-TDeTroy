# Plan Técnico — App de Valuación de Jugadores de Fútbol

> Este documento define el **cómo**: diseño técnico, modelo de datos, componentes, contratos entre módulos y decisiones de implementación necesarias para construir lo descrito en `spec.md`, dentro del marco fijo e innegociable de `constitution.md`. No reabre ninguna decisión de producto — donde `spec.md` deja algo explícitamente derivado a esta etapa, acá se resuelve; donde esta etapa encuentra una decisión técnica sin resolver en la constitución, se resuelve acá y se marca como asunción técnica.

**Nota de alineación (revisión de arquitectura):** esta versión reemplaza una anterior que organizaba el backend por feature (`jugador/api/domain/application/infrastructure`, etc.). `constitution.md` fue actualizada para exigir una arquitectura clásica en capas técnicas — `Controller → Service → Repository → Model`, con paquetes `controllers/`, `services/`, `repositories/`, `model/`, `dto/` a nivel raíz (constitution §2/§3). Ninguna decisión de negocio, modelo de datos, contrato de API ni algoritmo cambia respecto de la versión anterior de este documento — sólo cambia **dónde vive cada pieza** y cómo se nombran las clases. `tasks.md` (rol `manager`) debe rehacerse a partir de esta versión.

---

## 0. Decisiones técnicas que este documento resuelve

`spec.md` deriva explícitamente 4 puntos a esta etapa, y `constitution.md` no deja preguntas técnicas abiertas (todas resueltas en su sección 5). Los cuatro se resuelven acá:

| # | Punto pendiente | Origen | Resolución técnica (este documento) |
|---|---|---|---|
| 1 | Ponderación de métricas de rendimiento en el cálculo de cotización | spec UC-06 | §6 — modelo de puntaje compuesto, pesos versionables, sin hardcodear en el algoritmo |
| 2 | Tratamiento de un jugador sin partidos en la semana | spec UC-06 | §6.3 — puntaje neutro + decaimiento leve por inactividad (nunca "salta" el cálculo) |
| 3 | Estrategias concretas de ranking (al menos 2) | spec UC-16 | §7 — Strategy pattern con 2 estrategias iniciales |
| 4 | Mecanismo de concurrencia sobre cantidad disponible en oferta P2P | constitution §2 | §9.3 — locking pesimista (`SELECT ... FOR UPDATE`) |

Los cuatro son decisiones puramente técnicas y quedan firmes. La cadencia de ingesta de WhoScored/Football-Data.org (antes listada acá como punto 5) ya no es una decisión de esta etapa: `constitution.md` §1 la fija de forma firme en semanal, alineada al job de cotización — se documenta como dato en §8.2, no como decisión pendiente.

---

## 1. Arquitectura general

Monolito de una sola aplicación desplegable (no microservicios), organizado por **capa técnica** a nivel de paquete raíz, tal como manda `constitution.md` §2/§3:

```
controllers/   # un Controller por entidad/proceso de negocio
services/      # interfaz + implementación por entidad/proceso de negocio
repositories/  # interfaces de acceso a datos + implementaciones (JPA sobre model/, y las de fuentes externas)
model/         # entidades JPA
dto/
  request/     # DTOs de entrada, `record`
  response/    # DTOs de salida, `record`
common/        # BigDecimal/RoundingMode centralizados, excepciones base
config/        # seguridad, CORS, OpenAPI, scheduling
```

**Flujo de dependencia obligatorio y unidireccional (constitution §2): `Controller → Service → Repository → Model ↔ DB`.** Ningún Controller llama a un Repository directamente; ningún Repository conoce a ningún Service; ningún Service conoce HTTP.

**Services de la aplicación** (cada uno definido como interfaz, con su implementación `<Nombre>Impl` inyectada por constructor — constitution §2):

| Service | Responsabilidad | Repository(s) que usa |
|---|---|---|
| `UsuarioService` | registro, autenticación, saldo virtual (débito/crédito), recarga por ADMIN | `UsuarioRepository` |
| `JugadorService` | catálogo (alta/edición/baja), invariante de 100 tokens emitidos, orquesta la baja (cancela ofertas abiertas y compensa tenencias) | `JugadorRepository`, `TenenciaTokenRepository`, `OfertaP2PRepository`, `MovimientoRepository` |
| `RendimientoService` | orquesta la ingesta semanal desde las fuentes externas y persiste el rendimiento crudo | `RendimientoExternoRepository`, `FixtureRepository`, `RendimientoPartidoRepository` |
| `CotizacionService` | motor de cálculo de cotización (job semanal + disparo manual), historial | `CotizacionHistoricaRepository`, `PesoMetricaRepository`, `RendimientoPartidoRepository`, `JugadorRepository` |
| `TokenService` | compra/venta de tokens contra el sistema | `JugadorRepository`, `TenenciaTokenRepository`, `UsuarioRepository`, `MovimientoRepository` |
| `PortfolioService` | vista de lectura del portfolio de un usuario | `TenenciaTokenRepository`, `CotizacionHistoricaRepository`, `UsuarioRepository` |
| `MovimientoService` | historial de movimientos de un usuario | `MovimientoRepository` |
| `OfertaService` | publicar/ejecutar/cancelar ofertas P2P | `OfertaP2PRepository`, `TenenciaTokenRepository`, `UsuarioRepository`, `MovimientoRepository` |
| `RankingService` | selecciona y aplica la `EstrategiaRanking` activa | `JugadorRepository`, `CotizacionHistoricaRepository` |
| `AuditoriaService` | escribe `RegistroAuditoria`; invocado desde otros Services dentro de su misma transacción | `RegistroAuditoriaRepository` |

`CotizacionService` es el **único punto de verdad** del precio (constitution §2): ningún otro Service calcula ni persiste un valor de cotización — `TokenService`, `OfertaService`, `PortfolioService` y `RankingService` sólo **leen** la cotización vigente vía `CotizacionHistoricaRepository`/`CotizacionService`.

---

## 2. Modelo (`model/`)

### 2.1 `Usuario`

```
Usuario (entidad JPA)
  id: UUID
  email: String (único)
  passwordHash: String
  rol: RolUsuario { USER, ADMIN }
  saldoVirtual: BigDecimal(19,2)
  fechaCreacion: Instant

Métodos propios (nunca setters públicos sobre saldoVirtual):
  debitarSaldo(BigDecimal monto)   -> valida saldo suficiente, lanza SaldoInsuficienteException
  acreditarSaldo(BigDecimal monto) -> valida monto > 0
```

Saldo inicial en alta (UC-01): `BigDecimal.ZERO`, escala 2.

### 2.2 `Jugador`

```
Jugador (entidad JPA)
  id: UUID
  nombre, club, posicion, fechaNacimiento, nacionalidad: campos identificatorios (detalle de listado exacto no bloqueante, ver spec §UC-03)
  estado: EstadoJugador { ACTIVO, INACTIVO }
  tokensEmitidos: int  (0..100, invariante propia de la entidad)
  cotizacionVigenteId: referencia a la última CotizacionHistorica (puede ser null hasta el primer cálculo)
  fechaUltimaActualizacionRendimiento: Instant (nullable)

Métodos propios:
  emitirTokens(int cantidad)   -> throws EmisionMaximaSuperadaException si tokensEmitidos + cantidad > 100
  liberarTokens(int cantidad)  -> usado en venta al sistema (UC-10), nunca supera el emitido histórico
  darDeBaja()                  -> estado = INACTIVO, tokensEmitidos queda congelado como registro histórico pero deja de aceptar operaciones
```

La invariante de **máximo 100 tokens emitidos** vive en este método (`emitirTokens`), no sólo en `JugadorService`, cumpliendo constitution §2. `tokensEmitidos` representa tokens en circulación (comprados y no devueltos); "tokens disponibles para compra" = `100 - tokensEmitidos`.

### 2.3 `RendimientoPartido` (insumo de ingesta, UC-05)

```
RendimientoPartido (entidad JPA)
  id: UUID
  jugadorId: UUID
  partidoExternoId: String   (id del partido según Football-Data.org, para deduplicar)
  fechaPartido: LocalDate
  semanaCalculo: String (ISO week, ej. "2026-W07") — semana a la que se imputa para el cálculo
  metricas: JSONB          -- TODAS las métricas que WhoScored entrega para ese jugador/partido, sin curar
  fuenteResultado: enum { FOOTBALL_DATA }  -- resultado/alineación/fixture del partido
  fechaIngesta: Instant
```

`metricas` se modela como **JSONB** (no columnas fijas) porque la spec (UC-05) exige incorporar "todas las métricas que efectivamente provee el proceso de scraping de WhoScored... sin curación previa de un subconjunto acotado de variables de negocio". Una tabla de columnas fijas violaría eso apenas WhoScored agregue/quite un campo. El motor de cotización (§6) sabe interpretar las claves conocidas dentro del JSON; claves nuevas no rompen la ingesta, sólo no participan del cálculo hasta que se las incorpore explícitamente a los pesos.

### 2.4 `CotizacionHistorica`

```
CotizacionHistorica (entidad JPA, append-only — nunca se actualiza, sólo se inserta)
  id: UUID
  jugadorId: UUID
  semana: String (ISO week)
  valor: BigDecimal(19,2)
  origen: enum { AUTOMATICO, MANUAL }
  fechaCalculo: Instant
```

"Cotización vigente" de un jugador = el registro con `fechaCalculo` más reciente para ese `jugadorId`. Nunca se hace `UPDATE` sobre un registro existente — así el historial (UC-08) es, por construcción, exactamente la secuencia de inserts, sin tablas paralelas que puedan desincronizarse.

### 2.5 `TenenciaToken` (base del portfolio)

No existe una entidad `Portfolio` separada: `PortfolioService` construye la vista de lectura combinando `TenenciaTokenRepository` (tenencias) y `CotizacionService`/`CotizacionHistoricaRepository` (valuación). Esto evita una segunda fuente de verdad sobre "cuántos tokens tiene un usuario de un jugador".

```
TenenciaToken (entidad JPA)
  id: UUID
  usuarioId: UUID
  jugadorId: UUID
  cantidad: int (≥0)              -- total en propiedad del usuario
  cantidadReservada: int (≥0, ≤ cantidad)  -- porción de `cantidad` comprometida en ofertas P2P abiertas (UC-17)
  UNIQUE(usuarioId, jugadorId)

Métodos propios:
  disponible(): int                    -- cantidad - cantidadReservada; es lo que puede venderse al sistema o publicarse en una nueva oferta
  acreditar(int cantidad)               -- compra al sistema, compra P2P, reintegro (cancelación/baja)
  debitar(int cantidad)                 -- venta al sistema (UC-10); throws TenenciaInsuficienteException si cantidad > disponible()
  reservar(int cantidad)                -- al publicar una oferta P2P (UC-17); throws TenenciaInsuficienteException si cantidad > disponible()
  liberarReserva(int cantidad)          -- al cancelar una oferta (total o parcialmente) o al cancelarla por baja del jugador; cantidadReservada -= cantidad
  ejecutarReserva(int cantidad)         -- al ejecutarse una compra sobre la oferta del vendedor: cantidad -= cantidad y cantidadReservada -= cantidad a la vez (los tokens salen de la tenencia)
```

La invariante `cantidadReservada ≤ cantidad` vive en estos métodos, no sólo en `OfertaService` — mismo criterio que la constitución exige para la invariante de 100 tokens en `Jugador` (constitution §2).

### 2.6 `Movimiento`

```
Movimiento (entidad JPA, append-only)
  id: UUID
  usuarioId: UUID
  jugadorId: UUID (nullable — una recarga de saldo no tiene jugador asociado)
  tipo: enum { COMPRA_SISTEMA, VENTA_SISTEMA, COMPRA_P2P, VENTA_P2P, RECARGA_SALDO, COMPENSACION_BAJA }
  cantidad: int (nullable para RECARGA_SALDO)
  precioUnitario: BigDecimal (nullable para RECARGA_SALDO)
  montoTotal: BigDecimal
  contraparteUsuarioId: UUID (nullable, sólo para COMPRA_P2P/VENTA_P2P — el otro usuario de la operación)
  fecha: Instant
```

Este es el registro **de cara al usuario** (UC-12), expuesto por `MovimientoService`. Es distinto del log de auditoría interno (§10), que registra además operaciones administrativas sin usuario final asociado (ej. alta de jugador).

### 2.7 `OfertaP2P` (P2P, UC-17)

```
OfertaP2P (entidad JPA)
  id: UUID
  vendedorId: UUID
  jugadorId: UUID
  cantidadPublicada: int
  cantidadDisponible: int   -- decrece con cada ejecución parcial/total
  estado: enum { ABIERTA, AGOTADA, CANCELADA }
  fechaCreacion: Instant

Métodos propios:
  ejecutarContra(int cantidadSolicitada) -> int cantidadEjecutada
     - nunca ejecuta más que cantidadDisponible
     - si cantidadDisponible llega a 0, estado = AGOTADA
```

### 2.8 `RegistroAuditoria`

```
RegistroAuditoria (entidad JPA, append-only, inmutable)
  id: UUID
  actorId: UUID (nullable — null = SISTEMA, ej. job semanal)
  accion: String (ej. "RECALCULO_MANUAL_COTIZACION", "BAJA_JUGADOR", "ALTA_JUGADOR")
  entidadAfectada: String, entidadId: UUID
  valoresAntes: JSONB (nullable), valoresDespues: JSONB
  fecha: Instant
```

### 2.9 `PesoMetrica` (configuración del motor de cotización, ver §6.2)

```
PesoMetrica (entidad JPA, tabla de configuración — NO hardcodeada en el algoritmo)
  clave: String  (debe matchear una key dentro de RendimientoPartido.metricas)
  peso: BigDecimal
  activo: boolean
```

---

## 3. Contratos de API (REST, `/api/v1`)

Todos los DTOs de entrada/salida son `record` de Java, ubicados en `dto/request`/`dto/response` (constitution §2/§3). Las entidades de `model/` nunca cruzan el borde del Controller — cada Controller devuelve DTOs.

| Método | Endpoint | Auth | UC | Controller | Notas |
|---|---|---|---|---|---|
| POST | `/auth/registro` | público | UC-01 | `AuthController` | crea cuenta USER, saldo 0 |
| POST | `/auth/login` | público | UC-02 | `AuthController` | devuelve JWT; error genérico sin indicar campo |
| GET | `/jugadores` | público | UC-07 | `JugadorController` | listado + cotización vigente de cada uno |
| GET | `/jugadores/{id}` | público | UC-07 | `JugadorController` | detalle + tokens disponibles + fecha última actualización de rendimiento |
| POST | `/jugadores` | ADMIN | UC-03 | `JugadorController` | alta |
| PUT | `/jugadores/{id}` | ADMIN | UC-04 | `JugadorController` | edición identificatoria, nunca toca cotización ni límite de tokens |
| POST | `/jugadores/{id}/baja` | ADMIN | UC-15 | `JugadorController` | inactivación + compensación (no es `DELETE` físico) |
| GET | `/jugadores/{id}/cotizaciones` | público | UC-08 | `JugadorController` | delega en `CotizacionService`; historial cronológico |
| GET | `/jugadores/ranking?estrategia={clave}` | público | UC-16 | `JugadorController` | delega en `RankingService`; `estrategia` opcional, default definido en §7 |
| POST | `/cotizaciones/recalculo` | ADMIN | UC-13 | `CotizacionController` | dispara el mismo algoritmo del job semanal; no acepta valor de cotización en el body |
| POST | `/jugadores/{id}/compras` | USER | UC-09 | `TokenController` | compra contra el sistema |
| POST | `/jugadores/{id}/ventas` | USER | UC-10 | `TokenController` | venta al sistema |
| GET | `/portfolios/me` | USER | UC-11 | `PortfolioController` | tenencias + valor + saldo |
| GET | `/movimientos/me` | USER | UC-12 | `MovimientoController` | historial propio, incluye P2P y compensaciones identificadas |
| POST | `/usuarios/{id}/saldo` | ADMIN | UC-14 | `UsuarioController` | recarga, monto > 0 |
| POST | `/ofertas` | USER | UC-17 | `OfertaController` | vendedor publica cantidad de un jugador que posee |
| GET | `/ofertas?jugadorId={id}` | público/USER | UC-17 | `OfertaController` | listado de ofertas abiertas (para que un comprador elija) |
| POST | `/ofertas/{id}/compras` | USER | UC-17 | `OfertaController` | comprador ejecuta contra la oferta, hasta la cantidad disponible |
| DELETE | `/ofertas/{id}` | USER (sólo dueño) | UC-17 | `OfertaController` | el vendedor cancela lo no vendido de su propia oferta; libera la reserva |

Errores: formato `application/problem+json` (constitution §3), traducidos desde excepciones de negocio lanzadas en el Service (constitution §2) por un manejador global (`@RestControllerAdvice`). `400` para validación de forma, `409 Conflict` para saldo/tenencia/emisión insuficiente, `403` para rol incorrecto, `404` para entidad inexistente.

---

## 4. Seguridad

- Spring Security + JWT stateless. `POST /auth/login` (`AuthController`) emite el token; el resto de los endpoints protegidos lo validan vía filtro.
- Roles `USER`/`ADMIN` como `GrantedAuthority`. Anotación `@PreAuthorize("hasRole('ADMIN')")` en los endpoints administrativos (UC-03, UC-04, UC-13, UC-14, UC-15) — nunca chequeo de rol embebido a mano en el Controller.
- Endpoints públicos (`GET /jugadores`, `GET /jugadores/{id}`, `GET /jugadores/{id}/cotizaciones`, `GET /jugadores/ranking`) explícitamente permitidos sin autenticación en la config de `SecurityFilterChain` (`config/`); todo lo demás requiere token válido por defecto (whitelist explícita, no blacklist).
- Passwords: BCrypt.
- Validación de input: Bean Validation en cada DTO de `dto/request` (`@Positive` en montos y cantidades, `@Email`, etc.), aplicada en el borde del Controller.

---

## 5. Transaccionalidad

Cada operación multi-entidad es un único método `@Transactional` en el `Service` correspondiente (`services/`):

- **Compra/venta contra el sistema (UC-09/UC-10):** `TokenService.comprar(...)` / `.vender(...)` — debita/acredita saldo, debita/acredita tenencia, emite/libera tokens del jugador, inserta `Movimiento`. Todo o nada.
- **Publicar oferta P2P (UC-17):** `OfertaService.publicar(...)` — `TenenciaToken.reservar(cantidad)` del vendedor + creación de `OfertaP2P(ABIERTA)`, en una transacción.
- **Ejecutar compra sobre oferta P2P (UC-17):** `OfertaService.ejecutar(...)` — los 4 movimientos (`TenenciaToken.ejecutarReserva(cantidad)` del vendedor, crédito saldo vendedor, débito saldo comprador, `TenenciaToken.acreditar(cantidad)` del comprador) + 2 registros de `Movimiento` (uno por usuario) + actualización de `cantidadDisponible`/`estado` de la oferta, en una transacción.
- **Cancelar oferta P2P (UC-17):** `OfertaService.cancelar(...)` — `TenenciaToken.liberarReserva(cantidadDisponible)` del vendedor + `OfertaP2P.estado = CANCELADA`, en una transacción. Sin `Movimiento` (no hubo venta sobre esa cantidad).
- **Baja de jugador (UC-15):** `JugadorService.darDeBaja(...)`, en este orden dentro de una única transacción: (1) por cada `OfertaP2P` en estado `ABIERTA` de ese jugador, `TenenciaToken.liberarReserva(cantidadDisponible)` de su vendedor y la marca `CANCELADA`; (2) por cada `TenenciaToken` (ya sin reserva pendiente) con `cantidad > 0` de ese jugador: la pone a 0, acredita saldo del usuario a la cotización del día anterior, inserta `Movimiento` tipo `COMPENSACION_BAJA`; (3) marca el jugador `INACTIVO`. Si hay N usuarios afectados (con o sin ofertas abiertas), es una única transacción que cubre a todos — una falla parcial no puede dejar a algunos usuarios compensados/reintegrados y a otros no.
- **Recálculo de cotización (UC-06/UC-13):** por diseño, cada jugador se calcula y persiste en su propia sub-transacción dentro de `CotizacionService` (no una transacción gigante para los ~cientos de jugadores del catálogo), para que un error puntual en un jugador no revierta el cálculo ya persistido de los demás — pero cada cálculo individual (leer rendimiento, calcular, insertar `CotizacionHistorica`, actualizar puntero de "vigente") sí es atómico.

---

## 6. Motor de cotización (UC-06, UC-13)

Único servicio: `CotizacionService.recalcular(TipoDisparo origen, String actorId?)`. Invocado tanto por el job semanal como por `CotizacionController` (disparo manual) — **nunca hay una segunda implementación**.

### 6.1 Cálculo por jugador

```
1. Traer todos los RendimientoPartido del jugador con semanaCalculo == semana actual (RendimientoPartidoRepository).
2. Calcular puntajeRendimiento (BigDecimal, normalizado) a partir de esas métricas (ver 6.2).
3. factorAjuste = clamp(puntajeRendimiento * SENSIBILIDAD, -0.20, +0.20)   // tope +-20%/semana, evita saltos irreales
4. cotizacionAnterior = última CotizacionHistorica del jugador (CotizacionHistoricaRepository) o BASE_INICIAL si no existe ninguna
5. cotizacionNueva = cotizacionAnterior * (1 + factorAjuste), redondeado con RoundingMode.HALF_UP, escala 2
6. INSERT CotizacionHistorica(jugador, semana, cotizacionNueva, origen) vía CotizacionHistoricaRepository
7. Actualizar puntero "vigente" del Jugador vía JugadorRepository
```

`BASE_INICIAL` (valor con el que arranca un jugador que nunca tuvo cálculo previo) es una constante de configuración, no un valor de negocio fijado por producto — se documenta en `application.yml` como `cotizacion.valor-inicial` (default `100.00`), ajustable sin tocar código.

### 6.2 Puntaje de rendimiento — pesos versionables

Las métricas que WhoScored provee no son fijas de antemano (spec UC-05/UC-06: "sin curación previa"). Para no hardcodear una fórmula rígida que se rompa cada vez que la fuente cambia sus campos, `CotizacionService` usa `PesoMetrica` (§2.9, vía `PesoMetricaRepository`): recorre las claves presentes en `metricas` que tengan un `PesoMetrica` activo, normaliza cada valor (min-max sobre el histórico reciente del propio jugador, para no necesitar una escala global) y las combina en una suma ponderada. **Una métrica nueva que WhoScored empiece a proveer queda disponible como dato desde el día 1 (persistida en el JSON), pero sólo participa del cálculo una vez que alguien le asigna un peso** — separando "ingesta completa sin curar" (requisito de producto, UC-05) de "qué pondera el algoritmo" (decisión técnica, ajustable sin migración de esquema).

Set inicial de pesos para el MVP (ejemplo, ajustable en runtime vía tabla, no vía deploy):

| Métrica | Peso relativo |
|---|---|
| rating (calificación WhoScored del partido) | alto |
| goles | alto |
| asistencias | medio-alto |
| pases completados (%) | medio |
| tiros a puerta | medio |
| intercepciones/recuperaciones | medio |
| minutos jugados | bajo (factor de exposición, no de calidad) |

### 6.3 Jugador sin rendimiento en la semana

Resuelve el punto derivado por `spec.md` (UC-06, flujo alternativo). El jugador **siempre** pasa por el paso 1-7 de §6.1 (nunca se salta el cálculo, cumpliendo "no queda congelada"): si no hay `RendimientoPartido` esa semana, `puntajeRendimiento = 0` más un **decaimiento leve por inactividad** (`factorAjuste = -DECAY_INACTIVIDAD`, ej. `-1%` configurable), en lugar de dejar el valor exactamente igual. Justificación técnica: un jugador sin partidos (lesión, suplente, sin fixture esa semana) razonablemente no debería sostener su cotización indefinidamente sin evidencia de rendimiento, y esto evita el caso degenerado de `factorAjuste = 0` que sería indistinguible, en la práctica, de "congelar" el valor. Es una decisión técnica documentada, no de producto — ajustable en `application.yml` (`cotizacion.decay-inactividad`).

### 6.4 Job semanal + disparo manual

- Job: un componente de scheduling en `config/` con `@Scheduled(cron = "0 0 3 * * MON")` (lunes 03:00, configurable) invoca `CotizacionServiceImpl.recalcular(AUTOMATICO, null)` **directamente** (no hay Controller de por medio: el trigger no es HTTP) sobre **todos los jugadores en estado `ACTIVO`** del catálogo (un jugador `INACTIVO` queda excluido y su cotización permanece congelada en el valor de la baja, ver UC-06/UC-15).
- Endpoint manual: `POST /cotizaciones/recalculo` (`CotizacionController`), `@PreAuthorize("hasRole('ADMIN')")`, invoca `CotizacionService.recalcular(MANUAL, actorId)` sobre el mismo universo (todos los jugadores `ACTIVO`, sin selección de un jugador puntual — spec UC-13). Cada jugador afectado genera un `RegistroAuditoria` vía `AuditoriaService` con valores antes/después (constitution §2, §4).
- El endpoint **no** acepta ningún campo de tipo cotización/valor en su request body — el DTO de `dto/request` está vacío (sólo dispara), cumpliendo la prohibición explícita de la constitución.
- Al ser un monolito de una sola instancia en el MVP (constitution §2, sin microservicios/colas sin ADR), no se introduce un distributed lock (ej. ShedLock) para el job — si en el futuro se escala horizontalmente, eso amerita su propio ADR antes de agregarlo.

---

## 7. Ranking (UC-16)

Patrón Strategy, resolviendo el punto derivado por `spec.md`. Las estrategias son beans de Spring que viven en `services/` junto al resto de la lógica de negocio:

```java
interface EstrategiaRanking {
    String clave();
    List<Jugador> ordenar(List<JugadorConCotizacion> catalogo);
}
```

Dos estrategias iniciales (registradas como beans, seleccionables por `clave`):

1. **`cotizacion`** (default) — ordena por cotización vigente descendente.
2. **`variacion-semanal`** — ordena por variación porcentual entre la cotización vigente y la penúltima, descendente (mide "quién subió más esta semana", no sólo quién vale más en absoluto).

`RankingService` recibe la `clave` (query param `estrategia` de `GET /jugadores/ranking`, leído por `JugadorController`) y selecciona el bean correspondiente; si se omite o no matchea ninguna clave registrada, usa `cotizacion`. La selección de estrategia es lógica de negocio — vive en `RankingService`, nunca en el Controller (constitution §2). Agregar una tercera estrategia a futuro es sólo un nuevo bean, sin tocar el Controller (Open/Closed).

---

## 8. Ingesta de datos externos (UC-05)

### 8.1 Repositories para fuentes externas (constitution §1: interfaz + implementación obligatoria)

```
repositories/
  RendimientoExternoRepository (interfaz)      -- obtenerRendimiento(jugador, rangoFechas) -> List<RendimientoCrudo>
  FixtureRepository (interfaz)                 -- obtenerPartidosDisputados(rangoFechas) -> List<PartidoCrudo>
  WhoScoredRepositoryImpl implements RendimientoExternoRepository
  FootballDataRepositoryImpl implements FixtureRepository
  RendimientoPartidoRepository                 -- Spring Data JPA, persistencia sobre model/RendimientoPartido
```

`RendimientoService` sólo conoce las interfaces `RendimientoExternoRepository`/`FixtureRepository` y el modelo interno (`RendimientoPartido`); nunca el formato de respuesta de WhoScored ni de Football-Data.org (constitution §1/§2). Cada implementación traduce y aísla sus propios errores (try/catch interno + logging), nunca propaga una excepción que tumbe al otro flujo.

### 8.2 Frecuencia de ingesta (constitution §1, decisión firme)

`constitution.md` §1 fija que ambas ingestas corren con la **misma cadencia semanal** que el job de recotización, completándose **antes** de que este corra dentro del mismo ciclo — nunca diaria ni bajo ninguna otra frecuencia mayor. El job de cotización corre los lunes 03:00 (§6.4), así que ambas ingestas se programan el domingo anterior, invocando `RendimientoService` directamente desde un componente de scheduling en `config/` (mismo patrón que §6.4: sin Controller de por medio, el trigger no es HTTP):

- `FootballDataRepositoryImpl` (fixtures/resultados/alineaciones), vía `RendimientoService.ingestarFixtures()`: `@Scheduled(cron = "0 0 1 * * SUN")` (domingo 01:00).
- `WhoScoredRepositoryImpl` (rendimiento detallado), vía `RendimientoService.ingestarRendimiento()`: `@Scheduled(cron = "0 0 2 * * SUN")` (domingo 02:00, una hora después), para scrapear el detalle sólo de partidos ya confirmados por Football-Data en el paso anterior — reduce scraping innecesario sobre partidos que todavía no se jugaron.
- Queda un margen de una hora (02:00 a 03:00) entre el fin de la ventana de ingesta y el inicio del cálculo semanal, y de 2 horas entre ambas fuentes.
- Ambos jobs corren **desacoplados del job de cotización** (constitution §1: nunca síncrono a un request de usuario; §2: el job de cotización sólo lee lo ya persistido, nunca dispara la ingesta él mismo).
- Reintentos: si una fuente falla en su ventana semanal, el dato de ese partido no aporta al puntaje de esa semana (§6.3); no hay cola de reintentos dedicada en el MVP — un fallo puntual se resuelve recién en el ciclo semanal siguiente.

Esta cadencia ya no es un valor de cron ajustable a discreción técnica: es una regla fija de `constitution.md` §1. Un cambio de frecuencia requiere reabrir la constitución con el dueño del producto, no sólo tocar el `@Scheduled`.

---

## 9. Operación P2P (UC-17)

### 9.1 Publicar oferta

`POST /ofertas` (`OfertaController`) — `OfertaService.publicar` invoca `TenenciaToken.reservar(cantidadPublicada)` del vendedor: si `cantidadPublicada > disponible()` (tenencia total menos lo ya reservado en otras ofertas), se rechaza por completo (`TenenciaInsuficienteException`, `409`). Si la reserva es válida, la cantidad queda **físicamente bloqueada** (no sólo validada de palabra): `cantidadReservada` del vendedor sube de inmediato, así que una venta al sistema (UC-10) o una segunda publicación no pueden tocar esos tokens mientras la oferta siga abierta. Crea `OfertaP2P(cantidadPublicada, cantidadDisponible = cantidadPublicada, estado = ABIERTA)` vía `OfertaP2PRepository`.

### 9.2 Ejecutar compra sobre una oferta

`POST /ofertas/{id}/compras` (`OfertaController`) con `{cantidad}`:

1. Validar `cantidad ≤ cantidadDisponible` de la oferta y saldo suficiente del comprador a la cotización vigente **en el momento de la ejecución** (nunca la cotización de cuando se publicó la oferta).
2. Ejecutar la transacción atómica de 4 movimientos (§5): `TenenciaToken.ejecutarReserva(cantidad)` del vendedor (descuenta `cantidad` y `cantidadReservada` a la vez), crédito de saldo al vendedor, débito de saldo al comprador, `TenenciaToken.acreditar(cantidad)` al comprador.
3. Registrar 2 `Movimiento` (tipo `VENTA_P2P` para el vendedor, `COMPRA_P2P` para el comprador), cruzados por `contraparteUsuarioId`, vía `MovimientoRepository`.
4. Registrar en `AuditoriaService`.
5. Si `cantidadDisponible` llega a 0, `OfertaP2P.estado = AGOTADA`.

### 9.3 Concurrencia sobre la cantidad disponible (resuelve punto pendiente de constitution §2)

**Decisión: locking pesimista** (`SELECT ... FOR UPDATE` sobre la fila `OfertaP2P`, vía `@Lock(LockModeType.PESSIMISTIC_WRITE)` en `OfertaP2PRepository`) al leer la oferta dentro de la transacción de ejecución.

Justificación: la constitución exige que "nunca se venda más cantidad que la publicada", con "el primer comprador cuya operación se confirma se la lleva". Con locking optimista (campo `version`), varios compradores concurrentes generarían `OptimisticLockException` y requerirían reintento manual en el cliente/service, complejizando el flujo sin beneficio real acá: las ofertas P2P no son de alta frecuencia de escritura (no es un order book de trading de alta velocidad, está explícitamente prohibido que lo sea), así que el costo de un lock pesimista de fila por unos milisegundos es aceptable y da corrección directa sin lógica de reintento. Se aplica **únicamente sobre la fila de la oferta**, nunca sobre precio (que nunca se negocia ni se toca en esta operación — se lee de `CotizacionService` fuera del lock).

### 9.4 Retirar oferta

`DELETE /ofertas/{id}` (`OfertaController`) — sólo el vendedor dueño, y sólo si la oferta está `ABIERTA` (con `cantidadDisponible > 0`). Es un requisito **explícito** de `spec.md` UC-17 ("el vendedor puede cancelar la venta si nadie se la compró"). `OfertaService.cancelar` invoca `TenenciaToken.liberarReserva(cantidadDisponible)` del vendedor y pasa `cantidadDisponible` a 0 y `estado = CANCELADA`, sin generar `Movimiento` (no hubo venta). Si la oferta ya está `AGOTADA` o `CANCELADA`, se rechaza (`409`).

### 9.5 Interacción con la baja de un jugador (UC-15/UC-17)

Si el jugador de una oferta `ABIERTA` es dado de baja, `JugadorService.darDeBaja` cancela esa oferta como parte de su propia transacción (§5): `TenenciaToken.liberarReserva(cantidadDisponible)` del vendedor y `OfertaP2P.estado = CANCELADA`, **antes** de calcular la compensación de ese vendedor — así su tenencia queda completa (incluida la que estaba reservada) al momento de compensarla a la cotización del día anterior. No hay un `Movimiento` de cancelación por separado; la compensación resultante ya cubre esa cantidad.

---

## 10. Auditoría vs. historial de movimientos

Dos mecanismos, con propósitos distintos (ambos mandatados por constitution §4):

- **`Movimiento`** (§2.6), vía `MovimientoRepository`: de cara al usuario, expuesto por `MovimientoService`/`MovimientoController` (`GET /movimientos/me`, UC-12). Sólo cubre operaciones que afectan a un usuario concreto.
- **`RegistroAuditoria`** (§2.8), vía `RegistroAuditoriaRepository`, escrito siempre a través de `AuditoriaService`: interno, cubre además operaciones administrativas sin usuario final directo (alta/edición/baja de jugador, disparo manual de recálculo) con `valoresAntes`/`valoresDespues`. No tiene endpoint público en el MVP (no está en `spec.md`); es un mecanismo de trazabilidad interna, consultable directamente en base o vía una futura vista de administración fuera de este alcance.

Toda operación que dispare un `Movimiento` de tipo `COMPRA_P2P`/`VENTA_P2P`/`COMPENSACION_BAJA`/`RECARGA_SALDO` también invoca `AuditoriaService` — se escriben ambos dentro de la misma transacción de negocio.

---

## 11. Riesgos técnicos y mitigaciones

| Riesgo | Mitigación en este diseño |
|---|---|
| Inconsistencia entre `TenenciaToken.cantidadReservada` y la suma de `cantidadDisponible` de las ofertas `ABIERTA` de ese usuario/jugador (p. ej. por un bug en algún flujo que no pase por `reservar`/`liberarReserva`/`ejecutarReserva`) | Esos tres métodos son el único punto de mutación de `cantidadReservada` (no hay setter público); test de invariante dedicado en `OfertaService`/`TenenciaToken`: para todo usuario+jugador, `cantidadReservada == Σ cantidadDisponible` de sus ofertas `ABIERTA`. |
| Cambio de estructura del HTML de WhoScored rompe el scraping | Aislado en `WhoScoredRepositoryImpl`; falla se loguea y no propaga (constitution §1). El resto del sistema sigue funcionando con el último rendimiento ingerido con éxito. |
| Motor de cotización con pesos mal calibrados produce valores poco realistas | Pesos en tabla `PesoMetrica`, ajustables sin deploy; `factorAjuste` acotado a ±20%/semana para evitar saltos extremos mientras se calibra. |
| Job semanal corriendo sobre un catálogo grande tarda demasiado y bloquea otras operaciones | Cada jugador se calcula en su propia sub-transacción corta (§5), no una transacción global; el job corre fuera de horario pico (03:00). |

---

## 12. Testing (alineado a constitution §4)

- `services/` (lógica de negocio) y entidades de `model/` con invariantes propias: JUnit 5 + Mockito, cobertura ≥80% (JaCoCo, build-breaking).
- Casos límite obligatorios explícitos por Service/entidad:
  - `Jugador`/`JugadorService`: emisión = 100 exacto, intento de exceder, baja con/sin tenencias asociadas, baja con/sin ofertas P2P abiertas.
  - `Usuario`/`UsuarioService`: débito con saldo exacto, débito con saldo insuficiente, recarga con monto ≤0.
  - `CotizacionService`: jugador sin rendimiento en la semana (§6.3), jugador sin cotización previa (primer cálculo), clamp de ±20%.
  - `OfertaService`: dos compradores concurrentes sobre la misma oferta limitada (test de integración con hilos/`CompletableFuture` contra Testcontainers, verificando que la suma ejecutada nunca supera `cantidadPublicada`).
- Integración: Spring Boot Test + Testcontainers (Postgres real) para todo lo que toque `repositories/`. Los repositories de WhoScored/Football-Data.org se testean contra mocks/fixtures propios, nunca contra la red real (constitution §4).
- Frontend: React Testing Library + Vitest, sobre comportamiento observable (formularios de compra/venta/oferta, portfolio, catálogo público).

---

## 13. Resumen de decisiones técnicas firmes (ADR-lite)

1. Arquitectura en capas técnicas (`controllers/services/repositories/model` a nivel raíz) — reemplaza la organización anterior por feature, alineada a la revisión de `constitution.md` §2/§3.
2. `metricas` de rendimiento como JSONB, no columnas fijas — sostiene "ingesta sin curar" de UC-05 sin migraciones constantes.
3. Motor de cotización con pesos en tabla de configuración, no hardcodeados — separa "qué se ingiere" de "qué pesa", ajustable sin deploy.
4. Ausencia de partido en la semana → puntaje neutro + decaimiento leve configurable, nunca `factorAjuste` exactamente 0 sostenido indefinidamente.
5. Ranking vía Strategy pattern, 2 estrategias iniciales (`cotizacion`, `variacion-semanal`), selección resuelta en `RankingService`, nunca en el Controller.
6. Concurrencia en oferta P2P vía locking pesimista sobre la fila de la oferta — no optimista, no cola.
7. `Movimiento` (historial de usuario) y `RegistroAuditoria` (auditoría interna) son dos tablas separadas con propósitos distintos, aunque una misma operación escriba en ambas.
8. Ingesta semanal (domingo), alineada y previa al job de cotización (lunes) — cadencia fija por `constitution.md` §1, no ajustable por decisión técnica (§8.2).
9. `CotizacionHistorica` es append-only; "vigente" es siempre el último registro por fecha, nunca una tabla mutable en paralelo.
10. Publicar una oferta P2P reserva físicamente la cantidad (`TenenciaToken.cantidadReservada`), no sólo la valida — el vendedor puede cancelar lo no vendido, y la baja de un jugador cancela automáticamente sus ofertas abiertas y reintegra la reserva antes de compensar (§2.5, §9).

---

## 14. Siguiente paso del flujo SDD

Este `plan.md` es el input directo del rol `manager`, que lo descompone en `tasks.md` (tareas concretas, ordenadas por dependencia, ejecutables una por una por el rol `programador`). Con la organización por capas, el orden natural ya no es "un módulo feature completo a la vez": es, primero, transversal por capa base (`common`/`config`), y luego por **entidad de negocio**, recorriendo dentro de cada una las 4 capas en el orden que manda el flujo de dependencia (`Model → Repository → Service → Controller`):

`common`/`config` → `Usuario` (model→repository→service→controller) → `Jugador` (ídem) → `Rendimiento`/ingesta (repositories externos → service → jobs) → `Cotizacion` (motor + job + endpoint manual) → `Token`/`Portfolio`/`Movimiento` → `Oferta` (P2P) → `Ranking` → `Auditoria` transversal desde el principio (usada por el resto de los Services).
