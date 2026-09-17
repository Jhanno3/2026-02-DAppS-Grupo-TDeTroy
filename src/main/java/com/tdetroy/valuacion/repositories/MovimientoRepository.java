package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.model.Movimiento;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link Movimiento} (plan.md §2.6). Building block
 * compartido, todavía sin consumidor propio — {@code MovimientoService} (T5.5) es quien agrega
 * los métodos de consulta que efectivamente necesita (ej. historial cronológico por usuario,
 * UC-12), para no anticipar una forma de consulta que ese Service todavía no pidió.
 */
public interface MovimientoRepository extends JpaRepository<Movimiento, UUID> {
}
