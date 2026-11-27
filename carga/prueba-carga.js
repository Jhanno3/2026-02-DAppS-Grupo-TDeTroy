// Prueba de carga: verifica los SLAs de specs/plan.md. Ver carga/README.md.
import http from 'k6/http';
import { check, group, sleep } from 'k6';

const BASE = (__ENV.BASE_URL || 'http://host.docker.internal:8080') + '/api/v1';
const USUARIO = __ENV.USUARIO || 'user@example.com';
const PASSWORD = __ENV.PASSWORD || 'Password123!';

export const options = {
  scenarios: {
    navegacion: {
      executor: 'ramping-vus',
      stages: [
        { duration: '15s', target: Number(__ENV.VUS || 50) },
        { duration: __ENV.DURACION || '1m', target: Number(__ENV.VUS || 50) },
        { duration: '10s', target: 0 },
      ],
    },
  },
  // Los SLAs: si no se cumplen, k6 termina con error.
  thresholds: {
    'http_req_duration{tipo:consulta}': ['p(95)<300'],
    // Mismo SLA por endpoint, para ver cual se acerca al limite.
    'http_req_duration{name:ranking}': ['p(95)<300'],
    'http_req_duration{name:jugador}': ['p(95)<300'],
    'http_req_duration{name:historial}': ['p(95)<300'],
    'http_req_duration{name:cotizacion-semana}': ['p(95)<300'],
    'http_req_duration{name:portfolio}': ['p(95)<300'],
    'http_req_duration{tipo:login}': ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

const consulta = (nombre) => ({ tags: { tipo: 'consulta', name: nombre } });

export function setup() {
  const login = http.post(
    BASE + '/auth/login',
    JSON.stringify({ email: USUARIO, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { tipo: 'login', name: 'login' } },
  );
  check(login, { 'login 200': (r) => r.status === 200 });
  const jugadores = http.get(BASE + '/jugadores', consulta('jugadores')).json();
  if (!Array.isArray(jugadores) || jugadores.length === 0) {
    throw new Error('La base no tiene jugadores: cargar datos antes de la prueba');
  }
  const conSemanas = jugadores.slice(0, 20).map((j) => {
    const historial = http.get(BASE + '/jugadores/' + j.id + '/cotizaciones', consulta('historial')).json();
    return { id: j.id, semanas: (historial || []).map((c) => c.semana) };
  });
  return { token: login.json('token'), jugadores: conSemanas };
}

const azar = (lista) => lista[Math.floor(Math.random() * lista.length)];

export default function (datos) {
  const jugador = azar(datos.jugadores);
  const autenticado = { ...consulta('portfolio'), headers: { Authorization: 'Bearer ' + datos.token } };

  group('navegacion', () => {
    check(http.get(BASE + '/jugadores/ranking', consulta('ranking')), { 'ranking 200': (r) => r.status === 200 });
    check(http.get(BASE + '/jugadores/' + jugador.id, consulta('jugador')), { 'jugador 200': (r) => r.status === 200 });
    check(http.get(BASE + '/jugadores/' + jugador.id + '/cotizaciones', consulta('historial')), {
      'historial 200': (r) => r.status === 200,
    });
    if (jugador.semanas.length > 0) {
      const semana = azar(jugador.semanas);
      check(http.get(BASE + '/jugadores/' + jugador.id + '/cotizaciones/' + semana, consulta('cotizacion-semana')), {
        'cotizacion de semana 200': (r) => r.status === 200,
      });
    }
    check(http.get(BASE + '/portfolios/me', autenticado), { 'portfolio 200': (r) => r.status === 200 });
  });

  // Login real, pero poco frecuente: BCrypt es caro a proposito.
  if (Math.random() < 0.05) {
    const r = http.post(
      BASE + '/auth/login',
      JSON.stringify({ email: USUARIO, password: PASSWORD }),
      { headers: { 'Content-Type': 'application/json' }, tags: { tipo: 'login', name: 'login' } },
    );
    check(r, { 'login 200': (res) => res.status === 200 });
  }

  sleep(1);
}
