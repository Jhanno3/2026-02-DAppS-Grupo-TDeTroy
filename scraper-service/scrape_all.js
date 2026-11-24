const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const LIGAS = [
  {
    nombre: 'Premier League',
    url: 'https://www.whoscored.com/regions/252/tournaments/2/seasons/11141/stages/25544/playerstatistics/england-premier-league-2026-2027'
  },
  {
    nombre: 'La Liga',
    url: 'https://www.whoscored.com/regions/206/tournaments/4/seasons/11213/stages/25662/playerstatistics/spain-laliga-2026-2027'
  },
  {
    nombre: 'Serie A',
    url: 'https://www.whoscored.com/regions/108/tournaments/5/seasons/11126/stages/25518/playerstatistics/italy-serie-a-2026-2027'
  },
  {
    nombre: 'Bundesliga',
    url: 'https://www.whoscored.com/regions/81/tournaments/3/seasons/11217/stages/25666/playerstatistics/germany-bundesliga-2026-2027'
  },
  {
    nombre: 'Ligue 1',
    url: 'https://www.whoscored.com/regions/74/tournaments/22/seasons/11150/stages/25554/playerstatistics/france-ligue-1-2026-2027'
  }
];

function mapearPosicion(textoPosicion) {
  if (!textoPosicion) return 'Centrocampista';
  const p = textoPosicion.toUpperCase();
  if (p.includes('GK')) return 'Arquero';
  if (p.includes('D(') || p.includes('D,') || p.startsWith('D ') || p === 'D') return 'Defensor';
  if (p.includes('FW') || p.includes('F(') || p.includes('F,') || p.includes('S')) return 'Delantero';
  if (p.includes('M(') || p.includes('AM') || p.includes('DM') || p.includes('M,') || p === 'M') return 'Centrocampista';
  return 'Centrocampista';
}

