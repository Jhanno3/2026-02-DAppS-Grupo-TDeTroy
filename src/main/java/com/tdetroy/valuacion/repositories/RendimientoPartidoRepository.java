package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.RendimientoPartidoEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link RendimientoPartidoEntity} (plan.md §2.3).
 *
 * <p>{@link #existsByJugadorIdAndPartidoExternoId} (T3.5) aplica la clave de deduplicación
 * documentada en la migración V7 ({@code jugador_id} + {@code partido_externo_id}) frente a una
 * reingesta del mismo ciclo semanal.
 *
 * <p>{@link #findByJugadorIdAndSemanaCalculoGreaterThanEqual} (T4.3) trae, en una sola consulta, el
 * rendimiento del jugador de la semana en curso y su ventana histórica reciente (plan.md §6.2:
 * normalización min-max "sobre el histórico reciente del propio jugador") — {@code semanaCalculo}
 * tiene formato fijo {@code yyyy-Www} (ver {@code RendimientoPartido}), así que la comparación
 * lexicográfica de {@code GreaterThanEqual} coincide exactamente con el orden cronológico real, sin
 * necesitar parsear la semana a una fecha.
 */
public interface RendimientoPartidoRepository
        extends JpaRepository<RendimientoPartidoEntity, UUID> {

    boolean existsByJugadorIdAndPartidoExternoId(UUID jugadorId, String partidoExternoId);

    List<RendimientoPartidoEntity> findByJugadorIdAndSemanaCalculoGreaterThanEqual(
            UUID jugadorId, String semanaCalculoDesde);
}
