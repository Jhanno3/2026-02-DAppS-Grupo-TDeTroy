#!/usr/bin/env node
// Minimal chromium-cli-style REPL driver, for environments where the real
// chromium-cli tool isn't installed (this skill was authored on Windows,
// where it wasn't available). Reads one command per line from stdin,
// processes them strictly in order (for-await, not a bare event listener —
// a plain `rl.on('line', async ...)` would race ahead on a piped heredoc).
//
// Commands: nav, wait-for, click, fill, press, text, screenshot, console, quit.
// See SKILL.md for the full command reference and worked examples.

import { chromium } from 'playwright'
import { createInterface } from 'node:readline'
import { mkdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const SCREENSHOT_DIR = join(__dirname, 'screenshots')
mkdirSync(SCREENSHOT_DIR, { recursive: true })

const consoleErrors = []
const browser = await chromium.launch()
const page = await browser.newPage()
page.on('console', (msg) => {
  if (msg.type() === 'error') consoleErrors.push(msg.text())
})
page.on('pageerror', (err) => consoleErrors.push(String(err)))

let shotCount = 0

function resolveLocator(arg) {
  if (arg.startsWith('text=')) return page.getByText(arg.slice('text='.length))
  return page.locator(arg)
}

async function handle(line) {
  const trimmed = line.trim()
  if (!trimmed || trimmed.startsWith('#')) return

  const spaceIdx = trimmed.indexOf(' ')
  const cmd = spaceIdx === -1 ? trimmed : trimmed.slice(0, spaceIdx)
  const arg = spaceIdx === -1 ? '' : trimmed.slice(spaceIdx + 1)

  switch (cmd) {
    case 'nav':
      await page.goto(arg, { waitUntil: 'domcontentloaded' })
      console.log(`[nav] ${arg}`)
      break

    case 'wait-for':
      if (arg.startsWith('text=')) {
        await page
          .getByText(arg.slice('text='.length))
          .first()
          .waitFor({ timeout: 15000 })
      } else if (arg.startsWith('url=')) {
        await page.waitForURL(arg.slice('url='.length), { timeout: 15000 })
      } else {
        await page.locator(arg).first().waitFor({ timeout: 15000 })
      }
      console.log(`[wait-for] ${arg} OK`)
      break

    case 'click':
      await resolveLocator(arg).first().click()
      console.log(`[click] ${arg}`)
      break

    case 'hover':
      await resolveLocator(arg).first().hover()
      console.log(`[hover] ${arg}`)
      break

    case 'fill': {
      const sep = arg.indexOf(' ')
      const selector = arg.slice(0, sep)
      const value = arg.slice(sep + 1)
      await resolveLocator(selector).first().fill(value)
      console.log(`[fill] ${selector} <- "${value}"`)
      break
    }

    case 'press':
      await page.keyboard.press(arg)
      console.log(`[press] ${arg}`)
      break

    case 'login-as': {
      // Shortcut for protected screens: skips the login form, calls the API
      // directly and seeds the same localStorage key AuthContext reads on
      // load (see src/features/auth/AuthContext.tsx, TOKEN_STORAGE_KEY).
      // The page must already be on the frontend's origin (nav there first)
      // — localStorage is per-origin, `about:blank` won't persist it.
      const [email, password] = arg.split(' ')
      await page.evaluate(
        async ({ email, password }) => {
          const res = await fetch('http://localhost:8080/api/v1/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password }),
          })
          if (!res.ok) throw new Error(`login-as: backend respondió ${res.status}`)
          const data = await res.json()
          localStorage.setItem('valuacion.token', data.token)
        },
        { email, password },
      )
      console.log(`[login-as] ${email}`)
      break
    }

    case 'set-token': {
      // For roles with no public registration (ADMIN — see RolUsuario's javadoc, "no hay alta
      // pública de ADMIN en el MVP"), login-as has no real account to call. Seeds the same
      // localStorage key directly with an already-minted JWT instead. The page must already be
      // on the frontend's origin. Mint one locally with the dev secret from
      // application.properties (app.jwt.secret's default) — see the "Testing as ADMIN" section
      // in SKILL.md for the exact script.
      await page.evaluate((token) => {
        localStorage.setItem('valuacion.token', token)
      }, arg)
      console.log('[set-token] localStorage seeded')
      break
    }

    case 'text': {
      const content = await resolveLocator(arg).first().textContent()
      console.log(`[text] ${arg} -> ${content}`)
      break
    }

    case 'screenshot': {
      shotCount += 1
      const name = arg || `shot-${String(shotCount).padStart(2, '0')}`
      const path = join(SCREENSHOT_DIR, `${name}.png`)
      await page.screenshot({ path })
      console.log(`[screenshot] ${path}`)
      break
    }

    case 'screenshot-element': {
      // Crops to one element — use when the diff is in a specific component (e.g. a shadow/hover
      // effect that's too subtle to eyeball in a full-page shot), not the whole page.
      const sep = arg.indexOf(' ')
      const selector = sep === -1 ? arg : arg.slice(0, sep)
      const name = sep === -1 ? undefined : arg.slice(sep + 1)
      shotCount += 1
      const finalName = name || `shot-${String(shotCount).padStart(2, '0')}`
      const path = join(SCREENSHOT_DIR, `${finalName}.png`)
      await resolveLocator(selector).first().screenshot({ path })
      console.log(`[screenshot-element] ${selector} -> ${path}`)
      break
    }

    case 'console':
      console.log(
        consoleErrors.length === 0
          ? '[console] no errors captured'
          : `[console] ${consoleErrors.length} error(s):\n  ${consoleErrors.join('\n  ')}`,
      )
      break

    case 'quit':
      await browser.close()
      process.exit(0)
      break

    default:
      console.log(`[error] comando desconocido: ${cmd}`)
  }
}

const rl = createInterface({ input: process.stdin })
for await (const line of rl) {
  try {
    await handle(line)
  } catch (err) {
    console.error(`[error] ${err instanceof Error ? err.message : String(err)}`)
  }
}
await browser.close().catch(() => {})
process.exit(0)
