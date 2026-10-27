package com.tdetroy.valuacion.repositories;

import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositorio Spring Data JPA sobre {@link CotizacionHistoricaEntity} (plan.md S2.4).
 *
 * <p>{@link #findTopByJugadorIdOrderByFechaCalculoDesc} (T4.3) resuelve la "cotizacion
 * vigente"/"cotizacion anterior" de un jugador -- por definicion (plan.md S2.4), el registro con
 * {@code fechaCalculo} mas reciente para ese {@code jugadorId}; nunca una columna separada que
 * pueda desincronizarse.
 *
 * <p>{@link #findByJugadorIdOrderByFechaCalculoAsc} (T4.7) trae el historial cronologico completo
 * para {@code GET /jugadores/{id}/cotizaciones} (UC-08) -- incluye jugadores dados de baja: su
 * cotizacion queda congelada en el ultimo registro, pero el historial hasta ese punto sigue siendo
 * consultable.
 *
 * <p>{@link #findValoresVigentes} y {@link #findValoresIniciales} (plan.md S3bis) traen, en una
 * sola consulta cada una, el valor mas reciente/mas antiguo de *todos* los jugadores a la vez --
 * usadas por {@code JugadorServiceImpl#listarMercado} para armar precio y variacion del listado
 * paginado de Mercado sin pedir la cotizacion jugador por jugador (antes N+1 desde el cliente).
 * {@code DISTINCT ON (jugador_id)} es sintaxis propia de Postgres (ya el unico motor soportado por
 * este proyecto, ver application.properties) para "una fila por jugador, la que gane el ORDER BY".
 */
public interface CotizacionHistoricaRepository
        extends JpaRepository<CotizacionHistoricaEntity, UUID> {

    Optional<CotizacionHistoricaEntity> findTopByJugadorIdOrderByFechaCalculoDesc(UUID jugadorId);

    List<CotizacionHistoricaEntity> findByJugadorIdOrderByFechaCalculoAsc(UUID jugadorId);

    @Query(
            value =
                    "SELECT jugador_id AS \"jugadorId\", valor AS \"valor\", fecha_calculo AS"
                            + " \"fechaCalculo\" FROM (SELECT DISTINCT ON (jugador_id) jugador_id,"
                            + " valor, fecha_calculo FROM cotizaciones_historicas ORDER BY"
                            + " jugador_id, fecha_calculo DESC) t",
            nativeQuery = true)
    List<ValorPorJugadorProjection> findValoresVigentes();

    @Query(
            value =
                    "SELECT jugador_id AS \"jugadorId\", valor AS \"valor\", fecha_calculo AS"
                            + " \"fechaCalculo\" FROM (SELECT DISTINCT ON (jugador_id) jugador_id,"
                            + " valor, fecha_calculo FROM cotizaciones_historicas ORDER BY"
                            + " jugador_id, fecha_calculo ASC) t",
            nativeQuery = true)
    List<ValorPorJugadorProjection> findValoresIniciales();
}
