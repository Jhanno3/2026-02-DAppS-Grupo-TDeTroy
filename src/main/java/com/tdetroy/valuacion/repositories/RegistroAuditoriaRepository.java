package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.model.RegistroAuditoria;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link RegistroAuditoria} (plan.md §2.8/§10). Sin métodos de
 * consulta propios: no hay endpoint público en el MVP, se escribe siempre a través de {@code
 * AuditoriaService} y se consulta directamente en base (plan.md §10).
 */
public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, UUID> {}
