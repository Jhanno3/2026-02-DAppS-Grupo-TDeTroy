# Especificación Funcional — App de Valuación de Jugadores de Fútbol

> Este documento define **qué** hace el sistema y **por qué**, en base a la idea de producto recibida y respetando como marco no negociable la `constitution.md` ya aprobada por el dueño del producto. No define tecnología, esquemas de base de datos, endpoints ni ningún aspecto de **cómo** se implementa — eso corresponde al rol técnico.

---

## 1. Objetivo del producto

Permitir que usuarios registrados especulen, dentro de un entorno cerrado y sin dinero real, sobre el rendimiento deportivo de jugadores de fútbol profesionales, comprando y vendiendo "tokens" que representan una porción del valor de un jugador. El valor de esos tokens sube o baja según el rendimiento real del jugador en los partidos que disputa, calculado automáticamente por el sistema semana a semana. Cada usuario gestiona una cartera (portfolio) de tokens y un saldo virtual con el que opera.

Es un producto de entretenimiento/simulación (fantasy/juego de valuación), no un producto financiero real: no hay dinero real involucrado en ningún punto.

---

## 2. Alcance

### 2.1 Incluye (MVP)

- Registro e inicio de sesión de usuarios.
- Asignación de saldo virtual inicial a todo usuario nuevo (saldo cero, ver UC-01).
- Alta, edición y baja de jugadores por parte de un administrador.
- Recarga de saldo virtual de un usuario, ejecutada por un administrador (ver UC-14).
- Incorporación automática, por parte del sistema, del rendimiento de un jugador en cada partido que disputa, a partir de fuentes externas de datos deportivos (insumo para el cálculo de cotización).
- Cálculo automático y periódico (semanal) de la cotización de cada jugador en base a su rendimiento.
- Disparo manual, restringido a administradores, de ese mismo cálculo de cotización fuera del ciclo semanal, para necesidades operativas o de resolución de incidencias (nunca para fijar un valor a mano).
- Consulta pública del catálogo de jugadores, su cotización actual, su cotización histórica y un ranking de jugadores.
- Compra de tokens de un jugador por parte de un usuario, con un tope de 100 tokens emitidos por jugador y sin tope adicional de concentración por usuario.
- Venta de tokens de un jugador por parte de un usuario, al sistema, a la cotización vigente.
- Publicación, por parte de un usuario vendedor, de una cantidad de tokens de un jugador que posee para la venta directa entre usuarios (operación peer-to-peer, P2P): cualquier otro usuario interesado con saldo suficiente puede comprarla, ejecutándose la operación automáticamente en el momento en que decide comprar, siempre a la cotización oficial vigente del jugador, como vía adicional que convive con la compra/venta contra el sistema, sin reemplazarla (ver UC-17).
- Consulta del portfolio de un usuario: tokens que posee, jugador al que corresponden, y valor actual de la tenencia.
- Consulta del historial de movimientos (compras/ventas, incluidas las operaciones P2P) de un usuario.

### 2.2 No incluye (explícitamente fuera de alcance de esta versión)

- Cualquier manejo de dinero real: no hay pagos, cobros, retiros, ni pasarelas de pago. Todo el saldo es virtual.
- Subastas y negociación de precio entre usuarios: aunque la vía P2P (UC-17) permite que un usuario compre tokens publicados para la venta por otro, el precio de esa operación nunca es pactado, ofertado ni negociado entre las partes — es siempre la cotización oficial vigente calculada algorítmicamente por el sistema. Queda fuera de alcance cualquier mecanismo de descubrimiento de precio entre usuarios (order book, subasta, puja, prioridad de ejecución pagada, o esquema de oferta/demanda que determine o varíe el precio); la única variable que un vendedor publica es una cantidad de tokens, nunca un precio.
- Actualización de cotización en tiempo real, o bajo demanda por parte de un usuario final: la cotización solo cambia mediante el recálculo periódico semanal o el disparo manual acotado a un administrador (ver UC-13); ningún usuario final puede pedir ni provocar un recálculo.
- Notificaciones (push, email, en tiempo real) de cambios de cotización o de la actividad del usuario.
- Funcionalidades sociales: rankings **entre usuarios**, comentarios, seguimiento de otros usuarios, compartir portfolio. (Esto es distinto del ranking de jugadores por rendimiento incorporado en UC-16, que no es una funcionalidad social entre usuarios sino una vista más sobre el catálogo de jugadores, disponible públicamente.)
- Recuperación/reseteo de contraseña y edición de datos de perfil más allá del alta de cuenta: el MVP incluye únicamente registro e inicio de sesión.
- Cualquier medio de fijar la cotización que no sea el cálculo algorítmico basado en rendimiento (sin ajuste manual de administrador, sin mercado de oferta/demanda, ni siquiera en la vía P2P).
- Multi-idioma, multi-moneda (el saldo virtual es una única unidad, sin conversión).

---

## 3. Actores

