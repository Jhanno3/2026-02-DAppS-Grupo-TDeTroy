---
name: run-valuacion-jugadores
description: Build, run, and drive the valuacion-jugadores full-stack app (Spring Boot backend + Vite/React frontend). Use when asked to start the app, launch the backend or frontend, take a screenshot of a screen, or click/fill/verify something in the running UI.
---

Full-stack web app: Spring Boot backend (repo root, port 8080) + Vite/React
frontend (`frontend/`, port 5173). Drive it via
`.claude/skills/run-valuacion-jugadores/driver.mjs`, a small
chromium-cli-style REPL — `chromium-cli` itself isn't installed in this
environment (authored on Windows, not the Linux container `chromium-cli`
normally ships in), so this driver is the stand-in. Commands below assume
the agent's shell is Git Bash (the `Bash` tool in this environment); a
plain PowerShell session needs the `.cmd` variants noted in Gotchas.

## Prerequisites

- **Docker Desktop** running (for the dev Postgres container). If it's not
  running, `docker compose up -d db` fails with a daemon-connection error —
  launch it and poll before continuing:

  ```bash
  powershell -Command "Start-Process 'C:\Program Files\Docker\Docker\Docker Desktop.exe'"
  timeout 90 bash -c 'until docker info >/dev/null 2>&1; do sleep 3; done'
  ```

- JDK 21 and Node.js already on PATH (both present in this environment;
  `pom.xml` pins `java.version=21`).

## Setup

```bash
cd C:/Users/carne/Desktop/Facu/Dessarrollo/2026-02-DAppS-Grupo-TDeTroy
docker compose up -d db          # idempotent — safe to re-run
```

Driver dependencies (one-time, inside the skill dir):

```bash
cd .claude/skills/run-valuacion-jugadores
npm install                      # installs playwright@1.63.0
npx playwright install chromium  # only needed if the browser binary isn't
                                  # already cached under
                                  # C:\Users\<user>\AppData\Local\ms-playwright
```

No env vars required — `application.properties` and `frontend`'s Vite
config both default to `localhost` dev values already.

## Build

No separate build step for running in dev mode — `mvnw spring-boot:run`
compiles on the fly and `npm run dev` doesn't need a build. (`npm run
build` and `./mvnw verify` exist for the production/CI path — see Test.)

## Run (agent path)

Launch both servers in the background from repo root, then poll each:

```bash
nohup ./mvnw spring-boot:run > /tmp/backend.log 2>&1 &
timeout 90 bash -c 'until curl -sf http://localhost:8080/v3/api-docs >/dev/null 2>&1; do sleep 3; done'

cd frontend && nohup npm run dev > /tmp/frontend.log 2>&1 & cd ..
timeout 30 bash -c 'until curl -sf http://localhost:5173 >/dev/null 2>&1; do sleep 2; done'
```

Drive it by piping commands into the REPL:

```bash
cd .claude/skills/run-valuacion-jugadores
node driver.mjs <<'EOF'
nav http://localhost:5173/login
wait-for text=Iniciar sesión
screenshot login-page
console
quit
EOF
```

Screenshots land in `.claude/skills/run-valuacion-jugadores/screenshots/`.

Driver commands:

| command | what it does |
|---|---|
| `nav <url>` | Navigate to a URL. |
| `wait-for text=<text>` | Wait for text to appear anywhere on the page. |
| `wait-for url=<pattern>` | Wait for the URL to match (glob, e.g. `**/login`). |
| `wait-for <css-selector>` | Wait for an element matching the selector. |
| `click <selector-or-text=...>` | Click the first match. |
| `fill <selector> <value>` | Fill a form field (real Playwright `fill` — required for React controlled inputs, see Gotchas). |
| `press <key>` | Keyboard key press (e.g. `Enter`). |
| `text <selector>` | Print an element's text content — quick assertion without a screenshot. |
| `screenshot [name]` | Save a PNG to `screenshots/<name or shot-NN>.png`. |
| `console` | Print captured `console.error`/`pageerror` events so far. |
| `login-as <email> <password>` | Skip the login form: calls `POST /api/v1/auth/login` directly and seeds `localStorage['valuacion.token']` (same key `AuthContext` reads) so the next `nav` loads already authenticated. Requires the page to already be on `localhost:5173` (localStorage is per-origin). Only works for a real, already-registered `USER` account (registration is public; `ADMIN` isn't — see `set-token` below). |
| `set-token <jwt>` | Seed `localStorage['valuacion.token']` with an already-minted JWT directly, no backend call. For `ADMIN`: there's no public registration for that role (`RolUsuario`'s javadoc: "no hay alta pública de ADMIN en el MVP"), so `login-as` has nothing to log into — mint one with `mint-jwt.mjs` (below) instead. Same per-origin requirement as `login-as`. |
| `quit` | Close the browser and exit. |

**Testing as ADMIN** (no public registration for that role — see `set-token` above): mint a token locally with the dev JWT secret (`app.jwt.secret`'s default in `application.properties`, a documented non-sensitive local-only placeholder) and feed it to `set-token`:

```bash
ADMIN_TOKEN=$(node mint-jwt.mjs ADMIN)
node driver.mjs <<EOF
nav http://localhost:5173/
set-token $ADMIN_TOKEN
nav http://localhost:5173/admin/jugadores
wait-for text=Administrar jugadores
screenshot admin-jugadores
quit
EOF
```

`mint-jwt.mjs` takes `USER` or `ADMIN` and an optional email; claims match `JwtService.generarToken`'s exact shape so the backend accepts it like any other token.

Stop the servers when done (find the PID on the port — `npm`'s own PID
doesn't receive signals cleanly, killing the actual listener does):

```bash
netstat -ano | grep -E ':(8080|5173) ' | grep LISTENING
powershell -Command "Stop-Process -Id <pid> -Force"
```

## Run (human path)

```bash
docker compose up -d db
./mvnw spring-boot:run           # or, in PowerShell: .\mvnw.cmd spring-boot:run
```

Second terminal:

```bash
cd frontend
npm run dev                      # -> http://localhost:5173
```

Ctrl-C each to stop.

## Test

```bash
./mvnw verify -Ddependency-check.skip=true   # backend: build + tests + JaCoCo + Spotless gates
cd frontend && npm run lint && npm run format:check && npm run build   # frontend: no unit tests yet (T8.x)
```

`dependency-check.skip=true` skips OWASP Dependency-Check, which needs an
NVD API key not configured in this environment (see `.github/workflows/ci.yml`
for the same flag, same reason).

---

## Gotchas

- **`./mvnw` vs `.\mvnw.cmd`** — in Git Bash (this environment's shell),
  `./mvnw` runs fine (it's a POSIX `#!/bin/sh` script). A raw PowerShell
  session can't execute it directly and needs `.\mvnw.cmd` instead — this
  tripped up a human user running the same project outside this driver.
