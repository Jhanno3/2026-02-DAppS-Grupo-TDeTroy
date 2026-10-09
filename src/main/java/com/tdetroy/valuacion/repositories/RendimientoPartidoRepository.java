package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.RendimientoPartidoEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link RendimientoPartidoEntity} (plan.md §2.3). {@link
 * #existsByJugadorIdAndPartidoExternoId} es la primera consulta propia, agregada por {@code
 * RendimientoServiceImpl} (tasks.md T3.5) para aplicar la clave de deduplicación documentada en la
 * migración V7 ({@code jugador_id} + {@code partido_externo_id}) frente a una reingesta del mismo
 * ciclo semanal. {@code CotizacionServiceImpl} (T4.3+) agrega, a su turno, las consultas que
 * efectivamente necesite (ej. rendimiento de un jugador en la semana de cálculo vigente), mismo
 * criterio que {@link JugadorRepository} para no anticipar una forma de consulta que todavía no se
 * pidió.
 */
public interface RendimientoPartidoRepository
        extends JpaRepository<RendimientoPartidoEntity, UUID> {

    boolean existsByJugadorIdAndPartidoExternoId(UUID jugadorId, String partidoExternoId);
}
