#!/usr/bin/env node
// Mints an HS256 JWT matching JwtService.generarToken's exact claim shape
// (src/main/java/com/tdetroy/valuacion/config/JwtService.java), signed with the local-dev
// secret from application.properties (app.jwt.secret's default — explicitly documented there as
// a non-sensitive dev-only placeholder, never used outside local dev).
//
// For roles with no public registration path (ADMIN: "no hay alta pública de ADMIN en el MVP",
// RolUsuario's javadoc) this is how the driver gets a valid token to test admin-only screens
// with — see `set-token` in driver.mjs / SKILL.md.
//
// Usage: node mint-jwt.mjs <rol: USER|ADMIN> [email]

import crypto from 'node:crypto'

const SECRET =
  'secreto-de-desarrollo-local-nunca-usar-en-produccion-cambiar-por-variable-de-entorno'

const rol = process.argv[2]
if (rol !== 'USER' && rol !== 'ADMIN') {
  console.error('Usage: node mint-jwt.mjs <USER|ADMIN> [email]')
  process.exit(1)
}
const email = process.argv[3] ?? `${rol.toLowerCase()}-driver-check@example.com`

function base64url(input) {
  return Buffer.from(input)
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}

const now = Math.floor(Date.now() / 1000)
const headerB64 = base64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
const payloadB64 = base64url(
  JSON.stringify({
    sub: email,
    usuarioId: crypto.randomUUID(),
    rol,
    iat: now,
    exp: now + 3600,
  }),
)
const signature = crypto
  .createHmac('sha256', SECRET)
  .update(`${headerB64}.${payloadB64}`)
  .digest('base64')
  .replace(/\+/g, '-')
  .replace(/\//g, '_')
  .replace(/=+$/, '')

console.log(`${headerB64}.${payloadB64}.${signature}`)
