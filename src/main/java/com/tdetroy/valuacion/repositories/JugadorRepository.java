package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.model.EstadoJugador;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link JugadorEntity} (plan.md §2.2). {@link
 * #findByEstado(EstadoJugador)} es la primera consulta propia, agregada por {@code
 * RendimientoServiceImpl} (tasks.md T3.5) para resolver el universo de jugadores {@code ACTIVO}
 * sobre el que corre la ingesta semanal — mismo criterio que {@link MovimientoRepository} de no
 * anticipar una forma de consulta que todavía no se pidió.
 */
public interface JugadorRepository extends JpaRepository<JugadorEntity, UUID> {

    List<JugadorEntity> findByEstado(EstadoJugador estado);
}