- **Docker Desktop isn't always running.** `docker compose up -d db` fails
  with `open //./pipe/dockerDesktopLinuxEngine` if the Docker Desktop app
  itself isn't started — starting the daemon service isn't enough on
  Windows, the whole app has to launch. See Prerequisites for the
  launch-and-poll snippet.
- **`chromium-cli` isn't installed here.** This environment was set up on
  Windows without it; `driver.mjs` reimplements just enough of its
  vocabulary (`nav`/`wait-for`/`click`/`fill`/`screenshot`/`console`) to
  not need it. If a future environment does have `chromium-cli`, prefer it
  and skip the driver.
- **React controlled inputs need real `fill`, never `eval el.value = ...`.**
  Playwright's `fill`/`type` go through the actual input pipeline and fire
  React's `onChange`; setting `.value` directly leaves React's state
  (and therefore the submit) unaware anything changed.
- **A bare `rl.on('line', async ...)` races ahead on a piped heredoc** —
  readline fires `line` events for every buffered line before the first
  async handler resolves, so commands can execute out of order. The driver
  uses `for await (const line of rl)` instead, which awaits each command
  before reading the next — required for multi-step scripts like the ones
  above to run in the order they're written.
- **Killing the dev servers**: `npm run dev &`'s PID (`$!`) is the `npm`
  wrapper, not the Vite process it spawns — killing it doesn't free port
  5173. Find the actual listener via `netstat -ano | grep LISTENING` and
  kill that PID instead (same story for `mvnw spring-boot:run` and 8080).
- **`fill`'s selector can't contain a space.** The driver splits `fill`'s
  argument on the *first* space (selector vs. value), so a Playwright
  chained selector like `input[type="text"] >> nth=0` gets mangled — the
  `>> nth=0` part becomes part of the value instead. `click` doesn't have
  this problem (its whole argument passes straight to `page.locator()`,
  which understands `>>` natively). Fix: give the input a `name` and use
  `input[name="..."]` — this project's forms all have real `name`
  attributes on their fields for exactly this reason.
- **`text <selector>` reads `textContent`, which is empty for `<input>`.**
  An input's value isn't a child text node, so `text input[name="x"]`
  always prints nothing — it's the wrong tool for checking what's *in* a
  field. A screenshot is the reliable check for input values; the driver
  doesn't have an `input-value` command (add one if a future task needs to
  assert on it programmatically instead of by eye).
- **Seeding multiple test players makes list order non-obvious.** With
  several rows on `/admin/jugadores` or `/jugadores`, `nth=0`-style
  selectors pick whatever the backend's `findAll()` order happens to be
  (not insertion order, not guaranteed) — don't assume position N is a
  specific player. Target by visible text (`text=<player name>`) or scope
  a locator to the row containing that text instead.
