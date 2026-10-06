# Tareas — App de Valuación de Jugadores de Fútbol

> Descomposición de `plan.md` en tareas concretas, ordenadas por dependencia, del tamaño justo para que el rol `programador` las ejecute una por una. No reabre decisiones de producto ni técnicas — cada tarea referencia el punto exacto de `spec.md`/`plan.md` que implementa. No incluye código.

**Reescritura (alineación de arquitectura):** esta versión reemplaza una anterior organizada por módulo/feature. `constitution.md` y `plan.md` fueron actualizados a una arquitectura en capas técnicas (`controllers/`, `services/`, `repositories/`, `model/`, `dto/` a nivel raíz — constitution §2/§3, plan §1). El orden ya no agrupa "un módulo feature completo a la vez": agrupa primero la base transversal y luego, por cada **entidad de negocio**, las 4 capas en el orden del flujo de dependencia (`Model → Repository → Service → Controller`, plan §14).

**Definición de hecho (aplica a toda tarea que toque `services/` o `model/`), salvo que se indique lo contrario:**
Tests unitarios (JUnit 5 + Mockito) cubriendo los casos límite listados en `plan.md` §12 cuando corresponda; cobertura ≥80% en `services/` y en la lógica propia de `model/` (JaCoCo); sin violar ninguna regla de `constitution.md` (revisar en particular las prohibiciones de la sección 2 — especialmente el sentido único del flujo `Controller → Service → Repository → Model` — antes de dar la tarea por terminada).

---

## Fase 0 — Base transversal

Sin esto no puede arrancar ninguna entidad de negocio.

| ID | Tarea | Depende de | Referencia |
|---|---|---|---|
| T0.1 | Inicializar proyecto Spring Boot + Maven Wrapper (`mvnw`); crear la estructura de paquetes por capa vacía: `controllers/`, `services/`, `repositories/`, `model/`, `dto/request/`, `dto/response/`, `common/`, `config/` | — | plan §1, constitution §2/§3 |
| T0.2 | Levantar Postgres vía Docker + docker-compose (dev y test); configurar Flyway con migración baseline vacía | T0.1 | constitution §1 |
| T0.3 | `common/`: configuración centralizada de `BigDecimal`/`RoundingMode`, jerarquía de excepciones base (`SaldoInsuficienteException`, `TenenciaInsuficienteException`, `EmisionMaximaSuperadaException`, etc.) | T0.1 | plan §1, constitution §2 |
| T0.4 | `config/`: `SecurityFilterChain` esqueleto (sin reglas de negocio todavía), CORS por whitelist, OpenAPI, manejador global de errores (`@RestControllerAdvice`) en `application/problem+json` | T0.1 | plan §3/§4, constitution §2/§4 |
| T0.5 | `model/RegistroAuditoria` (append-only) + `RegistroAuditoriaRepository` + `AuditoriaService`/`AuditoriaServiceImpl` (interfaz + implementación, inyección por constructor) | T0.2, T0.3 | plan §2.8, §10 |
| T0.6 | `model/Movimiento` (append-only) + `MovimientoRepository` — building block compartido, usado desde varios Services más adelante (recarga, compra/venta, P2P, compensación) | T0.2, T0.3 | plan §2.6 |
| T0.7 | Gates de build: JaCoCo (80% `services/`/`model/`), Checkstyle/Spotless, OWASP Dependency-Check; Testcontainers para tests de integración sobre `repositories/` (Postgres real, nunca H2) | T0.1 | constitution §4 |
| T0.8 | Scaffold frontend: Vite + React + TypeScript strict, ESLint + Prettier, estructura `features/shared/api/app`, capa `api/` centralizada vacía (sin `fetch` suelto en componentes) | — | constitution §1/§3 |

---

## Fase 1 — Usuario

| ID | Tarea | Depende de | Referencia |
|---|---|---|---|
| T1.1 | `model/Usuario` + métodos propios `debitarSaldo`/`acreditarSaldo` (sin setters públicos sobre saldo); tests (saldo exacto, saldo insuficiente, monto ≤0) | T0.3 | plan §2.1, spec UC-01 |
| T1.2 | `UsuarioRepository` (Spring Data JPA) | T1.1, T0.2 | plan §1 |
| T1.3 | `UsuarioService`/`UsuarioServiceImpl`: `registrar()` (UC-01, valida identificador único, saldo inicial 0), `recargarSaldo()` (UC-14, monto > 0, escribe `Movimiento` tipo `RECARGA_SALDO` vía `MovimientoRepository` y audita vía `AuditoriaService`) | T1.2, T0.6, T0.5 | plan §1, spec UC-01/UC-14 |
| T1.4 | Seguridad completa en `config/`: JWT stateless, filtro de validación, `UserDetailsService` sobre `UsuarioRepository`, roles `USER`/`ADMIN` como `GrantedAuthority`, BCrypt | T1.2, T0.4 | plan §4, spec UC-02 |
| T1.5 | `AuthController`: `POST /auth/registro` (UC-01), `POST /auth/login` (UC-02, emite JWT, error genérico sin indicar campo) | T1.3, T1.4 | plan §3, spec UC-01/UC-02 |
| T1.6 | `UsuarioController`: `POST /usuarios/{id}/saldo` (UC-14), restringido a ADMIN (`@PreAuthorize`) | T1.3, T1.5 | plan §3, spec UC-14 |

