package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.PesoMetricaEntity;
import com.tdetroy.valuacion.entity.PesoMetricaId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data JPA sobre {@link PesoMetricaEntity}. */
public interface PesoMetricaRepository extends JpaRepository<PesoMetricaEntity, PesoMetricaId> {

    List<PesoMetricaEntity> findByActivoTrue();

    List<PesoMetricaEntity> findByPosicion(String posicion);

    List<PesoMetricaEntity> findByClave(String clave);

    Optional<PesoMetricaEntity> findByClaveAndPosicion(String clave, String posicion);
}
