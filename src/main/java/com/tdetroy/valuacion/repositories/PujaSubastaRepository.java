package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.PujaSubastaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data JPA sobre {@link PujaSubastaEntity} (UC-18). */
public interface PujaSubastaRepository extends JpaRepository<PujaSubastaEntity, UUID> {

    List<PujaSubastaEntity> findBySubastaIdOrderByFechaDesc(UUID subastaId);
}