---

## Fase 2 — Jugador (alta, edición, catálogo — la baja se completa en Fase 6)

| ID | Tarea | Depende de | Referencia |
|---|---|---|---|
| T2.1 | `model/Jugador` (estado `ACTIVO`/`INACTIVO`, `tokensEmitidos`) + métodos propios `emitirTokens`/`liberarTokens`/`darDeBaja`; tests (emisión = 100 exacto, intento de exceder, baja) | T0.3 | plan §2.2, spec regla 1 (§5) |
| T2.2 | `model/TenenciaToken` (`cantidad`, `cantidadReservada`, `disponible()`, `acreditar`/`debitar`/`reservar`/`liberarReserva`/`ejecutarReserva`); tests de la invariante `cantidadReservada ≤ cantidad` | T2.1 | plan §2.5 |
| T2.3 | `JugadorRepository`, `TenenciaTokenRepository` (Spring Data JPA) | T2.1, T2.2, T0.2 | plan §1 |
| T2.4 | `JugadorService`/`JugadorServiceImpl`: `darAlta()` (UC-03, hasta 100 tokens habilitados), `editar()` (UC-04, nunca toca cotización ni límite) | T2.3 | plan §1, spec UC-03/UC-04 |
| T2.5 | `JugadorController`: `POST /jugadores` (UC-03, ADMIN, audita vía `AuditoriaService`), `PUT /jugadores/{id}` (UC-04, ADMIN), `GET /jugadores` y `GET /jugadores/{id}` (UC-07, público, con tokens disponibles y fecha de última actualización de rendimiento) | T2.4, T1.4, T0.5 | plan §3, spec UC-03/UC-04/UC-07 |

---

## Fase 3 — Rendimiento (ingesta, UC-05)

| ID | Tarea | Depende de | Referencia |
|---|---|---|---|
| T3.1 | `model/RendimientoPartido` (`metricas` como JSONB, sin curar) + `RendimientoPartidoRepository` | T2.1, T0.2 | plan §2.3, §8.1 |
| T3.2 | Interfaces `RendimientoExternoRepository`/`FixtureRepository` en `repositories/` | T3.1 | plan §8.1 |
| T3.3 | `WhoScoredRepositoryImpl implements RendimientoExternoRepository`: scraping, traducción a modelo interno, aislamiento de errores (no propaga fallas a otros flujos); tests contra fixtures propios, nunca red real | T3.2 | plan §8.1, constitution §1/§4 |
| T3.4 | `FootballDataRepositoryImpl implements FixtureRepository`: consumo de API oficial, traducción a modelo interno, aislamiento de errores; tests contra fixtures propios, nunca red real | T3.2 | plan §8.1, constitution §1/§4 |
| T3.5 | `RendimientoService`/`RendimientoServiceImpl`: `ingestarFixtures()`/`ingestarRendimiento()`, orquesta las dos interfaces y persiste `RendimientoPartido` asociado a jugador y semana ISO | T3.3, T3.4, T3.1 | plan §1, §8.1 |
| T3.6 | Scheduling en `config/` (domingo 01:00 fixtures, domingo 02:00 rendimiento) invocando `RendimientoServiceImpl` directamente — sin Controller de por medio, el trigger no es HTTP | T3.5 | plan §8.2 |

---

## Fase 4 — Cotización (motor + job)

