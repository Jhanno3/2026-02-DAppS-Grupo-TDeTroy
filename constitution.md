# Constitución del Proyecto

**Proyecto:** App de valuación de jugadores de fútbol profesionales (tokenización de jugadores, cotización, portfolio por usuario).

Este documento define las reglas del juego innegociables para todo el proyecto. Ningún rol (planificador, implementador, revisor) puede contradecirlas sin que esta constitución se actualice primero explícitamente. No es una especificación funcional: no define features ni tareas, sólo el marco dentro del cual esas features se construyen.

---

## 1. Stack tecnológico permitido

### Backend
- **Lenguaje:** Java (versión LTS vigente, 21).
- **Framework:** Spring Boot (Spring Web, Spring Data JPA, Spring Security, Spring Validation).
- **Build tool:** Maven (con Maven Wrapper `mvnw` versionado en el repo). Gradle está **prohibido** para evitar duplicidad de herramientas de build en el equipo.
- **Persistencia:** Spring Data JPA / Hibernate sobre PostgreSQL. Consultas complejas vía JPQL o `@Query` parametrizado; Criteria API permitida para filtros dinámicos.
- **Migraciones de esquema:** Flyway, obligatorio. Está **prohibido** modificar el esquema con `ddl-auto=update/create` en cualquier ambiente que no sea el `local` de desarrollo individual.
- **Mapeo objeto-DTO:** MapStruct o mapeo manual explícito. Está **prohibido** exponer entidades JPA directamente como respuesta de API.
- **Boilerplate:** Lombok permitido para getters/setters/constructores. **Prohibido** `@Data` sobre entidades JPA (genera `equals`/`hashCode`/`toString` inseguros con proxies Hibernate y colecciones lazy).

### Frontend
- **Librería:** React (con Vite como bundler; Create React App está **prohibido** por estar deprecado).
- **Lenguaje:** TypeScript obligatorio, modo `strict` activado. JavaScript plano (`.js`/`.jsx`) **prohibido** en código nuevo.
- **Gestor de paquetes:** npm, con `package-lock.json` versionado.
- **Cliente HTTP / estado de servidor:** una librería de fetching con cache (ej. TanStack Query) para toda comunicación con el backend. **Prohibido** hacer `fetch`/`axios` sueltos dentro de componentes; todo acceso a la API pasa por una capa `api/` centralizada.
- **Estado local/UI:** `useState`/`useReducer`/Context de React. Librerías de estado global (Redux, MobX, Zustand, etc.) están **prohibidas** salvo que se justifique por escrito (ADR) que el estado de UI compartido superó lo que Context puede manejar razonablemente — evitar sobre-ingeniería en el MVP.
- **Estilos:** un único enfoque consistente en todo el proyecto (CSS Modules o equivalente con scope por componente). **Prohibido** mezclar CSS global sin scope, estilos inline como mecanismo principal, y más de una librería de estilos conviviendo en el proyecto.
- **Comunicación en tiempo real:** **prohibido** incorporar WebSocket, Server-Sent Events (SSE), long-polling, o cualquier otra infraestructura de push en tiempo real. No hay ningún flujo del dominio (ver cotización, sección 2) que la requiera; el frontend consume siempre datos ya calculados vía API REST estándar.

### Base de datos
- **Motor:** PostgreSQL exclusivamente (todos los ambientes, incluido local vía contenedor). **Prohibido** usar H2 u otro motor in-memory para tests de integración o para desarrollo local — el motor debe ser idéntico al de producción, particularmente por el manejo de precisión numérica en valores monetarios.

### Modelo de tokens (decisión de producto confirmada)
- Los tokens de jugador son un **modelo puramente interno**, representado en tablas de PostgreSQL. **Queda descartada de forma definitiva** cualquier integración con blockchain, smart contracts, wallets o Web3. **Prohibido** introducir librerías, nodos, gestión de claves criptográficas o cualquier otra dependencia asociada a blockchain para este propósito.
- El saldo con el que operan los usuarios es **saldo virtual interno a la aplicación**, no dinero real. Ver sección 4 (Seguridad) para las restricciones que se derivan de esto.