| Actor | Descripción |
|---|---|
| **Usuario (rol USER)** | Persona registrada en la app. Compra y vende tokens contra el sistema (UC-09, UC-10); además puede, como vendedor, publicar una cantidad de tokens que posee de un jugador para la venta directa a cualquier otro usuario interesado, y puede, como comprador, adquirir tokens de una oferta publicada por otro usuario, siempre a la cotización oficial vigente (operación P2P, ver UC-17); consulta su portfolio, su saldo y su historial de movimientos; además puede consultar el catálogo de jugadores, cotizaciones y ranking (estas últimas tres también son accesibles sin cuenta, ver actor Visitante). |
| **Administrador / Superusuario (rol ADMIN)** | Responsable de mantener el catálogo de jugadores: alta, edición y baja (ver UC-03, UC-04, UC-15). Puede además disparar manualmente el recálculo de cotización fuera del ciclo semanal (ver UC-13) y acreditar saldo virtual en la cuenta de otro usuario (ver UC-14), pero nunca fija un valor de cotización a mano ni carga rendimiento partido a partido — esto último lo hace el sistema de forma automática (ver UC-05). **Nota de terminología (resuelta):** el dueño del producto confirmó que "Superusuario" y "Administrador" son el mismo rol (ADMIN); ambos nombres son válidos e intercambiables para referirse a él, no se trata de dos roles distintos. |
| **Visitante (no autenticado)** | Persona que aún no se registró. Puede consultar libremente el catálogo público de jugadores, su cotización actual e histórica, y el ranking de jugadores (ver UC-07, UC-08, UC-16), sin necesidad de crear una cuenta ni autenticarse. No puede comprar ni vender tokens (ni contra el sistema ni por vía P2P), ni acceder a un portfolio o historial de movimientos: esas funciones requieren cuenta y sesión iniciada. |
| **Sistema** | Actor no humano: incorpora automáticamente, desde fuentes externas de datos deportivos, el rendimiento de los jugadores en cada partido disputado (ver UC-05), y ejecuta semanalmente el recálculo de cotización de todos los jugadores en base a ese rendimiento (ver UC-06). Ninguna de las dos cosas es operada manualmente, salvo el disparo manual acotado y auditado que un administrador puede iniciar sobre el recálculo (ver UC-13). |

---

## 4. Casos de uso

Cada caso de uso incluye actor, descripción, precondiciones, flujo principal, flujos alternativos relevantes y criterios de aceptación.

### UC-01 — Registro de usuario

- **Actor:** Visitante.
- **Descripción:** Un visitante crea una cuenta de usuario para poder operar en la plataforma.
- **Precondiciones:** El visitante no tiene ya una cuenta con el mismo identificador (ej. email).
- **Flujo principal:**
  1. El visitante ingresa sus datos de registro.
  2. El sistema valida que el identificador no esté ya registrado.
  3. El sistema crea la cuenta con rol USER.
  4. El sistema asigna al usuario un saldo virtual inicial de valor cero (0).
  5. El usuario queda en condiciones de iniciar sesión.
- **Flujos alternativos:**
  - Si el identificador ya existe, el sistema rechaza el registro e informa el motivo.
- **Criterios de aceptación:**
  - Dado un identificador no usado antes, cuando el visitante completa el registro con datos válidos, entonces la cuenta queda creada con un saldo virtual inicial de cero (0).
  - Dado un identificador ya registrado, cuando se intenta registrar de nuevo con ese identificador, entonces el sistema no crea una cuenta duplicada y muestra un motivo de rechazo claro.
  - Nota: dado que el saldo inicial es cero, un usuario recién registrado necesita que un administrador le acredite saldo virtual (ver UC-14) antes de poder comprar tokens, salvo que dependa únicamente de vender tokens que ya posea (lo cual tampoco es posible sin haber comprado antes).

### UC-02 — Inicio de sesión

- **Actor:** Usuario / Administrador.
- **Descripción:** Una persona con cuenta existente se autentica para acceder a las funciones asociadas a su cuenta.
- **Precondiciones:** La cuenta existe.
- **Flujo principal:**
  1. La persona ingresa sus credenciales.
  2. El sistema valida las credenciales.
  3. El sistema concede acceso según el rol de la cuenta (USER o ADMIN).
- **Flujos alternativos:**
  - Credenciales inválidas: el sistema rechaza el acceso sin indicar cuál dato específico es incorrecto (por seguridad).
- **Criterios de aceptación:**
  - Dadas credenciales correctas, cuando la persona inicia sesión, entonces accede a las funciones correspondientes a su rol.
  - Dadas credenciales incorrectas, cuando la persona intenta iniciar sesión, entonces el acceso es rechazado y no se revela cuál campo falló.

### UC-03 — Alta de jugador

- **Actor:** Administrador.
- **Descripción:** El administrador incorpora un nuevo jugador al catálogo para que pueda ser tokenizado y operado por los usuarios.
- **Precondiciones:** El administrador está autenticado.
- **Flujo principal:**
  1. El administrador ingresa los datos del jugador: sus datos personales/identificatorios, y las métricas de rendimiento que sean necesarias para el cálculo de su cotización (el mismo conjunto de datos que provee el proceso de scraping de rendimiento, ver UC-05 y UC-06). El listado exacto de campos identificatorios (ej. nombre, club, posición, fecha de nacimiento, nacionalidad) es un detalle menor no bloqueante, a definir sin necesidad de reabrir esta especificación.
  2. El sistema crea el jugador en el catálogo.
  3. El sistema habilita al jugador con una emisión disponible de hasta 100 tokens para compra, aún sin cotización calculada.
- **Flujos alternativos:**
  - Datos incompletos o inválidos: el sistema rechaza el alta e informa qué falta corregir.
- **Criterios de aceptación:**
  - Dado un administrador autenticado con datos válidos de un jugador nuevo, cuando da de alta al jugador, entonces el jugador aparece en el catálogo con hasta 100 tokens disponibles para compra.
  - Dado un usuario sin rol ADMIN, cuando intenta dar de alta un jugador, entonces la operación es rechazada.
  - Bajo ninguna circunstancia la cantidad total de tokens habilitados para compra de un jugador supera 100.

### UC-04 — Edición de datos de un jugador

- **Actor:** Administrador.
- **Descripción:** El administrador corrige o actualiza los datos identificatorios de un jugador ya existente (ej. cambio de club).
- **Precondiciones:** El jugador existe en el catálogo.
- **Flujo principal:**
  1. El administrador modifica los datos identificatorios del jugador.
  2. El sistema guarda los cambios.
- **Criterios de aceptación:**
  - Dado un jugador existente, cuando el administrador edita sus datos identificatorios, entonces el catálogo refleja la información actualizada sin alterar la cotización vigente ni las tenencias de tokens de los usuarios.
  - La edición de datos identificatorios nunca modifica el límite de 100 tokens ni la cotización del jugador.

### UC-05 — Ingesta automática de rendimiento de un jugador en un partido

