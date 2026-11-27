-- Demuestra el efecto del indice de V24 con volumen: carga datos sinteticos, mide la consulta
-- sin y con el indice, y deshace todo al final (ROLLBACK). Correr contra la base de test.
\timing off
BEGIN;

DROP INDEX IF EXISTS idx_cotizaciones_historicas_jugador_semana_fecha;

-- 200 jugadores x 104 semanas x 3 estrategias x 4 recalculos por semana = 249.600 filas.
INSERT INTO cotizaciones_historicas (id, jugador_id, semana, valor, origen, estrategia, fecha_calculo)
SELECT gen_random_uuid(),
       ('00000000-0000-0000-0000-' || lpad(j::text, 12, '0'))::uuid,
       to_char(DATE '2025-01-06' + (s * 7), 'IYYY-"W"IW'),
       100 + random() * 900,
       'CALCULADA',
       (ARRAY['RENDIMIENTO_TEMPORADA', 'RENDIMIENTO_HISTORICO', 'MERCADO'])[e],
       TIMESTAMPTZ '2025-01-06' + (s * 7 + r) * INTERVAL '1 day' + e * INTERVAL '1 minute'
FROM generate_series(1, 200) j, generate_series(0, 103) s, generate_series(1, 3) e,
     generate_series(0, 3) r;
ANALYZE cotizaciones_historicas;

\echo '=== SIN el indice: cotizacion del jugador 1 en su primera semana ==='
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM cotizaciones_historicas
WHERE jugador_id = '00000000-0000-0000-0000-000000000001' AND semana = '2025-W02'
ORDER BY fecha_calculo DESC LIMIT 1;

CREATE INDEX idx_cotizaciones_historicas_jugador_semana_fecha
    ON cotizaciones_historicas (jugador_id, semana, fecha_calculo DESC);
ANALYZE cotizaciones_historicas;

\echo '=== CON el indice ==='
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM cotizaciones_historicas
WHERE jugador_id = '00000000-0000-0000-0000-000000000001' AND semana = '2025-W02'
ORDER BY fecha_calculo DESC LIMIT 1;

ROLLBACK;