### Integraciones de datos externos (decisión de producto confirmada, fijas y no negociables)
- **WhoScored:** se utiliza **scraping** para extraer datos detallados de rendimiento de jugadores y equipos (pases, tiros, intercepciones, calificaciones, entre otros). Es una fuente **no oficial** (scraping de una web pública) y, por lo tanto, **intrínsecamente frágil** ante cambios de estructura del sitio.
- **Football-Data.org:** se utiliza su **API oficial** para obtener resultados de partidos, alineaciones y fixtures.
- Principios de arquitectura que se derivan de hacer convivir una fuente por scraping y otra por API oficial (ver también sección 2):
  - Cada fuente externa se aísla detrás de su **propio adapter/puerto** (patrón puerto-adaptador, aplicado puntualmente a estas integraciones). El dominio y los casos de uso de la aplicación nunca dependen del formato de respuesta de WhoScored ni de Football-Data.org; dependen únicamente de una interfaz propia definida por el dominio, que cada adapter implementa traduciendo el formato externo al modelo interno.
  - **Manejo explícito de fallos/indisponibilidad por fuente:** la falla, cambio de estructura o indisponibilidad de una fuente externa (típicamente esperable en WhoScored por ser scraping) no debe afectar la disponibilidad de funcionalidades que dependen de la otra fuente ni del resto del sistema. Cada integración aísla y maneja sus propios errores.
  - El scraping de WhoScored **no** se ejecuta en el flujo síncrono de un request de usuario. Es un **proceso de ingesta propio y desacoplado** (batch/job) que obtiene y persiste los datos para que el resto del sistema los consuma ya almacenados — nunca on-demand disparado por una request HTTP de un usuario esperando respuesta.
- Esta constitución fija el principio de arquitectura, no el detalle de implementación: no define aquí el algoritmo de scraping, librerías específicas, ni política de reintentos para ninguna de las dos integraciones.

### Herramientas transversales
- **Contenerización:** Docker + docker-compose para levantar Postgres (y toda dependencia externa) en desarrollo y en tests de integración (vía Testcontainers).
- **Linters/formatters obligatorios:** Checkstyle o Spotless en backend; ESLint + Prettier en frontend. El build **falla** si hay violaciones.

---

## 2. Principios de arquitectura