- **Actor:** Sistema (ingesta automática).
- **Descripción:** El sistema incorpora automáticamente, sin que un administrador tenga que cargarlo a mano, la información de cómo le fue a un jugador en un partido disputado. Esa información proviene de las fuentes externas de datos deportivos con las que integra la aplicación (rendimiento detallado del partido por un lado, resultados/alineaciones/fixtures por otro) y queda disponible como insumo para el cálculo semanal de cotización.
- **Precondiciones:** El jugador existe en el catálogo. El partido ya se disputó y sus datos están disponibles en las fuentes externas de las que se nutre el sistema.
- **Flujo principal:**
  1. El sistema obtiene, de forma periódica y desacoplada de cualquier request de un usuario, los datos de rendimiento del jugador en el partido disputado desde las fuentes externas de datos deportivos integradas a la aplicación. Se incorporan todas las métricas de rendimiento que efectivamente provee el proceso de scraping de WhoScored (pases, tiros, intercepciones, calificaciones, y cualquier otra que la fuente entregue), sin una curación previa de un subconjunto acotado de variables de negocio.
  2. El sistema asocia ese rendimiento al jugador y a la semana correspondiente.
  3. Ese rendimiento queda registrado como insumo disponible para el próximo cálculo semanal de cotización.
- **Flujos alternativos:**
  - Si una fuente externa no está disponible o no provee el dato de un partido puntual, la incorporación de ese rendimiento queda pendiente para un próximo intento del proceso de ingesta, sin que eso afecte la disponibilidad del resto de las funciones del sistema.
- **Criterios de aceptación:**
  - Dado un jugador existente y un partido ya disputado con datos disponibles en las fuentes externas integradas, cuando el sistema ejecuta la ingesta automática, entonces todas las métricas de rendimiento que la fuente provee para ese partido quedan disponibles como insumo para el próximo cálculo semanal de cotización de ese jugador, sin que ningún administrador haya tenido que cargarlas manualmente.
  - No queda asociado rendimiento a un jugador que no existe en el catálogo.
  - La indisponibilidad temporal de una fuente externa no impide que el resto del sistema (catálogo, portfolio, compra/venta, etc.) siga funcionando con normalidad.

### UC-06 — Cálculo semanal de cotización

- **Actor:** Sistema (automático, sin intervención humana).
- **Descripción:** Una vez por semana, el sistema recalcula la cotización de cada jugador en base a su rendimiento en los partidos disputados esa semana, y ese valor pasa a ser la cotización vigente hasta el próximo recálculo.
- **Precondiciones:** Corresponde el ciclo semanal de recálculo.
- **Flujo principal:**
  1. El sistema toma el rendimiento registrado de cada jugador durante la semana.
  2. El sistema calcula la nueva cotización de cada jugador utilizando como entrada el conjunto de métricas de rendimiento que efectivamente provee el proceso de scraping de WhoScored (ver UC-05): no hay una selección acotada de variables de negocio elegidas a mano, se parte de todo el dato disponible que la fuente entrega. **Nota para diseño técnico (`tecnico`):** cómo se pondera y combina cada métrica dentro de la fórmula de cálculo es un detalle técnico que corresponde definir en esa etapa, no en esta especificación de producto.
  3. El sistema persiste la nueva cotización como la vigente y conserva la anterior como parte del historial.
  4. El valor de las tenencias en los portfolios de los usuarios se actualiza en base a la nueva cotización.
- **Flujos alternativos:**
  - Un jugador sin rendimiento registrado esa semana: su cotización igualmente se recalcula en ese ciclo — nunca queda congelada al valor de la semana anterior ni se salta el cálculo para ese jugador. **Nota para diseño técnico (`tecnico`):** el criterio concreto de cómo se trata la ausencia de datos de partido dentro del algoritmo (por ejemplo, tratamiento neutro, decaimiento u otro criterio) es una decisión técnica pendiente de definición en esa etapa, no una decisión de producto.
- **Criterios de aceptación:**
  - Dado que se cumple el ciclo semanal, cuando el sistema ejecuta el recálculo, entonces todos los jugadores del catálogo (incluidos los que no tuvieron rendimiento registrado esa semana) tienen una nueva cotización vigente, y la cotización anterior queda accesible en el historial.
  - Ningún actor humano (ni siquiera un ADMIN) puede fijar o sobrescribir manualmente el valor de cotización resultante.
  - La cotización de un jugador nunca cambia fuera de este ciclo semanal (salvo el disparo manual acotado de UC-13, que ejecuta este mismo cálculo).

### UC-07 — Consulta de catálogo y detalle de jugador

- **Actor:** Usuario, Administrador y Visitante. El catálogo de jugadores y sus cotizaciones son públicos: no requieren cuenta ni autenticación (ver actor Visitante, sección 3).
- **Descripción:** Se consulta el listado de jugadores disponibles y el detalle de uno en particular: datos identificatorios, cotización actual, tokens disponibles para compra y frescura de sus datos de rendimiento.
- **Precondiciones:** Ninguna: la consulta es pública.
- **Flujo principal:**
  1. El actor solicita ver el catálogo de jugadores.
  2. El sistema muestra el listado con cotización actual de cada uno.
  3. El actor selecciona un jugador y ve su detalle, incluyendo cuántos de sus 100 tokens siguen disponibles para compra y cuándo fue la última vez que se actualizaron sus datos de rendimiento.
- **Criterios de aceptación:**
  - Dado el catálogo de jugadores, cuando el actor lo consulta, entonces ve, para cada jugador, su cotización vigente (la última calculada por el sistema), sin necesidad de tener una cuenta.
  - Dado un jugador puntual, cuando el actor consulta su detalle, entonces ve cuántos tokens de ese jugador siguen disponibles para ser comprados (sobre el máximo de 100).
  - Dado un jugador puntual, cuando el actor consulta su detalle, entonces ve la fecha y hora de la última actualización de sus datos de rendimiento.
  - Un jugador dado de baja (ver UC-15) se muestra en el catálogo marcado como inactivo, con 0 tokens disponibles para compra, y no admite ninguna operación de compra ni venta sobre él.

