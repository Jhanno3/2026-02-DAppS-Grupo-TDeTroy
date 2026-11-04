const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const LEAGUES = [
  {
    name: 'Premier League',
    url: 'https://www.whoscored.com/regions/252/tournaments/2/seasons/11141/stages/25544/playerstatistics/england-premier-league-2026-2027'
  },
  {
    name: 'La Liga',
    url: 'https://www.whoscored.com/regions/206/tournaments/4/seasons/11213/stages/25662/playerstatistics/spain-laliga-2026-2027'
  },
  {
    name: 'Serie A',
    url: 'https://www.whoscored.com/regions/108/tournaments/5/seasons/11126/stages/25518/playerstatistics/italy-serie-a-2026-2027'
  },
  {
    name: 'Bundesliga',
    url: 'https://www.whoscored.com/regions/81/tournaments/3/seasons/11217/stages/25666/playerstatistics/germany-bundesliga-2026-2027'
  },
  {
    name: 'Ligue 1',
    url: 'https://www.whoscored.com/regions/74/tournaments/22/seasons/11150/stages/25554/playerstatistics/france-ligue-1-2026-2027'
  }
];

function mapPosition(posStr) {
  if (!posStr) return 'Centrocampista';
  const p = posStr.toUpperCase();
  if (p.includes('GK')) return 'Arquero';
  if (p.includes('D(') || p.includes('D,') || p.startsWith('D ') || p === 'D') return 'Defensor';
  if (p.includes('FW') || p.includes('F(') || p.includes('F,') || p.includes('S')) return 'Delantero';
  if (p.includes('M(') || p.includes('AM') || p.includes('DM') || p.includes('M,') || p === 'M') return 'Centrocampista';
  return 'Centrocampista';
}

const COUNTRY_MAP = {
  'br': 'Brasil', 'ar': 'Argentina', 'es': 'España', 'fr': 'Francia', 'de': 'Alemania',
  'it': 'Italia', 'gb-eng': 'Inglaterra', 'eng': 'Inglaterra', 'pt': 'Portugal', 'nl': 'Países Bajos',
  'be': 'Bélgica', 'uy': 'Uruguay', 'co': 'Colombia', 'no': 'Noruega', 'se': 'Suecia',
  'dk': 'Dinamarca', 'hr': 'Croacia', 'pl': 'Polonia', 'at': 'Austria', 'ch': 'Suiza',
  'cm': 'Camerún', 'ng': 'Nigeria', 'sn': 'Senegal', 'ci': 'Costa de Marfil', 'gh': 'Ghana',
  'ma': 'Marruecos', 'dz': 'Argelia', 'eg': 'Egipto', 'jp': 'Japón', 'kr': 'Corea del Sur',
  'us': 'Estados Unidos', 'ca': 'Canadá', 'mx': 'México', 'cl': 'Chile', 'ec': 'Ecuador',
  'py': 'Paraguay', 'pe': 'Perú', 've': 'Venezuela', 'rs': 'Serbia', 'cz': 'República Checa',
  'sk': 'Eslovaquia', 'ua': 'Ucrania', 'tr': 'Turquía', 'gr': 'Grecia', 'ro': 'Rumania',
  'hu': 'Hungría', 'scot': 'Escocia', 'gb-sct': 'Escocia', 'wal': 'Gales', 'gb-wls': 'Gales',
  'nir': 'Irlanda del Norte', 'ie': 'Irlanda', 'ge': 'Georgia', 'ba': 'Bosnia'
};

function mapCountry(flagClass) {
  if (!flagClass) return 'Internacional';
  const match = flagClass.match(/flg-([a-z0-9-]+)/i);
  if (!match) return 'Internacional';
  const code = match[1].toLowerCase();
  return COUNTRY_MAP[code] || 'Internacional';
}