| ID | Tarea | Depende de | Referencia |
|---|---|---|---|
| T4.1 | `model/CotizacionHistorica` (append-only) + `CotizacionHistoricaRepository`; "vigente" = último registro por `jugadorId`; tests (nunca `UPDATE`, historial cronológico correcto) | T2.1, T0.2 | plan §2.4 |
| T4.2 | `model/PesoMetrica` (clave/peso/activo) + `PesoMetricaRepository` | T0.2 | plan §2.9, §6.2 |
| T4.3 | `CotizacionService`/`CotizacionServiceImpl`: cálculo por jugador (puntaje ponderado sobre `metricas`, `factorAjuste` clamp ±20%, `BASE_INICIAL` configurable); tests unitarios (clamp, primer cálculo sin historial previo) | T4.1, T4.2, T3.1 | plan §6.1 |
| T4.4 | Tratamiento de jugador activo sin rendimiento en la semana: puntaje neutro + decaimiento leve configurable; tests | T4.3 | plan §6.3, spec UC-06 |
| T4.5 | Scheduling en `config/` (lunes 03:00) invocando `CotizacionServiceImpl.recalcular(AUTOMATICO, null)` directamente, sólo sobre jugadores `ACTIVO`; cada jugador en su propia sub-transacción | T4.4, T3.6 | plan §5, §6.4, spec UC-06 |
| T4.6 | `CotizacionController`: `POST /cotizaciones/recalculo` (UC-13, ADMIN), invoca el mismo `CotizacionService` sobre todos los jugadores `ACTIVO`, sin parámetro de jugador ni de valor de cotización en el body; audita vía `AuditoriaService` por jugador afectado | T4.5, T1.4, T0.5 | plan §3/§6.4, spec UC-13 |
| T4.7 | `JugadorController`: `GET /jugadores/{id}/cotizaciones` (UC-08, público), delegando en `CotizacionService`; incluye jugadores dados de baja con su cotización congelada | T4.1, T2.5 | plan §3, spec UC-08 |

---

## Fase 5 — Token / Portfolio / Movimiento

| ID | Tarea | Depende de | Referencia |
|---|---|---|---|
| T5.1 | `TokenService`/`TokenServiceImpl`: `comprar()` (UC-09, valida saldo y tokens disponibles, débito saldo, `emitirTokens`, acredita tenencia, inserta `Movimiento`), `vender()` (UC-10, valida tenencia **disponible** vía `TenenciaToken.disponible()`, `liberarTokens`, acredita saldo, inserta `Movimiento`); tests de casos límite (saldo insuficiente, excede 100, vender tokens reservados en una oferta, sin tope de concentración) | T4.1, T2.3, T1.2, T0.6 | plan §5, spec UC-09/UC-10 |
| T5.2 | `TokenController`: `POST /jugadores/{id}/compras` (UC-09), `POST /jugadores/{id}/ventas` (UC-10) | T5.1, T1.4 | plan §3, spec UC-09/UC-10 |
| T5.3 | `PortfolioService`/`PortfolioServiceImpl`: vista de lectura (UC-11) combinando `TenenciaTokenRepository` y `CotizacionHistoricaRepository` — sin entidad `Portfolio` propia | T2.3, T4.1, T1.2 | plan §2.5, §1, spec UC-11 |
| T5.4 | `PortfolioController`: `GET /portfolios/me` (tenencias + valor + saldo) | T5.3, T1.4 | plan §3, spec UC-11 |
| T5.5 | `MovimientoService`/`MovimientoServiceImpl`: historial propio cronológico (UC-12), incluye compensaciones y operaciones P2P identificadas como tales | T0.6 | plan §1, spec UC-12 |
| T5.6 | `MovimientoController`: `GET /movimientos/me` | T5.5, T1.4 | plan §3, spec UC-12 |

---

## Fase 6 — Oferta (P2P) y baja de jugador (UC-15/UC-17)

| ID | Tarea | Depende de | Referencia |
|---|---|---|---|
| T6.1 | `model/OfertaP2P` (`cantidadPublicada`, `cantidadDisponible`, `estado`) + `OfertaP2PRepository` con `@Lock(LockModeType.PESSIMISTIC_WRITE)`; tests del método propio `ejecutarContra` | T2.2, T0.2 | plan §2.7, §9.3 |
| T6.2 | `OfertaService`/`OfertaServiceImpl.publicar()`: `TenenciaToken.reservar` del vendedor, rechazo si excede `disponible()`; crea oferta `ABIERTA` | T6.1, T2.3, T1.2 | plan §5, §9.1, spec UC-17 |
| T6.3 | `OfertaController`: `POST /ofertas` (publicar), `GET /ofertas?jugadorId={id}` (listado de ofertas abiertas) | T6.2, T1.4 | plan §3, spec UC-17 |
| T6.4 | `OfertaService.ejecutar()`: locking pesimista sobre la fila de la oferta, transacción de 4 movimientos (`ejecutarReserva` vendedor + saldo + tenencia comprador), 2 `Movimiento`, auditoría; `OfertaController`: `POST /ofertas/{id}/compras`; test de integración con compradores concurrentes verificando que nunca se supera `cantidadPublicada` | T6.3, T0.6, T0.5 | plan §5, §9.2, §9.3, spec UC-17 |
| T6.5 | `OfertaService.cancelar()`: sólo el vendedor dueño, sólo si `ABIERTA`; `liberarReserva` de lo no vendido, sin `Movimiento`; `OfertaController`: `DELETE /ofertas/{id}` | T6.4 | plan §9.4, spec UC-17 |
| T6.6 | `JugadorService.darDeBaja()` completo (UC-15): dentro de una única transacción — cancela ofertas `ABIERTA` del jugador y reintegra reserva, luego compensa cada `TenenciaToken` a la cotización del día anterior, marca jugador `INACTIVO`, audita; `JugadorController`: `POST /jugadores/{id}/baja` (ADMIN); tests (con y sin ofertas abiertas, con y sin tenencias) | T6.5, T4.1, T0.6, T0.5 | plan §5, §9.5, spec UC-15 |

