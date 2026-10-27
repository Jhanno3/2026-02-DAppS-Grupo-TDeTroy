package com.tdetroy.valuacion.model;

import java.math.BigDecimal;

/**
 * Jugador del catálogo junto a su cotización vigente y su variación porcentual desde el primer
 * cálculo que tuvo (UC-07/UC-08, plan.md §3bis) — composición de sólo lectura para el listado
 * paginado de Mercado, nunca persistida por su cuenta: {@code precio}/{@code variacionPorcentual}
 * siguen viniendo de {@code CotizacionHistoricaRepository} (constitution.md §2, único punto de
 * verdad de la cotización).
 *
 * @param precio {@code null} si el jugador nunca tuvo una cotización calculada ("Sin cotización").
 * @param variacionPorcentual {@code null} si el jugador tiene como mucho un único cálculo histórico
 *     (no hay desde dónde medir variación) — {@code 0} es un valor válido distinto de {@code null}
 *     (hubo dos o más cálculos, pero el valor no cambió).
 */
public record JugadorConPrecio(
        Jugador jugador, BigDecimal precio, BigDecimal variacionPorcentual) {}