### UC-08 — Consulta de cotización histórica de un jugador

- **Actor:** Usuario, Administrador y Visitante. Es una consulta pública, igual que el catálogo (UC-07).
- **Descripción:** Se consulta la evolución de la cotización de un jugador a lo largo de las sucesivas semanas, para entender la tendencia de su rendimiento tokenizado.
- **Precondiciones:** El jugador tiene al menos una cotización calculada.
- **Flujo principal:**
  1. El actor selecciona un jugador.
  2. El actor accede a la vista de cotización histórica.
  3. El sistema muestra los valores de cotización de ese jugador en cada ciclo semanal pasado.
- **Criterios de aceptación:**
  - Dado un jugador con varias cotizaciones calculadas a lo largo del tiempo, cuando el actor consulta su historial, entonces ve cada valor de cotización junto con la semana a la que corresponde, en orden cronológico, sin necesidad de tener una cuenta.
  - Un jugador recién dado de alta, sin cotización calculada todavía, se muestra sin historial (no es un error).
  - El historial de cotización de un jugador dado de baja (ver UC-15) sigue siendo consultable después de la baja, por motivos de trazabilidad, incluyendo la cotización del día anterior a la baja utilizada para compensar a los usuarios.

### UC-09 — Compra de tokens de un jugador

- **Actor:** Usuario.
- **Descripción:** Un usuario adquiere una cantidad de tokens de un jugador, pagando con su saldo virtual a la cotización vigente del jugador.
- **Precondiciones:** El usuario está autenticado. El jugador tiene tokens disponibles para compra (no se llegó al máximo de 100 emitidos). El usuario tiene saldo virtual suficiente para la cantidad solicitada a la cotización vigente.
- **Flujo principal:**
  1. El usuario elige un jugador y una cantidad de tokens a comprar.
  2. El sistema calcula el costo total a la cotización vigente.
  3. El sistema valida que el usuario tenga saldo suficiente y que haya tokens disponibles.
  4. El sistema descuenta el saldo virtual del usuario, acredita los tokens en su portfolio y registra el movimiento.
- **Flujos alternativos:**
  - Saldo insuficiente: la operación se rechaza por completo, sin modificar saldo ni tenencias.
  - Tokens insuficientes disponibles del jugador (se pide más cantidad de la que queda del máximo de 100): la operación se rechaza por completo (confirmado: no se ejecuta una compra parcial por la cantidad disponible).
- **Criterios de aceptación:**
  - Dado saldo suficiente y tokens disponibles del jugador, cuando el usuario compra una cantidad válida de tokens, entonces su saldo virtual disminuye exactamente en el costo total, su tenencia de ese jugador aumenta en la cantidad comprada, y queda un registro del movimiento.
  - Dado saldo insuficiente, cuando el usuario intenta comprar, entonces la operación se rechaza por completo y ni el saldo ni la tenencia cambian.
  - Dado que la compra solicitada haría que la emisión total del jugador supere 100 tokens, cuando el usuario intenta comprar, entonces la operación se rechaza por completo.
  - Dado que se pide una cantidad mayor a la disponible del jugador, la operación se rechaza por completo; nunca se ejecuta una compra parcial por la cantidad efectivamente disponible.
  - No existe un tope adicional a la cantidad de tokens de un mismo jugador que un usuario puede poseer: un usuario puede acumular tantos tokens de un mismo jugador como su saldo virtual le permita comprar, sin más límite que el máximo de 100 tokens emitidos por jugador.
  - Ninguna compra deja al sistema en un estado parcial (por ejemplo, saldo descontado sin tokens acreditados).

### UC-10 — Venta de tokens de un jugador (al sistema)

- **Actor:** Usuario.
- **Descripción:** Un usuario se desprende de una cantidad de tokens de un jugador que posee, recibiendo a cambio saldo virtual calculado a la cotización vigente del jugador. Este caso de uso describe específicamente la venta **al sistema** — una de las vías disponibles para desprenderse de tokens, y la vía por defecto contra la cual siempre hay contraparte garantizada (el propio sistema). Existe, además, una vía alternativa de venta directa a otros usuarios mediante una oferta abierta (operación P2P, ver UC-17), que convive con esta sin reemplazarla ni limitarla: un usuario puede elegir vender al sistema (este caso de uso) o publicar sus tokens para que cualquier otro usuario interesado los compre (UC-17), según le convenga.
- **Precondiciones:** El usuario está autenticado y posee al menos la cantidad de tokens de ese jugador que quiere vender.
- **Flujo principal:**
  1. El usuario elige un jugador del que posee tokens y una cantidad a vender.
  2. El sistema valida que el usuario posea al menos esa cantidad.
  3. El sistema calcula el monto a acreditar a la cotización vigente.
  4. El sistema descuenta los tokens del portfolio del usuario, acredita el saldo virtual correspondiente, libera esa cantidad de tokens de vuelta a la emisión disponible del jugador (vuelven a estar disponibles para que otros usuarios los compren) y registra el movimiento.
- **Flujos alternativos:**
  - El usuario intenta vender más tokens de los que posee: la operación se rechaza por completo.
- **Criterios de aceptación:**
  - Dado que el usuario posee una cantidad de tokens de un jugador, cuando vende una cantidad igual o menor a esa tenencia, entonces su tenencia disminuye en la cantidad vendida, su saldo virtual aumenta en el monto correspondiente a la cotización vigente, y queda un registro del movimiento.
  - Dado que el usuario intenta vender más tokens de los que posee, cuando ejecuta la venta, entonces la operación se rechaza por completo y ni el saldo ni la tenencia cambian.
  - Ninguna venta deja al sistema en un estado parcial.
  - Los tokens vendidos vuelven a estar disponibles en el mercado para que otros usuarios los compren: la emisión total disponible del jugador se mantiene, no se retira de circulación de forma permanente por una venta. **Esto es distinto de lo que ocurre con la baja de un jugador (UC-15), donde los tokens sí se retiran de circulación de forma permanente.**
  - Esta vía (venta al sistema) convive con la vía P2P (UC-17) sin reemplazarla: ambas están disponibles simultáneamente para que un usuario se desprenda de tokens que posee.

