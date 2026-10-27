package com.tdetroy.valuacion.repositories;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Proyección de una fila de {@code cotizaciones_historicas} (sólo {@code jugadorId}/{@code valor}/
 * {@code fechaCalculo}), usada por {@link CotizacionHistoricaRepository#findValoresVigentes()} y
 * {@link CotizacionHistoricaRepository#findValoresIniciales()} para traer, en una sola consulta, el
 * valor de todos los jugadores a la vez -- en vez de una consulta por jugador (plan.md §3bis: el
 * listado de Mercado paginado existe justamente para evitar ese patrón N+1).
 */
public interface ValorPorJugadorProjection {

    UUID getJugadorId();

    BigDecimal getValor();

    Instant getFechaCalculo();
}
