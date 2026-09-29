package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.model.RendimientoPartido;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA sobre {@link RendimientoPartido} (plan.md §2.3). Sin métodos de
 * consulta propios todavía: {@code RendimientoServiceImpl}/{@code CotizacionServiceImpl} (T3.5+)
 * agregan las consultas que efectivamente necesiten (ej. rendimiento de un jugador en la semana de
 * cálculo vigente), mismo criterio que {@link JugadorRepository} para no anticipar una forma de
 * consulta que todavía no se pidió.
 */
public interface RendimientoPartidoRepository extends JpaRepository<RendimientoPartido, UUID> {}