const PAISES = {
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

function mapearPais(claseBandera) {
  if (!claseBandera) return 'Internacional';
  const coincidencia = claseBandera.match(/flg-([a-z0-9-]+)/i);
  if (!coincidencia) return 'Internacional';
  const codigo = coincidencia[1].toLowerCase();
  return PAISES[codigo] || 'Internacional';
}

async function extraerTodo() {
  console.log('=== Iniciando extracción masiva de 5 Grandes Ligas (>= 100 jugadores por liga) ===');
  const navegador = await chromium.launch({
    headless: true,
    args: ['--disable-blink-features=AutomationControlled', '--no-sandbox', '--disable-setuid-sandbox']
  });

  const contexto = await navegador.newContext({
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    viewport: { width: 1366, height: 768 }
  });

  const resultados = [];

  for (const liga of LIGAS) {
    console.log(`\n========================================`);
    console.log(`[LIGA] Extrayendo: ${liga.nombre}`);
    console.log(`========================================`);
    const pagina = await contexto.newPage();
    await pagina.addInitScript(() => {
      delete Object.getPrototypeOf(navigator).webdriver;
    });

    try {
      console.log(`Navegando a ${liga.url}...`);
      await pagina.goto(liga.url, { waitUntil: 'domcontentloaded', timeout: 45000 });
      await pagina.waitForTimeout(4000);

      // Esperar a que la tabla cargue
      try {
        await pagina.waitForSelector('tbody#player-table-statistics-body > tr', { timeout: 20000 });
      } catch (e) {
        console.log(`Esperando 5s extra por la tabla de ${liga.nombre}...`);
        await pagina.waitForTimeout(5000);
      }

      // Clickea "All players" si está disponible
      try {
        await pagina.evaluate(() => {
          const enlaces = Array.from(document.querySelectorAll('a, button'));
          const todos = enlaces.find(el => el.textContent && el.textContent.includes('All players'));
          if (todos) todos.click();
        });
        await pagina.waitForTimeout(2000);
      } catch (e) {
        console.log('Continuando sin clickear All players...');
      }

      const jugadoresLiga = [];
      const MAX_PAGINAS = 11; // 11 páginas * 10 = 110 jugadores por liga (garantiza >= 100)

      for (let p = 0; p < MAX_PAGINAS; p++) {
        // Asegurar que las filas de la tabla existen
        try {
          await pagina.waitForSelector('tbody#player-table-statistics-body > tr', { timeout: 10000 });
        } catch (e) {
          console.warn(`Timeout esperando filas en pág ${p + 1}`);
        }

        const filas = await pagina.$$eval('tbody#player-table-statistics-body > tr', (trs) => {
          return trs.map(tr => {
            const celda = tr.querySelector('td.grid-ghost-cell');
            const enlace = celda ? celda.querySelector('a.player-link') : null;
            const nombre = enlace ? enlace.textContent.trim() : '';
            const href = enlace ? enlace.getAttribute('href') : '';
            const coincidenciaId = href ? href.match(/\/players\/(\d+)\//) : null;
            const playerId = coincidenciaId ? coincidenciaId[1] : null;

            const spanBandera = celda ? celda.querySelector('span.country') : null;
            const claseBandera = spanBandera ? spanBandera.className : '';

            const spanClub = celda ? celda.querySelector('span.team-name') : null;
            const club = spanClub ? spanClub.textContent.replace(/,\s*$/, '').trim() : '';

            // Metas: edad y posiciones
            const spansMeta = celda ? Array.from(celda.querySelectorAll('span.player-meta-data')) : [];
            let edad = 24;
            let posicionCruda = '';
            for (const sp of spansMeta) {
              const texto = sp.textContent.replace(/,\s*/g, '').trim();
              if (/^\d+$/.test(texto)) {
                edad = parseInt(texto, 10);
              } else if (texto.length > 0 && !texto.includes(club)) {
                posicionCruda = texto;
              }
            }

            const tds = tr.querySelectorAll('td');
            const partidos = tds.length > 2 ? tds[2].textContent.trim() : '0';
            const minutos = tr.querySelector('td.minsPlayed') ? tr.querySelector('td.minsPlayed').textContent.trim() : '0';
            const goles = tr.querySelector('td.goal') ? tr.querySelector('td.goal').textContent.trim() : '0';
            const asistencias = tr.querySelector('td.assistTotal') ? tr.querySelector('td.assistTotal').textContent.trim() : '0';
            const amarillas = tr.querySelector('td.yellowCard') ? tr.querySelector('td.yellowCard').textContent.trim() : '0';
            const rojas = tr.querySelector('td.redCard') ? tr.querySelector('td.redCard').textContent.trim() : '0';
            const motm = tr.querySelector('td.manOfTheMatch') ? tr.querySelector('td.manOfTheMatch').textContent.trim() : '0';
            const rating = tr.querySelector('td.rating') ? tr.querySelector('td.rating').textContent.trim() : '6.00';

            return {
              nombre,
              playerId,
              claseBandera,
              club,
              edad,
              posicionCruda,
              partidos,
              minutos,
              goles,
              asistencias,
              amarillas,
              rojas,
              motm,
              rating
            };
          });
        });

        for (const r of filas) {
          if (!r.nombre) continue;
          // Evitar duplicados dentro de la misma liga
          if (jugadoresLiga.some(lp => lp.nombre === r.nombre && lp.club === r.club)) continue;

          jugadoresLiga.push({
            nombre: r.nombre,
            playerId: r.playerId,
            club: r.club || `${liga.nombre} Club`,
            liga: liga.nombre,
            posicion: mapearPosicion(r.posicionCruda),
            nacionalidad: mapearPais(r.claseBandera),
            edad: r.edad || 24,
            fotoUrl: r.playerId ? `https://d2zywfiolv4f83.cloudfront.net/img/players/${r.playerId}.jpg` : null,
            rating: parseFloat(r.rating) || 6.50,
            metricas: {
              apps: parseInt(r.partidos.replace(/\(\d+\)/, ''), 10) || 0,
              minsPlayed: parseInt(r.minutos, 10) || 0,
              goals: parseInt(r.goles === '-' ? '0' : r.goles, 10) || 0,
              assists: parseInt(r.asistencias === '-' ? '0' : r.asistencias, 10) || 0,
              yellowCard: parseInt(r.amarillas === '-' ? '0' : r.amarillas, 10) || 0,
              redCard: parseInt(r.rojas === '-' ? '0' : r.rojas, 10) || 0,
              manOfTheMatch: parseInt(r.motm === '-' ? '0' : r.motm, 10) || 0
            }
          });
        }

        console.log(`[${liga.nombre}] Pagina ${p + 1}: ${jugadoresLiga.length} jugadores acumulados`);

        if (p < MAX_PAGINAS - 1) {
          // Click en next via DOM evaluate para evitar bloqueo por loading-spinner
          const haySiguiente = await pagina.evaluate(() => {
            const botonSiguiente = document.querySelector('a#next') || document.querySelector('a.next');
            if (!botonSiguiente) return false;
            if (botonSiguiente.classList.contains('disabled') || botonSiguiente.getAttribute('aria-disabled') === 'true') return false;
            botonSiguiente.click();
            return true;
          });

          if (!haySiguiente) {
            console.log(`[${liga.nombre}] No hay más páginas después de la página ${p + 1}`);
            break;
          }

          // Esperar actualización del DOM
          await pagina.waitForTimeout(2000);
        }
      }

      console.log(`>>> Exito: ${jugadoresLiga.length} jugadores extraídos para ${liga.nombre}`);
      resultados.push(...jugadoresLiga);
    } catch (err) {
      console.error(`Error procesando ${liga.nombre}:`, err.message);
    } finally {
      await pagina.close();
    }
  }

  await navegador.close();

  console.log(`\n======================================================`);
  console.log(`TOTAL GENERAL: ${resultados.length} jugadores de las 5 Grandes Ligas`);
  console.log(`======================================================`);

  const rutaSalida = path.join(__dirname, 'todos_los_jugadores.json');
  fs.writeFileSync(rutaSalida, JSON.stringify(resultados, null, 2), 'utf-8');
  console.log(`Archivo persistido en: ${rutaSalida}`);
}

extraerTodo().catch(err => {
  console.error('Fallo general en extraerTodo:', err);
  process.exit(1);
});
