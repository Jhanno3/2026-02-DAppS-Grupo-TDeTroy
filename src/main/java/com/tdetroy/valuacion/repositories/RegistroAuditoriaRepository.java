package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.RegistroAuditoriaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link RegistroAuditoriaEntity} (plan.md §2.8/§10). Sin métodos
 * de consulta propios: no hay endpoint público en el MVP, se escribe siempre a través de {@code
 * AuditoriaService} y se consulta directamente en base (plan.md §10).
 */
public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoriaEntity, UUID> {}