### Patrones a seguir
- **Monolito modular** organizado por *feature* (jugador, token, cotización, portfolio, usuario), no por capa técnica global. Es la arquitectura mandatada para el MVP dado que no hay restricciones de infraestructura que justifiquen microservicios.
- Dentro de cada feature, separación en capas: **Controller (API) → Service (lógica de negocio) → Repository (persistencia) → Domain model**. El Controller sólo orquesta: recibe request, valida forma, delega al Service, mapea respuesta.
- **Inyección de dependencias por constructor** en todo el backend. Inyección por campo (`@Autowired` en atributo) está **prohibida**: dificulta testear y oculta dependencias.
- **DTOs inmutables** (Java `record`) en los bordes de la API, tanto de entrada como de salida. Las entidades de persistencia nunca cruzan ese borde.
- Un único punto de verdad para cada regla de negocio crítica (en particular, la lógica de cálculo/actualización de cotización vive en un único servicio de dominio; ningún otro componente recalcula o duplica esa lógica).
- Las entidades de dominio con invariantes financieras (saldo de portfolio, tenencia de tokens) exponen **métodos de dominio** que validan la operación (ej. `acreditarTokens(cantidad)`, `debitarTokens(cantidad)`), no setters públicos que permitan mutación arbitraria del estado.
- **Invariante de emisión de tokens (no negociable):** todo jugador tiene, al momento de ser creado, un **máximo de 100 tokens** disponibles para que los usuarios compren. Este límite es una invariante de dominio y debe estar protegida en el propio modelo de dominio (entidad/agregado `Jugador`/`Token`), no sólo validada en la capa de servicio o de API — el modelo nunca debe poder alcanzar un estado en el que la emisión total de tokens de un jugador supere 100, bajo ninguna operación (incluidas las administrativas).
- **Mecanismo de cotización (decisión de producto confirmada):** la cotización de cada jugador se determina mediante un **cálculo algorítmico basado en datos de rendimiento** del jugador luego de sus partidos (resultado/desempeño de cada partido afecta el valor de su token). El precio nunca surge de un mercado de oferta y demanda entre usuarios ni de negociación entre las partes (sin order book de precios distintos, sin subastas), ni de un ajuste manual de administrador como mecanismo de determinación de precio — ni siquiera las operaciones P2P entre usuarios descritas más abajo determinan o negocian precio. Ese cálculo se nutre de los datos de rendimiento ingeridos desde WhoScored y de los resultados/fixtures ingeridos desde Football-Data.org (ver "Integraciones de datos externos" en la sección 1), siempre a través de los adapters propios y nunca acoplado a su formato externo.
- **Operaciones peer-to-peer (P2P) entre usuarios (decisión de producto confirmada):** además de comprar tokens nuevos al sistema (mientras haya emisión disponible de los 100) y vendérselos de vuelta al sistema, un usuario vendedor puede **publicar/poner a la venta una cantidad determinada de tokens que posee** de un jugador, disponible para que **cualquier otro usuario interesado** con saldo suficiente la compre. Esta vía P2P es **adicional** a la compra/venta contra el sistema y **convive** con ella; no la reemplaza ni la limita. Es una **oferta abierta a cualquier usuario interesado**, no una transferencia dirigida de antemano a un destinatario específico elegido por el vendedor. Principios que rigen esta operación:
  - El precio de toda operación P2P es siempre y exclusivamente la **cotización oficial vigente** del jugador en el momento de la ejecución — nunca un precio distinto, negociado, pactado u ofertado libremente entre las partes. No hay negociación de precio en ningún caso, ni entre usuarios ni contra el sistema. El vendedor publica una **cantidad** de tokens a vender, nunca un precio: el precio lo determina siempre y únicamente el servicio de cotización.
  - El precio se obtiene del **mismo y único servicio de cotización vigente** que ya usan las operaciones contra el sistema (ver "Mecanismo de cotización" arriba y el "único punto de verdad" de cotización más arriba en esta sección); no se crea una segunda fuente de precio ni una fórmula distinta para P2P.
  - La operación se **ejecuta en el momento en que el comprador decide ejecutarla** (siempre que tenga saldo suficiente a la cotización vigente), sin paso de confirmación adicional por parte del vendedor ni del comprador más allá de esa acción — no hay intercambio de propuestas ni aceptación bilateral: es una compra directa contra una oferta ya publicada.
  - Dado que la cantidad publicada por un vendedor es limitada y **más de un comprador interesado puede intentar comprarla al mismo tiempo**, el sistema **debe garantizar consistencia bajo concurrencia sobre esa cantidad publicada**: nunca puede venderse más cantidad de la que el vendedor puso a disposición ("sobre-venta"). Se queda con la operación el primer comprador cuya ejecución se confirme contra la cantidad disponible en ese momento; los interesados restantes son rechazados o ajustados según la cantidad remanente. Esta constitución fija el principio — consistencia/atomicidad bajo concurrencia sobre la cantidad de una oferta —, no el mecanismo técnico concreto (lock optimista, pesimista, cola u otro), que queda para las decisiones de diseño técnico/implementación.
  - Esta resolución de concurrencia es exclusivamente sobre **cantidad disponible**, nunca sobre precio: en ningún caso puede dar lugar a que distintos compradores paguen valores distintos por la misma oferta, ni a ningún tipo de puja, prioridad pagada, o ventaja de precio entre interesados.
  - Es una **operación atómica de cuatro movimientos** — débito de tokens y crédito de saldo en el usuario vendedor; débito de saldo y crédito de tokens en el usuario comprador — con la misma exigencia de atomicidad/transaccionalidad (`@Transactional`, reversión total ante fallo parcial) que el resto de las operaciones que afectan saldo y tenencia descritas en esta sección.
  - Queda registrada en el log de auditoría inmutable (sección 4) y reflejada en el historial de movimientos de **ambos** usuarios involucrados.
