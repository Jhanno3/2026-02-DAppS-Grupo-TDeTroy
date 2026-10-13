package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.MovimientoEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link MovimientoEntity} (plan.md §2.6). {@link
 * #findByUsuarioIdOrderByFechaAsc} (T5.5) es la primera consulta propia, agregada por {@code
 * MovimientoServiceImpl} para el historial cronológico de un usuario (UC-12) — alcanza con filtrar
 * por {@code usuarioId}: una operación P2P (fase 6) persiste dos filas, una por cada participante
 * con su propio {@code usuarioId} (plan.md §9.2), así que esta misma consulta ya trae también las
 * operaciones donde el usuario fue la contraparte, sin un segundo filtro.
 */
public interface MovimientoRepository extends JpaRepository<MovimientoEntity, UUID> {

    List<MovimientoEntity> findByUsuarioIdOrderByFechaAsc(UUID usuarioId);
}
