package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.SubastaEntity;
import com.tdetroy.valuacion.model.EstadoSubasta;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Repositorio Spring Data JPA sobre {@link SubastaEntity} (UC-18). {@link #findById} toma {@code
 * PESSIMISTIC_WRITE} (mismo criterio que {@code OfertaP2PRepository}): dos pujas simultáneas sobre
 * la misma subasta se procesan de a una, y la segunda se calcula sobre el precio ya actualizado.
 */
public interface SubastaRepository extends JpaRepository<SubastaEntity, UUID> {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SubastaEntity> findById(UUID id);

    /** Lectura sin lock, para el detalle público (nunca para mutar). */
    Optional<SubastaEntity> findOneById(UUID id);

    List<SubastaEntity> findByEstadoOrderByFechaCreacionDesc(EstadoSubasta estado);

    List<SubastaEntity> findByJugadorIdAndEstadoOrderByFechaCreacionDesc(
            UUID jugadorId, EstadoSubasta estado);

    List<SubastaEntity> findByMejorPostorIdAndEstado(UUID mejorPostorId, EstadoSubasta estado);
}