- **Frecuencia de recotización (decisión de producto confirmada):** la cotización de cada jugador se recalcula y persiste **por defecto** mediante un **job/proceso batch centralizado con frecuencia fija semanal (cada 7 días)** — no en tiempo real. Ese job ejecuta el único algoritmo determinístico de cálculo de cotización (el mismo mencionado en el punto anterior) y es el disparador por defecto del sistema.
- **Disparo manual del recálculo de cotización (decisión de producto confirmada):** además del job semanal automático, existe una vía para disparar manualmente ese mismo recálculo, pensada para operación y testing (ej. forzar una recotización puntual para validar un fix o resolver una incidencia). Este disparo manual:
  - Ejecuta **exactamente el mismo** servicio/algoritmo determinístico que el job semanal — no existe una segunda implementación ni una variante del cálculo para uso manual; el único punto de verdad de la lógica de cotización mencionado más arriba sigue siendo uno solo.
  - **Nunca** permite que un humano fije, escriba o sobrescriba directamente un valor de cotización. No es un mecanismo de override de precio: es exclusivamente un disparador que invoca el mismo cálculo fuera de su horario programado.
  - Está restringido al rol `ADMIN` (o un rol operativo equivalente que se defina en el futuro); ningún otro rol puede invocarlo.
  - Queda registrado en el log de auditoría inmutable igual que cualquier otra operación sensible (ver sección 4): quién lo disparó, cuándo, y el resultado (valores antes/después por jugador afectado).
  - El frontend y cualquier otro consumidor externo sólo **leen** el último valor ya calculado a través de la API REST normal, sin importar si ese valor se originó en el job semanal automático o en un disparo manual; no existe (ni se necesita) push en tiempo real de cotización hacia el cliente.
- Operaciones que afectan múltiples entidades relacionadas con dinero/tokens (ej. compra de un token contra el sistema: descuenta saldo, acredita tenencia, registra movimiento; o la operación P2P de cuatro movimientos descrita arriba) son **atómicas**, envueltas en `@Transactional`, y de existir un fallo parcial el estado revierte por completo. No se admiten actualizaciones "en varios pasos" sin transacción que puedan dejar el sistema en estado inconsistente.
- El frontend es una capa de presentación pura respecto de las reglas de negocio: consume la API, muestra datos, valida forma de inputs (UX), pero **nunca** decide ni calcula valores de cotización, saldos o reglas de negocio — esas decisiones siempre las toma el backend y el frontend las refleja.

