package com.tdetroy.valuacion.repositories.jugador;

import com.tdetroy.valuacion.entity.jugador.JugadorEntity;
import com.tdetroy.valuacion.model.jugador.EstadoJugador;
import com.tdetroy.valuacion.model.jugador.JugadorConPrecio;
import com.tdetroy.valuacion.repositories.cartera.MovimientoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data JPA sobre {@link JugadorEntity} (plan.md §2.2). {@link
 * #findByEstado(EstadoJugador)} es la primera consulta propia, agregada por {@code
 * RendimientoServiceImpl} (tasks.md T3.5) para resolver el universo de jugadores {@code ACTIVO}
 * sobre el que corre la ingesta semanal — mismo criterio que {@link MovimientoRepository} de no
 * anticipar una forma de consulta que todavía no se pidió.
 */
public interface JugadorRepository extends JpaRepository<JugadorEntity, UUID> {

    List<JugadorEntity> findByEstado(EstadoJugador estado);

    /**
     * Top por cotización vigente. La inicial se busca sólo para las filas que entran en el límite.
     */
    @Query(
            value =
                    """
                    SELECT t.id AS "jugadorId",
                           t.valor AS "valorVigente", t.fecha_calculo AS "fechaVigente",
                           i.valor AS "valorInicial", i.fecha_calculo AS "fechaInicial"
                    FROM (
                        SELECT j.id, j.nombre, v.valor, v.fecha_calculo
                        FROM jugadores j
                        LEFT JOIN LATERAL (
                            SELECT c.valor, c.fecha_calculo
                            FROM cotizaciones_historicas c
                            WHERE c.jugador_id = j.id
                            ORDER BY c.fecha_calculo DESC
                            LIMIT 1
                        ) v ON TRUE
                        ORDER BY v.valor DESC NULLS LAST, j.nombre, j.id
                        LIMIT :limite
                    ) t
                    LEFT JOIN LATERAL (
                        SELECT c.valor, c.fecha_calculo
                        FROM cotizaciones_historicas c
                        WHERE c.jugador_id = t.id
                        ORDER BY c.semana, c.fecha_calculo DESC
                        LIMIT 1
                    ) i ON TRUE
                    ORDER BY t.valor DESC NULLS LAST, t.nombre, t.id
                    """,
            nativeQuery = true)
    List<FilaRankingProjection> buscarTopPorCotizacion(@Param("limite") int limite);

    /**
     * Top por variación entre la cotización inicial y la vigente; los jugadores sin variación van
     * al final, con el mismo criterio que {@link JugadorConPrecio}.
     */
    @Query(
            value =
                    """
                    SELECT j.id AS "jugadorId",
                           v.valor AS "valorVigente", v.fecha_calculo AS "fechaVigente",
                           i.valor AS "valorInicial", i.fecha_calculo AS "fechaInicial"
                    FROM jugadores j
                    LEFT JOIN LATERAL (
                        SELECT c.valor, c.fecha_calculo
                        FROM cotizaciones_historicas c
                        WHERE c.jugador_id = j.id
                        ORDER BY c.fecha_calculo DESC
                        LIMIT 1
                    ) v ON TRUE
                    LEFT JOIN LATERAL (
                        SELECT c.valor, c.fecha_calculo
                        FROM cotizaciones_historicas c
                        WHERE c.jugador_id = j.id
                        ORDER BY c.semana, c.fecha_calculo DESC
                        LIMIT 1
                    ) i ON TRUE
                    ORDER BY CASE WHEN v.fecha_calculo <> i.fecha_calculo
                                  THEN (v.valor - i.valor) / NULLIF(i.valor, 0)
                             END DESC NULLS LAST,
                             j.nombre, j.id
                    LIMIT :limite
                    """,
            nativeQuery = true)
    List<FilaRankingProjection> buscarTopPorVariacion(@Param("limite") int limite);
}
