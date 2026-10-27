package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.EstrategiaCotizacion;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Motor de calculo de cotizacion (UC-06, UC-08, UC-13, plan.md S1/S6). Unico punto de verdad del
 * precio de un jugador (constitution.md S2): ningun otro Service calcula ni persiste un valor de
 * cotizacion -- {@code TokenService}, {@code OfertaService}, {@code PortfolioService} y {@code
 * RankingService} (fases siguientes) solo leen la cotizacion vigente via {@code
 * CotizacionHistoricaRepository}/este Service.
 *
 * <p>Soporta múltiples estrategias de valuación vía patrón Strategy ({@link EstrategiaCotizacion}):
 * {@link EstrategiaCotizacion#RENDIMIENTO_HISTORICO} y {@link
 * EstrategiaCotizacion#RENDIMIENTO_ACTUAL}.
 */
public interface CotizacionService {

    /**
     * Calcula y persiste la cotizacion de {@code jugadorId} para la semana ISO en curso usando la
     * estrategia por defecto del sistema.
     */
    CotizacionHistorica calcularCotizacion(UUID jugadorId, OrigenCotizacion origen, UUID actorId);

    /**
     * Calcula y persiste la cotizacion de {@code jugadorId} para la semana ISO en curso usando la
     * {@code estrategia} especificada, en su propia sub-transacción.
     *
     * @throws com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException si {@code
     *     jugadorId} no existe
     */
    CotizacionHistorica calcularCotizacion(
            UUID jugadorId, OrigenCotizacion origen, EstrategiaCotizacion estrategia, UUID actorId);

    /**
     * Recalcula la cotizacion de todos los jugadores {@code ACTIVO} usando la estrategia por
     * defecto.
     */
    void recalcular(OrigenCotizacion origen, UUID actorId);

    /**
     * Recalcula la cotizacion de todos los jugadores {@code ACTIVO} usando la {@code estrategia}
     * indicada (si es {@code null}, usa la estrategia por defecto).
     */
    void recalcular(OrigenCotizacion origen, EstrategiaCotizacion estrategia, UUID actorId);

    /**
     * Historial cronologico completo de cotizaciones de {@code jugadorId} (todas las estrategias).
     */
    List<CotizacionHistorica> obtenerHistorial(UUID jugadorId);

    /**
     * Historial cronologico de cotizaciones de {@code jugadorId} para una {@code estrategia}
     * puntual.
     */
    List<CotizacionHistorica> obtenerHistorial(UUID jugadorId, EstrategiaCotizacion estrategia);

    /** Obtiene la cotización vigente de un jugador para una estrategia determinada. */
    Optional<CotizacionHistorica> obtenerVigente(UUID jugadorId, EstrategiaCotizacion estrategia);
}