### Patrones prohibidos
- **Prohibido** ubicar lógica de negocio en Controllers.
- **Prohibido** el patrón "God Service"/"God Class": un servicio o clase que concentra lógica de múltiples features no relacionadas. Cada servicio tiene una responsabilidad acotada a su feature.
- **Prohibido** el paquete-por-capa a nivel raíz del proyecto (`controllers/`, `services/`, `repositories/` como paquetes top-level que mezclan todas las features). La organización es por feature, no por capa técnica.
- **Prohibido** usar `float`/`double` para representar dinero, cotizaciones, o cualquier valor monetario/valuación. Obligatorio `BigDecimal`, con escala y `RoundingMode` definidos de forma centralizada y consistente en todo el sistema.
- **Prohibido** construir queries SQL por concatenación de strings. Sólo JPQL, `@Query` con parámetros nombrados/posicionales, o Criteria API.
- **Prohibido** que el frontend acceda directamente a la base de datos o a cualquier recurso que no sea la API REST propia del backend.
- **Prohibido** introducir microservicios, colas de mensajería, o cualquier componente de infraestructura adicional sin un ADR (decisión de arquitectura documentada) que justifique por qué el monolito modular dejó de ser suficiente.
- **Prohibido** el acoplamiento circular entre paquetes de features distintas (ej. que `token` dependa de `portfolio` y `portfolio` dependa de `token` al mismo tiempo). Las dependencias entre features son unidireccionales; si dos features necesitan compartir algo, se extrae a un paquete `common`/`shared`.
- **Prohibido** que la emisión total de tokens de un jugador supere las 100 unidades, en cualquier circunstancia u operación.
- **Prohibido** cualquier mecanismo de **descubrimiento de precio entre usuarios**: order book con precios distintos entre órdenes, subastas, negociación de precio, o cualquier esquema de oferta/demanda que mueva o determine el valor del token. Un usuario nunca puede comprar ni vender tokens —ni a otro usuario ni al sistema— a un precio distinto de la cotización oficial vigente. Esto **no** prohíbe la operación P2P descrita en "Patrones a seguir": esa oferta P2P, abierta a cualquier usuario interesado (incluso cuando compite por ella más de un comprador), está permitida, pero siempre y únicamente a la cotización oficial vigente, sin negociación de precio.
- **Prohibido** implementar la operación P2P (o cualquier otra vía de intercambio de tokens entre usuarios) mediante cualquier mecanismo de **descubrimiento o negociación de precio**: order book con precios distintos entre ofertas, subastas, puja, prioridad de ejecución pagada, o cualquier esquema donde distintos compradores puedan pagar valores distintos por la misma oferta o donde el precio se determine por interacción entre usuarios. Esto sigue absolutamente prohibido. En cambio, **no** está prohibido que una oferta P2P esté abierta a múltiples usuarios interesados a la vez, ni que exista un mecanismo simple de resolución de concurrencia sobre la **cantidad** publicada (p. ej. "el primer comprador cuya ejecución se confirma se la lleva", con ajuste o rechazo para el resto según la cantidad remanente) — siempre que ese mecanismo nunca implique una variación de precio entre compradores ni una segunda fuente de precio distinta de la cotización oficial vigente.
- **Prohibido** fijar o editar manualmente la cotización de un jugador (por un `ADMIN` o cualquier otro rol) a modo de mecanismo principal de precio (ver la excepción explícita y acotada de la sección 2 para el disparo manual del mismo algoritmo, que no constituye edición manual de precio); el precio surge siempre del cálculo algorítmico de rendimiento, tanto para operaciones contra el sistema como para operaciones P2P.
- **Prohibido** recalcular o persistir un nuevo valor de cotización mediante cualquier vía que no sea el único servicio/algoritmo determinístico de cotización, ya sea invocado por el job semanal automático o por el disparo manual restringido a `ADMIN` descrito en la sección 2 — evita múltiples fuentes de verdad o implementaciones divergentes sobre el precio de un jugador.
- **Prohibido**, en particular, que el endpoint o mecanismo de disparo manual acepte, reciba o permita como parámetro un valor de cotización: sólo puede iniciar el cálculo, nunca fijarlo.
- **Prohibido** agregar WebSocket, SSE, long-polling, o cualquier otra infraestructura de comunicación en tiempo real para propagar cotización (o cualquier otro dato) al frontend.
- **Prohibido** que el dominio o los casos de uso (capas `domain`/`application`) dependan directamente del formato de respuesta de WhoScored o de Football-Data.org; todo acceso a esas fuentes pasa por un adapter/puerto propio que traduce al modelo interno.
- **Prohibido** que un fallo, cambio de estructura o indisponibilidad de WhoScored (scraping) o de Football-Data.org (API) se propague y tumbe flujos del sistema no relacionados con esa fuente en particular.
- **Prohibido** ejecutar el scraping de WhoScored de forma síncrona dentro del ciclo de request/response de un usuario; debe resolverse siempre mediante un proceso de ingesta desacoplado.

---

## 3. Convenciones de código y estructura de carpetas