async function scrapeAll() {
  console.log('=== Iniciando extracción masiva de 5 Grandes Ligas (>= 100 jugadores por liga) ===');
  const browser = await chromium.launch({
    headless: true,
    args: ['--disable-blink-features=AutomationControlled', '--no-sandbox', '--disable-setuid-sandbox']
  });

  const context = await browser.newContext({
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    viewport: { width: 1366, height: 768 }
  });

  const allResults = [];

  for (const league of LEAGUES) {
    console.log(`\n========================================`);
    console.log(`[LIGA] Extrayendo: ${league.name}`);
    console.log(`========================================`);
    const page = await context.newPage();
    await page.addInitScript(() => {
      delete Object.getPrototypeOf(navigator).webdriver;
    });

    try {
      console.log(`Navegando a ${league.url}...`);
      await page.goto(league.url, { waitUntil: 'domcontentloaded', timeout: 45000 });
      await page.waitForTimeout(4000);

      // Esperar a que la tabla cargue
      try {
        await page.waitForSelector('tbody#player-table-statistics-body > tr', { timeout: 20000 });
      } catch (e) {
        console.log(`Esperando 5s extra por la tabla de ${league.name}...`);
        await page.waitForTimeout(5000);
      }

      // Clickea "All players" si está disponible
      try {
        await page.evaluate(() => {
          const links = Array.from(document.querySelectorAll('a, button'));
          const allPlayers = links.find(el => el.textContent && el.textContent.includes('All players'));
          if (allPlayers) allPlayers.click();
        });
        await page.waitForTimeout(2000);
      } catch (e) {
        console.log('Continuando sin clickear All players...');
      }

      const leaguePlayers = [];
      const MAX_PAGES = 11; // 11 páginas * 10 = 110 jugadores por liga (garantiza >= 100)

      for (let p = 0; p < MAX_PAGES; p++) {
        // Asegurar que las filas de la tabla existen
        try {
          await page.waitForSelector('tbody#player-table-statistics-body > tr', { timeout: 10000 });
        } catch (e) {
          console.warn(`Timeout esperando filas en pág ${p + 1}`);
        }

        const rows = await page.$$eval('tbody#player-table-statistics-body > tr', (trs) => {
          return trs.map(tr => {
            const ghost = tr.querySelector('td.grid-ghost-cell');
            const link = ghost ? ghost.querySelector('a.player-link') : null;
            const nombre = link ? link.textContent.trim() : '';
            const href = link ? link.getAttribute('href') : '';
            const idMatch = href ? href.match(/\/players\/(\d+)\//) : null;
            const playerId = idMatch ? idMatch[1] : null;

            const flagSpan = ghost ? ghost.querySelector('span.country') : null;
            const flagClass = flagSpan ? flagSpan.className : '';

            const teamSpan = ghost ? ghost.querySelector('span.team-name') : null;
            const club = teamSpan ? teamSpan.textContent.replace(/,\s*$/, '').trim() : '';

            // Metas: edad y posiciones
            const metaSpans = ghost ? Array.from(ghost.querySelectorAll('span.player-meta-data')) : [];
            let edad = 24;
            let rawPos = '';
            for (const sp of metaSpans) {
              const text = sp.textContent.replace(/,\s*/g, '').trim();
              if (/^\d+$/.test(text)) {
                edad = parseInt(text, 10);
              } else if (text.length > 0 && !text.includes(club)) {
                rawPos = text;
              }
            }

            const tds = tr.querySelectorAll('td');
            const apps = tds.length > 2 ? tds[2].textContent.trim() : '0';
            const mins = tr.querySelector('td.minsPlayed') ? tr.querySelector('td.minsPlayed').textContent.trim() : '0';
            const goals = tr.querySelector('td.goal') ? tr.querySelector('td.goal').textContent.trim() : '0';
            const assists = tr.querySelector('td.assistTotal') ? tr.querySelector('td.assistTotal').textContent.trim() : '0';
            const yellow = tr.querySelector('td.yellowCard') ? tr.querySelector('td.yellowCard').textContent.trim() : '0';
            const red = tr.querySelector('td.redCard') ? tr.querySelector('td.redCard').textContent.trim() : '0';
            const motm = tr.querySelector('td.manOfTheMatch') ? tr.querySelector('td.manOfTheMatch').textContent.trim() : '0';
            const rating = tr.querySelector('td.rating') ? tr.querySelector('td.rating').textContent.trim() : '6.00';

            return {
              nombre,
              playerId,
              flagClass,
              club,
              edad,
              rawPos,
              apps,
              mins,
              goals,
              assists,
              yellow,
              red,
              motm,
              rating
            };
          });
        });

        for (const r of rows) {
          if (!r.nombre) continue;
          // Evitar duplicados dentro de la misma liga
          if (leaguePlayers.some(lp => lp.nombre === r.nombre && lp.club === r.club)) continue;

          leaguePlayers.push({
            nombre: r.nombre,
            playerId: r.playerId,
            club: r.club || `${league.name} Club`,
            liga: league.name,
            posicion: mapPosition(r.rawPos),
            nacionalidad: mapCountry(r.flagClass),
            edad: r.edad || 24,
            fotoUrl: r.playerId ? `https://d2zywfiolv4f83.cloudfront.net/img/players/${r.playerId}.jpg` : null,
            rating: parseFloat(r.rating) || 6.50,
            metricas: {
              apps: parseInt(r.apps.replace(/\(\d+\)/, ''), 10) || 0,
              minsPlayed: parseInt(r.mins, 10) || 0,
              goals: parseInt(r.goals === '-' ? '0' : r.goals, 10) || 0,
              assists: parseInt(r.assists === '-' ? '0' : r.assists, 10) || 0,
              yellowCard: parseInt(r.yellow === '-' ? '0' : r.yellow, 10) || 0,
              redCard: parseInt(r.red === '-' ? '0' : r.red, 10) || 0,
              manOfTheMatch: parseInt(r.motm === '-' ? '0' : r.motm, 10) || 0
            }
          });
        }

        console.log(`[${league.name}] Pagina ${p + 1}: ${leaguePlayers.length} jugadores acumulados`);

        if (p < MAX_PAGES - 1) {
          // Click en next via DOM evaluate para evitar bloqueo por loading-spinner
          const canGoNext = await page.evaluate(() => {
            const nextBtn = document.querySelector('a#next') || document.querySelector('a.next');
            if (!nextBtn) return false;
            if (nextBtn.classList.contains('disabled') || nextBtn.getAttribute('aria-disabled') === 'true') return false;
            nextBtn.click();
            return true;
          });

          if (!canGoNext) {
            console.log(`[${league.name}] No hay más páginas después de la página ${p + 1}`);
            break;
          }

          // Esperar actualización del DOM
          await page.waitForTimeout(2000);
        }
      }

      console.log(`>>> Exito: ${leaguePlayers.length} jugadores extraídos para ${league.name}`);
      allResults.push(...leaguePlayers);
    } catch (err) {
      console.error(`Error procesando ${league.name}:`, err.message);
    } finally {
      await page.close();
    }
  }

  await browser.close();

  console.log(`\n======================================================`);
  console.log(`TOTAL GENERAL: ${allResults.length} jugadores de las 5 Grandes Ligas`);
  console.log(`======================================================`);

  const outputPath = path.join(__dirname, 'todos_los_jugadores.json');
  fs.writeFileSync(outputPath, JSON.stringify(allResults, null, 2), 'utf-8');
  console.log(`Archivo persistido en: ${outputPath}`);
}

scrapeAll().catch(err => {
  console.error('Fallo general en scrapeAll:', err);
  process.exit(1);
});
