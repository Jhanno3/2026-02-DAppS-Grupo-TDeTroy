package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.PesoMetricaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link PesoMetricaEntity} (plan.md §2.9). {@code clave} (no
 * {@code UUID}) es la clave primaria natural — ver javadoc de {@link PesoMetricaEntity}. {@link
 * #findByActivoTrue()} (T4.3) es la primera consulta propia: {@code CotizacionServiceImpl} recorre
 * únicamente las métricas activas al calcular el puntaje ponderado de un jugador (plan.md §6.2).
 */
public interface PesoMetricaRepository extends JpaRepository<PesoMetricaEntity, String> {

    List<PesoMetricaEntity> findByActivoTrue();
}