### UC-11 — Consulta de portfolio del usuario

- **Actor:** Usuario.
- **Descripción:** El usuario consulta su cartera: qué tokens posee, de qué jugadores, y cuál es el valor actual de cada tenencia y del total, a la cotización vigente.
- **Precondiciones:** El usuario está autenticado.
- **Flujo principal:**
  1. El usuario accede a su portfolio.
  2. El sistema muestra, por cada jugador del que posee tokens, la cantidad poseída y su valor actual (cantidad × cotización vigente), además del saldo virtual disponible y el valor total del portfolio.
- **Criterios de aceptación:**
  - Dado un usuario con tenencias en uno o más jugadores, cuando consulta su portfolio, entonces ve, para cada jugador, la cantidad de tokens que posee y el valor actual de esa tenencia a la cotización vigente.
  - El valor total mostrado del portfolio es la suma del valor de todas las tenencias más el saldo virtual disponible.
  - Un usuario sin tenencias ve su portfolio vacío (no es un error), junto con su saldo virtual disponible.

### UC-12 — Consulta de historial de movimientos del usuario

- **Actor:** Usuario.
- **Descripción:** El usuario consulta el historial de sus propias compras y ventas de tokens, para entender cómo llegó a la composición actual de su portfolio.
- **Precondiciones:** El usuario está autenticado.
- **Flujo principal:**
  1. El usuario accede a su historial de movimientos.
  2. El sistema muestra cada compra y venta realizada por el usuario, con jugador, cantidad, precio al momento de la operación y fecha.
- **Criterios de aceptación:**
  - Dado un usuario con operaciones previas, cuando consulta su historial, entonces ve cada movimiento con jugador, tipo de operación (compra/venta), cantidad, precio al momento de la operación y fecha, en orden cronológico.
  - Un usuario sin movimientos ve su historial vacío (no es un error).
  - Un usuario solo puede ver su propio historial, nunca el de otro usuario.
  - La compensación recibida por la baja de un jugador (ver UC-15) también queda reflejada en este historial, identificada como tal (no como una venta ordinaria).
  - Toda operación P2P (ver UC-17) en la que el usuario haya participado, ya sea como vendedor o como comprador, queda reflejada en este historial, identificada como operación P2P (no como una compra o venta ordinaria contra el sistema).

### UC-13 — Disparo manual del recálculo de cotización

- **Actor:** Administrador.
- **Descripción:** Un administrador dispara manualmente, fuera del ciclo semanal automático, el mismo cálculo algorítmico de cotización que ejecuta el sistema cada semana (UC-06). Está pensado para necesidades operativas — por ejemplo, validar una corrección o resolver una incidencia puntual — nunca para fijar un valor de cotización a mano.
- **Precondiciones:** El administrador está autenticado con rol ADMIN.
- **Flujo principal:**
  1. El administrador solicita el disparo manual del recálculo de cotización.
  2. El sistema ejecuta exactamente el mismo cálculo algorítmico que utiliza el ciclo semanal automático (UC-06), tomando el rendimiento disponible en ese momento.
  3. El sistema persiste la nueva cotización resultante como la vigente por cada jugador afectado, conservando la anterior en el historial, igual que en el ciclo semanal.
  4. El sistema deja registro en el log de auditoría de quién disparó el recálculo, cuándo, y los valores antes/después por jugador afectado.
- **Flujos alternativos:**
  - Un actor sin rol ADMIN intenta disparar el recálculo: el sistema rechaza la operación.
- **Criterios de aceptación:**
  - Dado un administrador autenticado, cuando dispara el recálculo manual, entonces el sistema ejecuta el mismo algoritmo determinístico que el job semanal y persiste la nueva cotización vigente por cada jugador afectado, conservando el historial.
  - El disparo manual no acepta ni admite como entrada un valor de cotización: sólo puede iniciar el cálculo, nunca fijarlo directamente.
  - Dado un actor sin rol ADMIN, cuando intenta disparar el recálculo manual, entonces la operación se rechaza.
  - Toda ejecución del disparo manual queda registrada en el log de auditoría, con quién la ejecutó, cuándo, y los valores de cotización antes/después por jugador afectado.
  - El disparo manual no reemplaza ni altera la periodicidad del ciclo semanal automático (UC-06): ambos coexisten como dos formas distintas de invocar el mismo cálculo.

### UC-14 — Recarga de saldo virtual de un usuario por un administrador

- **Actor:** Administrador.
- **Descripción:** Un administrador acredita saldo virtual adicional en la cuenta de un usuario, más allá del saldo inicial en cero de UC-01 y de lo que el usuario gane vendiendo tokens (UC-10).
- **Precondiciones:** El administrador está autenticado con rol ADMIN. La cuenta del usuario destino existe.
- **Flujo principal:**
  1. El administrador selecciona la cuenta de un usuario y un monto de saldo virtual a acreditar.
  2. El sistema valida que el monto sea válido (positivo).
  3. El sistema acredita el monto al saldo virtual del usuario y registra el movimiento.
- **Flujos alternativos:**
  - Un actor sin rol ADMIN intenta recargar saldo a un usuario: la operación se rechaza.
  - Monto inválido (cero, negativo o el usuario destino no existe): la operación se rechaza.
- **Criterios de aceptación:**
  - Dado un administrador autenticado y un usuario existente, cuando el administrador acredita un monto válido de saldo virtual, entonces el saldo virtual del usuario aumenta exactamente en ese monto y queda un registro trazable de la operación (quién la ejecutó, sobre qué cuenta, cuándo, monto).
  - Dado un actor sin rol ADMIN, cuando intenta recargar saldo virtual a un usuario, entonces la operación se rechaza.
  - Dado un monto inválido o un usuario destino inexistente, cuando se intenta la recarga, entonces la operación se rechaza sin modificar ningún saldo.

