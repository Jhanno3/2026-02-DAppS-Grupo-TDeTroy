-- Jugador.fotoUrl (plan.md S8.1): URL de la foto de perfil, obtenida por WhoScoredRepositoryImpl
-- durante la ingesta semanal de rendimiento (RendimientoServiceImpl, T3.5/T3.6) -- nunca
-- seteable por el alta/edicion de UC-03/UC-04. Nullable: queda NULL hasta el primer ciclo de
-- ingesta que la encuentre, mismo criterio que fecha_ultima_actualizacion_rendimiento
-- (V5__jugadores.sql).
ALTER TABLE jugadores ADD COLUMN foto_url VARCHAR(2048);
