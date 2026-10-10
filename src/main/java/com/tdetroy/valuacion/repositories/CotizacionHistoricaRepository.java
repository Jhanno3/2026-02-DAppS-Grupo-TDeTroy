package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link CotizacionHistoricaEntity} (plan.md S2.4).
 *
 * <p>{@link #findTopByJugadorIdOrderByFechaCalculoDesc} (T4.3) resuelve la "cotizacion
 * vigente"/"cotizacion anterior" de un jugador -- por definicion (plan.md S2.4), el registro con
 * {@code fechaCalculo} mas reciente para ese {@code jugadorId}; nunca una columna separada que
 * pueda desincronizarse.
 *
 * <p>{@link #findByJugadorIdOrderByFechaCalculoAsc} (T4.7) trae el historial cronologico completo
 * para {@code GET /jugadores/{id}/cotizaciones} (UC-08) -- incluye jugadores dados de baja: su
 * cotizacion queda congelada en el ultimo registro, pero el historial hasta ese punto sigue siendo
 * consultable.
 */
public interface CotizacionHistoricaRepository
        extends JpaRepository<CotizacionHistoricaEntity, UUID> {

    Optional<CotizacionHistoricaEntity> findTopByJugadorIdOrderByFechaCalculoDesc(UUID jugadorId);

    List<CotizacionHistoricaEntity> findByJugadorIdOrderByFechaCalculoAsc(UUID jugadorId);
}