### Backend — estructura de paquetes (package-by-feature)
```
src/main/java/<groupId>/valuacion/
  jugador/
    api/            (controllers, request/response DTOs)
    domain/         (entidades, value objects, interfaces de repositorio)
    application/    (services, casos de uso)
    infrastructure/ (implementaciones JPA, mappers)
  token/
    api/ domain/ application/ infrastructure/
  cotizacion/
    api/ domain/ application/ infrastructure/
  portfolio/
    api/ domain/ application/ infrastructure/
  usuario/
    api/ domain/ application/ infrastructure/
  common/           (excepciones base, utilidades, configuración de BigDecimal/RoundingMode)
  config/           (configuración Spring: seguridad, CORS, OpenAPI, etc.)
```

### Frontend — estructura de carpetas (feature-based)
```
src/
  features/
    jugadores/      (componentes, hooks, tipos específicos de la feature)
    tokens/
    cotizacion/
    portfolio/
    auth/
  shared/
    components/     (componentes de UI reutilizables, "dumb")
    hooks/
    utils/
  api/               (cliente HTTP centralizado, un módulo por recurso de la API)
  app/               (routing, providers, configuración global)
```

### Convenciones de nombres
- **Java:** clases en `PascalCase`, métodos/variables en `camelCase`, constantes en `UPPER_SNAKE_CASE`, paquetes en minúsculas sin guiones bajos. Endpoints REST: sustantivos en plural, versionados (`/api/v1/jugadores`, `/api/v1/portfolios/{id}`).
- **Errores de API:** formato consistente en toda la aplicación (tipo `application/problem+json`, RFC 7807). Prohibido devolver stack traces o mensajes de excepción interna crudos al cliente.
- **TypeScript/React:** componentes en `PascalCase`, hooks propios prefijados `useX` en `camelCase`, archivos de componente `NombreComponente.tsx`. Prohibido el tipo `any`; si un tipo es genuinamente desconocido, usar `unknown` y angostarlo explícitamente.
- **Commits/branches:** fuera del alcance de esta constitución salvo lo indicado en la sección de dependencias/testing más abajo; se puede definir en un documento de convenciones de equipo aparte.

---

## 4. Restricciones no negociables

### Seguridad
- Autenticación obligatoria (Spring Security) en todo endpoint que exponga o modifique datos de un usuario (portfolio, tenencias, operaciones de compra/venta). **Prohibido** cualquier endpoint de escritura sin autenticación.
- Autorización basada en roles como mínimo (`USER`, `ADMIN`). Las operaciones administrativas (alta/edición de jugadores, y el disparo manual del recálculo de cotización descrito en la sección 2) están restringidas a `ADMIN`.
- Contraseñas: hash con BCrypt (o Argon2). **Prohibido** almacenar contraseñas en texto plano o con cifrado reversible.
- **Prohibido** commitear secretos (credenciales de DB, claves JWT, API keys — incluida la credencial/API key de Football-Data.org) al repositorio. Configuración sensible vía variables de entorno; `application.yml` sólo contiene placeholders/defaults no sensibles para desarrollo local.
- Validación de todo input de la API mediante Bean Validation (`@Valid` + anotaciones `jakarta.validation`) en el borde del Controller. **Prohibido** confiar en validación hecha únicamente en el frontend.
- HTTPS obligatorio en todo ambiente que no sea `localhost`.
- CORS configurado explícitamente por whitelist de orígenes. **Prohibido** `*` como origen permitido en cualquier ambiente distinto de desarrollo local.
- Toda operación que modifique saldo, tenencia de tokens o cotización debe quedar registrada en un log de auditoría inmutable (quién, qué, cuándo, valores antes/después). Esto incluye explícitamente el disparo manual del recálculo de cotización (sección 2) y toda operación P2P entre usuarios (sección 2), registrada de forma que quede reflejada en el historial de movimientos de ambos usuarios involucrados (vendedor y comprador). **Prohibido** que este tipo de operaciones sea "silenciosa" (sin rastro).
- **Dinero real: fuera de alcance permanente (decisión de producto confirmada).** El sistema opera exclusivamente con **saldo virtual** asignado dentro de la app; en ningún momento se procesa, mueve, custodia ni referencia dinero real. En consecuencia queda **terminantemente prohibido**: integrar pasarelas de pago externas, procesar datos de tarjetas u otros medios de pago reales, o implementar cualquier flujo de carga/retiro de dinero real. La anterior obligación de cumplimiento PCI-DSS / procesador de pagos externo queda **eliminada** de esta constitución — no aplica al proyecto y no debe reintroducirse sin que el dueño del producto reabra explícitamente este alcance.