> **Nota de terminología (resuelta):** el dueño del producto se refirió a este actor como "Superusuario" al describir esta capacidad ("existe un Superusuario (admin), el cual puede cargar crédito a otro [usuario]"). El dueño del producto confirmó que "Superusuario" y "Administrador" (rol ADMIN) son el mismo rol: no son dos roles distintos, sino dos nombres válidos e intercambiables para el mismo rol. Se mantiene "Administrador"/rol ADMIN como término principal en el resto de esta especificación por consistencia con `constitution.md`.

### UC-15 — Baja (inactivación) de un jugador

- **Actor:** Administrador.
- **Descripción:** Un administrador da de baja (inactiva) un jugador ya existente en el catálogo. A diferencia de una venta, los tokens de ese jugador no vuelven a estar disponibles para que otros usuarios los compren: se retiran del mercado por completo. Cada usuario que poseía tokens de ese jugador recibe, como compensación, saldo virtual calculado a la cotización de mercado que el token tenía el día anterior a la baja.
- **Precondiciones:** El jugador existe en el catálogo. El administrador está autenticado con rol ADMIN.
- **Flujo principal:**
  1. El administrador selecciona un jugador y solicita darlo de baja.
  2. El sistema identifica, para cada usuario, la cantidad de tokens que posee de ese jugador, y toma la cotización que el jugador tenía el día anterior a la baja como cotización de referencia para la compensación.
  3. El sistema retira de los portfolios de los usuarios los tokens que poseían de ese jugador.
  4. El sistema acredita a cada uno de esos usuarios, como compensación, saldo virtual equivalente a la cantidad de tokens retirada multiplicada por la cotización de referencia (la del día anterior a la baja), y registra el movimiento en su historial (ver UC-12), identificado como compensación por baja y no como una venta ordinaria.
  5. El sistema retira del mercado, de forma permanente, la totalidad de los tokens emitidos del jugador: esos tokens no vuelven a estar disponibles para compra por ningún usuario.
  6. El sistema marca al jugador como inactivo y registra la operación de baja de forma trazable.
- **Flujos alternativos:**
  - Un actor sin rol ADMIN intenta dar de baja un jugador: la operación se rechaza.
- **Criterios de aceptación:**
  - Dado un jugador existente con usuarios que poseen tokens de él, cuando un administrador lo da de baja, entonces esos usuarios dejan de tener esos tokens en su portfolio y reciben en su lugar saldo virtual equivalente a la cantidad retirada multiplicada por la cotización del jugador correspondiente al día anterior a la baja (nunca la cotización del momento mismo de la baja).
  - Los tokens retirados por la baja de un jugador no vuelven a estar disponibles para compra por ningún usuario: quedan retirados de circulación de forma permanente, a diferencia de lo que ocurre con una venta (UC-10, regla 8 de la sección 5).
  - Dado un actor sin rol ADMIN, cuando intenta dar de baja un jugador, la operación se rechaza.
  - La baja de un jugador, la compensación acreditada a cada usuario afectado y la cotización de referencia (día anterior) utilizada quedan registradas de forma trazable (quién ejecutó la baja, cuándo, jugador afectado, usuarios compensados y monto de cada compensación).

> **Nota de producto (inferencia razonable, no una afirmación literal del dueño del producto):** la respuesta del dueño del producto sobre la baja de un jugador resuelve explícitamente el mecanismo central (retiro permanente de circulación y compensación a la cotización del día anterior), pero no aclara de forma literal si un jugador dado de baja sigue siendo consultable en el catálogo público (UC-07) y en su historial de cotización (UC-08) después de la baja. Dado que sus tokens quedan retirados del mercado, se infiere razonablemente que el jugador queda marcado como **inactivo**, con 0 tokens disponibles para compra, pudiendo seguir siendo consultable a nivel de catálogo e histórico de cotización por motivos de trazabilidad, pero sin admitir ninguna operación de compra o venta sobre él (ver criterios agregados en UC-07 y UC-08). Esto queda incorporado como inferencia de producto derivada de la respuesta recibida, no como una instrucción literal del dueño del producto; si en el futuro se aclara lo contrario, alcanza con ajustar este punto puntual de visibilidad, sin reabrir el mecanismo central de la baja (retiro de circulación + compensación al día anterior), que sí está confirmado de forma explícita.

### UC-16 — Consulta de ranking de jugadores

- **Actor:** Usuario, Administrador y Visitante. Es una consulta pública, igual que el catálogo (UC-07) y la cotización histórica (UC-08).
- **Descripción:** Cualquier persona puede consultar un ranking de jugadores, ordenados según una estrategia de ranking activa. Se confirma que existirá más de una estrategia posible (al menos dos), pero cuáles son concretamente esas estrategias y sus criterios de cálculo queda pendiente de definición en la etapa técnica.
- **Precondiciones:** Ninguna: la consulta es pública.
- **Flujo principal:**
  1. El actor solicita ver el ranking de jugadores.
  2. El sistema ordena a los jugadores del catálogo según la estrategia de ranking activa en ese momento.
  3. El sistema muestra el listado ordenado resultante.
- **Criterios de aceptación:**
  - Dado el catálogo de jugadores, cuando cualquier actor consulta el ranking, entonces ve el listado de jugadores ordenado según la estrategia activa, sin necesidad de tener una cuenta.

> **Nota para diseño técnico (`tecnico`):** el dueño del producto confirmó explícitamente que existen al menos dos estrategias de ranking posibles ("no, hay al menos 2. Por el momento no están definidas. Luego se implementará en la parte técnica"), pero cuáles son concretamente (por ejemplo, variación de cotización, rendimiento reciente, u otro criterio) y cómo se selecciona o activa una entre ellas es una decisión técnica pendiente de esa etapa, no de producto. Este caso de uso queda definido a nivel de producto como "existe la funcionalidad de ranking, con al menos dos estrategias posibles", sin fijar cuáles.

