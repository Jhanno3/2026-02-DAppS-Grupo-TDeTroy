const { AsyncLocalStorage } = require('node:async_hooks');
const { randomUUID } = require('node:crypto');
const express = require('express');
const { chromium } = require('playwright');

const app = express();
const PORT = process.env.PORT || 3001;
let navegador = null;

const contextoRequest = new AsyncLocalStorage();

// Log en JSON con el id que manda el backend, para seguir una ingesta entre los dos servicios.
function registrar(nivel, mensaje) {
  console.log(
    JSON.stringify({
      '@timestamp': new Date().toISOString(),
      log: { level: nivel },
      service: { name: 'scraper-service' },
      message: mensaje,
      correlationId: contextoRequest.getStore()
    })
  );
}

app.use((req, res, next) => {
  const recibido = req.get('X-Correlation-Id') || '';
  const id = /^[A-Za-z0-9._-]{1,64}$/.test(recibido) ? recibido : randomUUID();
  res.setHeader('X-Correlation-Id', id);
  contextoRequest.run(id, next);
});

async function obtenerNavegador() {
  if (!navegador) {
    navegador = await chromium.launch({
      headless: true,
      args: [
        '--disable-blink-features=AutomationControlled',
        '--no-sandbox',
        '--disable-setuid-sandbox'
      ]
    });
  }
  return navegador;
}

app.get('/health', (req, res) => res.status(200).send('OK'));

app.get('/fetch', async (req, res) => {
  const urlDestino = req.query.url;
  if (!urlDestino) {
    return res.status(400).send('Falta el parámetro "url"');
  }

  let contexto = null;
  try {
    const b = await obtenerNavegador();
    contexto = await b.newContext({
      userAgent:
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
      viewport: { width: 1366, height: 768 }
    });

    const pagina = await contexto.newPage();

    // Oculta la variable webdriver para evitar detección
    await pagina.addInitScript(() => {
      delete Object.getPrototypeOf(navigator).webdriver;
    });

    registrar('INFO', `Navegando a: ${urlDestino}`);
    await pagina.goto(urlDestino, { waitUntil: 'domcontentloaded', timeout: 35000 });

    // Espera breve para asegurar que Cloudflare resuelva el desafío
    await pagina.waitForTimeout(3000);

    const html = await pagina.content();
    await contexto.close();

    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    return res.status(200).send(html);
  } catch (err) {
    if (contexto) await contexto.close();
    registrar('ERROR', `Falló extracción de ${urlDestino}: ${err.message}`);
    return res.status(502).send(`Error al consultar WhoScored: ${err.message}`);
  }
});

// Tope de seguridad de paginas por corrida: evita un loop infinito si "next" nunca queda
// deshabilitado (ej. cambio de estructura del sitio) -- 50 paginas de a 10 jugadores cubre
// comodamente cualquier liga real (~500 jugadores).
const TOPE_PAGINAS = 50;
const ESPERA_CLICK_MS = 5000;

// Intenta clickear un control por su texto visible (mas resistente a cambios de id/clase que un
// selector CSS exacto, ver WhoScoredLigaRepositoryImpl). Nunca lanza: si no lo encuentra o no es
// clickeable, devuelve false y quien llama decide como seguir.
async function clickearPorTexto(pagina, texto, opciones = {}) {
  try {
    const control = pagina.getByText(texto, { exact: false }).first();
    await control.click({ timeout: ESPERA_CLICK_MS, ...opciones });
    return true;
  } catch (err) {
    return false;
  }
}

app.get('/fetch-liga', async (req, res) => {
  const urlDestino = req.query.url;
  if (!urlDestino) {
    return res.status(400).send('Falta el parámetro "url"');
  }

  let contexto = null;
  const paginas = [];
  try {
    const b = await obtenerNavegador();
    contexto = await b.newContext({
      userAgent:
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
      viewport: { width: 1366, height: 768 }
    });
    const pagina = await contexto.newPage();
    await pagina.addInitScript(() => {
      delete Object.getPrototypeOf(navigator).webdriver;
    });

    registrar('INFO', `Navegando a liga: ${urlDestino}`);
    await pagina.goto(urlDestino, { waitUntil: 'domcontentloaded', timeout: 35000 });
    await pagina.waitForTimeout(3000);

    // Filtro "All players": sin el, la tabla puede venir separada por posicion (arqueros aparte).
    // Si no se encuentra, se continua con lo que la pagina trae por defecto (constitution.md S1:
    // nunca bloquear el resto de la ingesta por un control puntual que cambio de lugar).
    const filtroAplicado = await clickearPorTexto(pagina, 'All players');
    if (filtroAplicado) {
      await pagina.waitForTimeout(1500);
    } else {
      registrar('WARN', 'No se encontro el filtro "All players"; se continua sin aplicarlo.');
    }

    for (let i = 0; i < TOPE_PAGINAS; i++) {
      paginas.push(await pagina.content());

      const siguiente = pagina.getByText('next', { exact: false }).first();
      const haySiguiente = (await siguiente.count()) > 0;
      if (!haySiguiente) {
        break;
      }
      const deshabilitado = await siguiente
        .evaluate((el) => el.classList.contains('disabled') || el.getAttribute('aria-disabled') === 'true')
        .catch(() => false);
      if (deshabilitado) {
        break;
      }
      const avanzo = await clickearPorTexto(pagina, 'next');
      if (!avanzo) {
        break;
      }
      await pagina.waitForTimeout(1500);
    }

    await contexto.close();
    return res.status(200).json({ pages: paginas });
  } catch (err) {
    if (contexto) await contexto.close();
    registrar('ERROR', `Falló la paginación de liga de ${urlDestino}: ${err.message}`);
    if (paginas.length > 0) {
      // Degradacion graciosa: devolver lo que ya se junto en vez de perder todo por una falla
      // a mitad de la paginacion (ej. "next" cambio de estructura en la pagina 6 de 10).
      return res.status(200).json({ pages: paginas });
    }
    return res.status(502).send(`Error al paginar liga de WhoScored: ${err.message}`);
  }
});

app.listen(PORT, '0.0.0.0', () => {
  registrar('INFO', `Scraper service escuchando en http://localhost:${PORT}`);
});