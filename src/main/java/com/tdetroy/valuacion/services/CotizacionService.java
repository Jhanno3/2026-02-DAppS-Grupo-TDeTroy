package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import java.util.List;
import java.util.UUID;

/**
 * Motor de calculo de cotizacion (UC-06, UC-08, UC-13, plan.md S1/S6). Unico punto de verdad del
 * precio de un jugador (constitution.md S2): ningun otro Service calcula ni persiste un valor de
 * cotizacion -- {@code TokenService}, {@code OfertaService}, {@code PortfolioService} y {@code
 * RankingService} (fases siguientes) solo leen la cotizacion vigente via {@code
 * CotizacionHistoricaRepository}/este Service.
 *
 * <p>{@link #calcularCotizacion} implementa el calculo por jugador de plan.md S6.1 (y el caso sin
 * rendimiento en la semana de S6.3); {@link #recalcular} (tasks.md T4.5/T4.6) lo invoca sobre todo
 * el catalogo {@code ACTIVO} -- job semanal y disparo manual restringido a ADMIN comparten
 * exactamente el mismo metodo, nunca reimplementan el algoritmo (constitution.md S2: "exactamente
 * el mismo" calculo en ambas vias).
 */
public interface CotizacionService {

    /**
     * Calcula y persiste la cotizacion de {@code jugadorId} para la semana ISO en curso (plan.md
     * S6.1, pasos 1 a 7), en su propia transaccion (tasks.md T4.5, plan.md S5):
     *
     * <ol>
     *   <li>Trae el rendimiento del jugador de la semana en curso y su ventana historica reciente
     *       (para la normalizacion min-max de S6.2).
     *   <li>Si no hubo ningun {@code RendimientoPartido} esta semana, aplica el decaimiento leve
     *       por inactividad de S6.3 en vez de calcular un puntaje.
     *   <li>En caso contrario, calcula el puntaje de rendimiento ponderado por los {@code
     *       PesoMetrica} activos.
     *   <li>Aplica el {@code factorAjuste} (acotado a +-20%) sobre la cotizacion anterior -- o
     *       {@code app.cotizacion.valor-inicial} si el jugador nunca tuvo un calculo previo.
     *   <li>Persiste el nuevo {@code CotizacionHistorica} y actualiza el puntero "vigente" del
     *       jugador.
     *   <li>Audita el cambio via {@code AuditoriaService} (constitution.md S4), con {@code actorId}
     *       tal cual se lo pasaron (ej. {@code null} para el job automatico, representando
     *       SISTEMA).
     * </ol>
     *
     * @throws com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException si {@code
     *     jugadorId} no existe
     */
    CotizacionHistorica calcularCotizacion(UUID jugadorId, OrigenCotizacion origen, UUID actorId);

    /**
     * Recalcula la cotizacion de todos los jugadores {@code ACTIVO} del catalogo (plan.md S6.4),
     * invocando {@link #calcularCotizacion} para cada uno en su propia sub-transaccion -- nunca una
     * transaccion gigante para todo el catalogo (plan.md S5): una falla puntual en un jugador se
     * loguea y no interrumpe ni revierte el calculo ya persistido de los demas (mismo criterio que
     * {@code RendimientoServiceImpl}, constitution.md S1).
     *
     * <p>Unico metodo invocado tanto por el job semanal ({@code recalcular(AUTOMATICO, null)},
     * tasks.md T4.5) como por el disparo manual restringido a ADMIN ({@code recalcular(MANUAL,
     * actorId)}, tasks.md T4.6).
     */
    void recalcular(OrigenCotizacion origen, UUID actorId);

    /**
     * Historial cronologico completo de cotizaciones de {@code jugadorId} (UC-08, tasks.md T4.7),
     * ordenado de la mas antigua a la mas reciente. Incluye jugadores dados de baja: su cotizacion
     * queda congelada en el ultimo registro (UC-15), pero el historial hasta ese punto sigue siendo
     * consultable.
     *
     * @throws com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException si {@code
     *     jugadorId} no existe
     */
    List<CotizacionHistorica> obtenerHistorial(UUID jugadorId);
}