---

## Fase 7 — Ranking (UC-16)

| ID | Tarea | Depende de | Referencia |
|---|---|---|---|
| T7.1 | Interfaz `EstrategiaRanking` en `services/` + bean `cotizacion` (default, orden por cotización vigente descendente) | T4.1, T2.3 | plan §7, spec UC-16 |
| T7.2 | Bean `variacion-semanal` (orden por variación % entre las dos últimas cotizaciones) | T7.1 | plan §7, spec UC-16 |
| T7.3 | `RankingService`/`RankingServiceImpl`: selecciona el bean por `clave` (fallback a `cotizacion`) — la selección es lógica de negocio, nunca vive en el Controller | T7.2 | plan §7, spec UC-16 |
| T7.4 | `JugadorController`: `GET /jugadores/ranking?estrategia={clave}` (UC-16, público) | T7.3, T2.5 | plan §3, spec UC-16 |

---

## Fase 8 — Frontend (consume la API ya construida en fases 1-7)

| ID | Tarea | Depende de | Referencia |
|---|---|---|---|
| T8.1 | Pantallas de registro e inicio de sesión (UC-01/UC-02), manejo de JWT en cliente | T1.5, T0.8 | spec UC-01/UC-02 |
| T8.2 | Catálogo público + detalle de jugador + histórico de cotización (UC-07/UC-08), accesible sin cuenta | T2.5, T4.7 | spec UC-07/UC-08 |
| T8.3 | Ranking de jugadores (UC-16), selector de estrategia | T7.4 | spec UC-16 |
| T8.4 | Portfolio del usuario + historial de movimientos (UC-11/UC-12) | T5.4, T5.6 | spec UC-11/UC-12 |
| T8.5 | Formularios de compra/venta contra el sistema (UC-09/UC-10) | T5.2 | spec UC-09/UC-10 |
| T8.6 | Flujo P2P: publicar oferta, listar ofertas de un jugador, comprar de una oferta, cancelar oferta propia (UC-17) | T6.4, T6.5 | spec UC-17 |
| T8.7 | Panel administrativo: alta/edición/baja de jugador, recarga de saldo, disparo manual de recálculo (UC-03/UC-04/UC-13/UC-14/UC-15) | T2.5, T4.6, T1.6, T6.6 | spec UC-03/UC-04/UC-13/UC-14/UC-15 |

---

## Fase 9 — Cierre e integración

| ID | Tarea | Depende de | Referencia |
|---|---|---|---|
| T9.1 | Suite de tests de integración end-to-end de los flujos críticos (compra → venta, publicar oferta → comprar, baja de jugador con oferta abierta) contra Postgres real vía Testcontainers | T6.6, T8.6 | plan §12 |
| T9.2 | Verificar en CI: JaCoCo ≥80% en `services/`/`model/`, Checkstyle/Spotless, ESLint/Prettier, OWASP Dependency-Check + `npm audit` sin vulnerabilidades altas/críticas sin mitigar | T9.1 | constitution §4 |
| T9.3 | Revisión final de `RegistroAuditoria`: confirmar que toda operación sensible (UC-13, UC-14, UC-15, UC-17, alta/edición de jugador) deja rastro completo (quién/qué/cuándo/antes-después) | T9.1 | constitution §4, plan §10 |

---

## Resumen de orden de ejecución

Fase 0 (base transversal) → Fase 1 (`Usuario`) → Fase 2 (`Jugador`, alta/edición/catálogo) → Fase 3 (`Rendimiento`, ingesta) → Fase 4 (`Cotizacion`, motor + job) → Fase 5 (`Token`/`Portfolio`/`Movimiento`) → Fase 6 (`Oferta` P2P + baja completa de `Jugador`) → Fase 7 (`Ranking`) → Fase 8 (frontend, en paralelo a partir de que cada endpoint que consume esté listo) → Fase 9 (cierre).

Dentro de cada fase de entidad, las tareas recorren las capas en el orden `Model → Repository → Service → Controller` (constitution §2, plan §14): cada tarea depende, como mínimo, de la anterior de su misma fase salvo que la columna "Depende de" indique lo contrario.