### UC-17 — Venta directa entre usuarios mediante oferta abierta (P2P)

- **Actor:** Usuario (vendedor) — publica/pone a la venta una cantidad de tokens que posee de un jugador. Usuario (comprador) — cualquier otro usuario interesado con saldo suficiente, no un destinatario elegido de antemano por el vendedor.
- **Descripción:** Un usuario vendedor ofrece una cantidad de sus tokens de un jugador para la venta directa entre usuarios. Es una **oferta abierta**: cualquier usuario interesado con saldo suficiente puede comprarla, en todo o en parte, sin que el vendedor la dirija de antemano a un comprador específico. La operación se ejecuta automáticamente en el momento en que un comprador interesado decide comprar, sin necesidad de que el vendedor confirme esa compra puntual ni de que el comprador realice ningún paso adicional más allá de decidir comprar. El precio es siempre la cotización oficial vigente del jugador en el momento de la ejecución — el vendedor publica una cantidad de tokens, nunca un precio.
- **Precondiciones:** El usuario vendedor está autenticado y posee al menos la cantidad de tokens del jugador que publica para la venta. En el momento en que un comprador decide comprar, éste tiene saldo virtual suficiente para pagar la cantidad que quiere adquirir a la cotización oficial vigente.
- **Flujo principal:**
  1. El vendedor publica una cantidad de tokens de un jugador que posee, para la venta directa a cualquier usuario interesado.
  2. Un usuario comprador interesado decide comprar una cantidad de esa oferta (hasta el total publicado).
  3. El sistema valida que el comprador tenga saldo virtual suficiente a la cotización oficial vigente del jugador, y que la cantidad solicitada siga disponible en la oferta.
  4. El sistema ejecuta, en una única operación atómica, los cuatro movimientos: débito de los tokens del vendedor, crédito del saldo virtual correspondiente al vendedor, débito del saldo virtual del comprador y crédito de los tokens al comprador.
  5. El movimiento queda reflejado en el historial de movimientos de ambos usuarios (ver UC-12), identificado como operación P2P, para diferenciarlo de una compra/venta contra el sistema.
- **Flujos alternativos:**
  - Un comprador interesado no tiene saldo virtual suficiente en el momento de intentar comprar: se rechaza esa operación puntual, sin afectar el resto de la oferta publicada ni a otros compradores interesados.
  - El vendedor no posee (o deja de poseer) la cantidad de tokens que publicó: no puede publicarse, o la oferta se ajusta/retira según la tenencia real disponible del vendedor.
  - Más de un comprador interesado compite por la misma oferta limitada al mismo tiempo: el sistema garantiza que nunca se vende más cantidad que la publicada por el vendedor; gana la cantidad disponible en cada momento el primer comprador cuya operación se confirma válidamente, sin que esto implique en ningún caso una variación de precio entre los compradores que compiten. (La resolución de esta concurrencia es un detalle técnico, no de producto.)
- **Criterios de aceptación:**
  - Dado que el vendedor tiene publicada una cantidad de tokens de un jugador, cuando cualquier usuario interesado con saldo suficiente decide comprar una cantidad de esa oferta (hasta el total publicado), entonces la operación se ejecuta automáticamente en ese mismo momento, sin ningún paso adicional de confirmación por parte del vendedor ni del comprador.
  - El precio utilizado en toda operación P2P es siempre la cotización oficial vigente del jugador en el momento de la ejecución; el vendedor nunca publica ni pacta un precio, sólo una cantidad.
  - Dado que un comprador interesado no tiene saldo virtual suficiente en el momento de intentar comprar, esa operación puntual se rechaza por completo, sin afectar el resto de la oferta publicada ni el saldo o tenencia de ninguno de los dos usuarios.
  - El sistema nunca permite que se venda, en conjunto, más cantidad de tokens que la publicada por el vendedor en una oferta, incluso cuando varios compradores interesados intentan comprar de ella al mismo tiempo.
  - Ninguna operación P2P deja al sistema en un estado parcial: los cuatro movimientos (débito de tokens y crédito de saldo en el vendedor, débito de saldo y crédito de tokens en el comprador) se aplican todos o ninguno.
  - La vía P2P convive con la compra/venta contra el sistema (UC-09/UC-10) sin reemplazarla ni limitarla: un usuario puede seguir comprando tokens nuevos al sistema y vendiéndoselos de vuelta, además de usar esta vía de oferta abierta entre usuarios.

---

## 5. Reglas de negocio heredadas de la constitución (no renegociables en esta spec)

Estas reglas ya fueron fijadas como decisiones de producto confirmadas por el dueño del producto en `constitution.md` y se toman como marco fijo para todos los casos de uso anteriores:

1. Ningún jugador puede tener jamás más de 100 tokens emitidos para compra, bajo ninguna operación.
2. La cotización de un jugador se determina exclusivamente por un cálculo algorítmico basado en su rendimiento en partidos. El precio nunca surge de oferta/demanda ni de negociación entre usuarios —ni siquiera en la vía P2P (UC-17), que siempre opera a la cotización oficial vigente— ni de un ajuste manual de administrador como mecanismo de precio.
3. La cotización se recalcula y persiste únicamente mediante el ciclo semanal centralizado o el disparo manual acotado a ADMIN (UC-13), ambos ejecutando el mismo algoritmo. No hay actualización de cotización en tiempo real, ni bajo demanda de un usuario final, ni por ajuste manual de un valor.
4. El saldo con el que operan los usuarios es exclusivamente virtual, interno a la aplicación. No hay dinero real en ningún punto del sistema, ni pasarelas de pago.
5. Toda operación que modifique saldo, tenencia de tokens o cotización debe quedar registrada de forma trazable (quién, qué, cuándo, valores antes/después) — reflejado funcionalmente en UC-12 (historial de movimientos) para el usuario, además del registro de auditoría interno.
6. El disparo manual del recálculo de cotización (UC-13), restringido a ADMIN, ejecuta exactamente el mismo algoritmo determinístico que el ciclo semanal automático (UC-06) y en ningún caso permite fijar o sobrescribir un valor de cotización a mano.

