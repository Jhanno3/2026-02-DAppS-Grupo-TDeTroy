const express = require('express');
const { chromium } = require('playwright');

const app = express();
const PORT = process.env.PORT || 3001;
let browser = null;

async function getBrowser() {
  if (!browser) {
    browser = await chromium.launch({
      headless: true,
      args: [
        '--disable-blink-features=AutomationControlled',
        '--no-sandbox',
        '--disable-setuid-sandbox'
      ]
    });
  }
  return browser;
}

app.get('/health', (req, res) => res.status(200).send('OK'));

app.get('/fetch', async (req, res) => {
  const targetUrl = req.query.url;
  if (!targetUrl) {
    return res.status(400).send('Falta el parámetro "url"');
  }

  let context = null;
  try {
    const b = await getBrowser();
    context = await b.newContext({
      userAgent:
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
      viewport: { width: 1366, height: 768 }
    });

    const page = await context.newPage();

    // Oculta la variable webdriver para evitar detección
    await page.addInitScript(() => {
      delete Object.getPrototypeOf(navigator).webdriver;
    });

    console.log(`[SCRAPER] Navegando a: ${targetUrl}`);
    await page.goto(targetUrl, { waitUntil: 'domcontentloaded', timeout: 35000 });

    // Espera breve para asegurar que Cloudflare resuelva el desafío
    await page.waitForTimeout(3000);

    const html = await page.content();
    await context.close();

    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    return res.status(200).send(html);
  } catch (err) {
    if (context) await context.close();
    console.error(`[ERROR] Falló extracción de ${targetUrl}:`, err.message);
    return res.status(502).send(`Error al consultar WhoScored: ${err.message}`);
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Scraper service escuchando en http://localhost:${PORT}`);
});