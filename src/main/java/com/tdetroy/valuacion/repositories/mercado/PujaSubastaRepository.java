package com.tdetroy.valuacion.repositories.mercado;

import com.tdetroy.valuacion.entity.mercado.PujaSubastaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data JPA sobre {@link PujaSubastaEntity} (UC-18). */
public interface PujaSubastaRepository extends JpaRepository<PujaSubastaEntity, UUID> {

    List<PujaSubastaEntity> findBySubastaIdOrderByFechaDesc(UUID subastaId);
}