Además, las siguientes reglas fueron confirmadas como decisiones de producto firmes al resolver las preguntas abiertas de esta especificación (no provienen directamente de la constitución, pero quedan igual de firmes para el resto del proyecto):

7. No existe un tope de concentración por usuario: un usuario puede acumular tantos tokens de un mismo jugador como su saldo virtual le permita comprar, sin más límite que el máximo de 100 tokens emitidos por jugador (ver UC-09).
8. Los tokens vendidos por un usuario al sistema (UC-10) vuelven a la emisión disponible del jugador: quedan nuevamente disponibles para que otros usuarios los compren. La emisión total no se reduce de forma permanente por el hecho de que un usuario venda sus tokens al sistema.
9. La baja (inactivación) de un jugador (UC-15) es un mecanismo distinto al de una venta: retira sus tokens de circulación de forma **permanente** (no vuelven a estar disponibles para compra por ningún usuario) y compensa a cada usuario afectado con saldo virtual equivalente a su tenencia valuada a la cotización que el jugador tenía el **día anterior** a la baja (no la cotización del momento de la baja).
10. La vía P2P (UC-17) es una **oferta abierta**: el vendedor publica una cantidad de tokens que posee de un jugador, disponible para que cualquier usuario interesado con saldo suficiente la compre; no es una transferencia dirigida de antemano a un comprador específico elegido por el vendedor. Se ejecuta automáticamente en el momento en que un comprador decide comprar, sin paso de confirmación adicional de ninguna de las partes, siempre a la cotización oficial vigente del jugador, nunca a un precio negociado o pactado entre las partes. El sistema garantiza que nunca se vende más cantidad que la publicada por el vendedor, incluso si compiten varios compradores interesados al mismo tiempo (detalle de concurrencia que se resuelve técnicamente, no de producto). Esta vía convive con la compra/venta contra el sistema (UC-09/UC-10) como una vía adicional, sin reemplazarla ni limitarla.

---

## 6. Preguntas abiertas

De las 13 preguntas abiertas originales de esta sección, 11 fueron respondidas directamente por el dueño del producto y quedaron incorporadas como reglas firmes o casos de uso nuevos/actualizados a lo largo de este documento (saldo inicial → UC-01; tope de concentración → regla 7 de la sección 5 y UC-09; destino de tokens vendidos → UC-10 y regla 8 de la sección 5; variables de rendimiento relevantes → UC-05/UC-06, con el detalle de ponderación derivado a `tecnico`; jugador sin partidos en la semana → UC-06, con el criterio de tratamiento derivado a `tecnico`; visibilidad pública del catálogo → sección 3 y UC-07/UC-08; gestión de cuenta → sección 2.2; compra parcial → UC-09; datos identificatorios de un jugador → UC-03; ranking de jugadores → UC-16, con las estrategias concretas derivadas a `tecnico`; frescura de los datos → UC-07). Las otras 2 preguntas originales dieron lugar a sendas preguntas nuevas, y ambas ya fueron respondidas por el dueño del producto y quedaron resueltas en este documento:

- Terminología "Superusuario" vs. "Administrador" (surgida de UC-14): resuelta — son el mismo rol ADMIN, ambos nombres son válidos e intercambiables (ver sección 3 y nota de UC-14).
- Alcance exacto de la baja de un jugador (surgida de UC-15): resuelta — sus tokens se retiran de circulación de forma permanente (no vuelven a estar disponibles para compra) y cada usuario afectado es compensado con saldo virtual a la cotización del día anterior a la baja (ver UC-15 y regla 9 de la sección 5).

El único punto que el dueño del producto no confirmó de forma literal —si un jugador dado de baja sigue siendo consultable en el catálogo público (UC-07) y en su histórico de cotización (UC-08) tras la baja— fue resuelto mediante una inferencia razonable de producto, dejada explícitamente marcada como tal en la nota al pie de UC-15, sin afectar el mecanismo central de la baja (retiro permanente de circulación + compensación al valor del día anterior), que sí está confirmado de forma explícita.

A partir de la incorporación de la capacidad P2P (constitución, sección 2; UC-17 de esta especificación) había surgido una pregunta abierta nueva sobre el consentimiento del comprador, que ya fue respondida por el dueño del producto:

- **Consentimiento del comprador en una operación P2P (surgida de UC-17): resuelta.** El dueño del producto confirmó textualmente: "automatico, si el vendedor inicia la venta y alguien la quiere comprar, mientras tenga saldo suficiente, la compra y se realiza la transaccion". Esto confirma que no hace falta un paso de confirmación explícita del comprador: la operación se ejecuta automáticamente en el momento en que un comprador interesado decide comprar, sujeta únicamente a la validación de saldo suficiente en ese momento. Además, esta respuesta reveló que el modelo correcto de UC-17 no era una transferencia dirigida a un comprador específico elegido de antemano por el vendedor, sino una **oferta abierta**: el vendedor publica una cantidad de tokens y cualquier usuario interesado con saldo suficiente puede comprarla. UC-17 fue reescrito en consecuencia (ver arriba), de forma consistente con la corrección ya incorporada en `constitution.md` (sección 2).

**En síntesis: esta especificación tiene 17 casos de uso (UC-01 a UC-17) y 0 (cero) preguntas abiertas genuinamente pendientes.** Todas las preguntas planteadas a lo largo del documento fueron respondidas por el dueño del producto o resueltas mediante una inferencia razonable explícitamente marcada como tal (ver nota de UC-15), sin que quede ninguna decisión de producto pendiente de definición antes de pasar a la etapa técnica.
