package com.tdetroy.valuacion.repositories.jugador;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Fila del ranking: jugador con su cotización vigente y la inicial (última de su primera semana).
 * Los valores son {@code null} si el jugador nunca tuvo cotización.
 */
public interface FilaRankingProjection {

    UUID getJugadorId();

    BigDecimal getValorVigente();

    Instant getFechaVigente();

    BigDecimal getValorInicial();

    Instant getFechaInicial();
}