### Testing
- Backend: JUnit 5 + Mockito para tests unitarios de `application`/`domain`. Tests de integración con Spring Boot Test + **Testcontainers** (PostgreSQL real, nunca H2) para todo lo que toque `infrastructure`/persistencia.
- Cobertura mínima obligatoria del **80%** en los paquetes `domain` y `application` de cada feature, verificada en build (JaCoCo). El build **falla** si no se alcanza.
- Toda lógica que involucre cálculo o mutación de cotización, saldo de portfolio, o tenencia de tokens **debe** tener tests unitarios cubriendo casos límite (cantidades cero, negativas, saldo insuficiente, intento de exceder el máximo de 100 tokens emitidos, concurrencia si aplica). **Prohibido** mergear cambios a esa lógica sin tests nuevos o actualizados. Esto incluye tanto la vía de disparo automático (job semanal) como la vía de disparo manual (ADMIN) descritas en la sección 2, dado que ambas invocan el mismo algoritmo.
- Frontend: React Testing Library + Jest (o Vitest) para componentes y hooks. **Prohibido** testear implementación interna (usar Enzyme o inspeccionar estado interno); los tests validan comportamiento observable por el usuario.
- **Prohibido** que un test unitario dependa de red real, servicios externos, o de un estado de base de datos compartido entre tests (cada test de integración levanta/limpia su propio contexto). En particular, los tests que ejercitan los adapters de WhoScored y Football-Data.org no deben depender de la disponibilidad real de esos sitios/APIs.

### Dependencias
- Toda dependencia nueva (Maven o npm) requiere justificación explícita en el PR que la introduce: qué problema resuelve y por qué no alcanza con lo ya presente en el stack.
- Versiones **fijas y explícitas** en `pom.xml`/`package.json`. **Prohibido** el uso de rangos dinámicos (`+`, `latest`, `^`/`~` sin lockfile aplicado) que permitan que el build resuelva a una versión distinta en cada ejecución.
- `package-lock.json` y `pom.xml` (con versiones fijadas) se versionan siempre en el repositorio.
- Escaneo de vulnerabilidades obligatorio como parte del pipeline: OWASP Dependency-Check (backend) y `npm audit` (frontend). Una vulnerabilidad de severidad alta/crítica sin mitigar **bloquea** el merge.
- **Prohibido** agregar librerías sin mantenimiento activo (sin releases en más de ~2 años, o explícitamente archivadas) salvo justificación explícita y aprobada.

---

## 5. Preguntas abiertas

Las 4 preguntas originales de esta sección (naturaleza de la tokenización, mecanismo de cotización, dinero real vs. saldo virtual, y frecuencia de actualización de cotización) fueron respondidas por el dueño del producto y quedaron incorporadas como reglas firmes en las secciones 1, 2 y 4 de este documento.

A partir de la incorporación de las fuentes de datos externas (WhoScored y Football-Data.org, sección 1) queda una pregunta abierta nueva:

1. **Frecuencia de ingesta automática de cada fuente externa:** esta constitución fija que el *recálculo de cotización* corre semanalmente (por defecto) y admite un disparo manual acotado (sección 2), pero no define con qué frecuencia corren los procesos de ingesta que alimentan de datos crudos a ese cálculo — es decir, cada cuánto se ejecuta el scraping de WhoScored y cada cuánto se sincroniza con la API de Football-Data.org (¿diario? ¿ligado al calendario de partidos? ¿bajo demanda antes de cada job semanal de cotización?). Esta definición queda pendiente de decisión del dueño del producto antes de planificar las features de ingesta.
</content>
