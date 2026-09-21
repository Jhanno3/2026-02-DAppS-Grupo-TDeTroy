package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.model.Jugador;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link Jugador} (plan.md §2.2). Sin métodos de consulta propios
 * todavía: {@code JugadorServiceImpl} (T2.4) y el resto de los Services que lo consumen (T2.4+)
 * agregan las consultas que efectivamente necesiten (ej. listado del catálogo, sólo jugadores
 * {@code ACTIVO} para el job de recotización), mismo criterio que {@link MovimientoRepository} para
 * no anticipar una forma de consulta que todavía no se pidió.
 */
public interface JugadorRepository extends JpaRepository<Jugador, UUID> {}
