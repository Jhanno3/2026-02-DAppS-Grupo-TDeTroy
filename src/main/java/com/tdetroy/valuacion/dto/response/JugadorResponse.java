package com.tdetroy.valuacion.dto.response;

import com.tdetroy.valuacion.model.EstadoJugador;
import com.tdetroy.valuacion.model.Jugador;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Respuesta de {@code GET /jugadores}/{@code GET /jugadores/{id}} (UC-07) y de {@code POST
 * /jugadores}/{@code PUT /jugadores/{id}} (UC-03/UC-04). {@code tokensDisponibles} delega en {@link
 * Jugador#tokensDisponibles()} — nunca se recalcula acá (spec.md UC-07: un jugador {@link
 * EstadoJugador#INACTIVO} siempre muestra 0, aunque {@code tokensEmitidos} haya quedado congelado
 * en un valor menor a 100). Deliberadamente no incluye cotización vigente todavía — {@code
 * CotizacionService} llega en Fase 4 (T4.x); se suma cuando ese Service exista.
 */
public record JugadorResponse(
        UUID id,
        String nombre,
        String club,
        String posicion,
        LocalDate fechaNacimiento,
        String nacionalidad,
        EstadoJugador estado,
        int tokensDisponibles,
        Instant fechaUltimaActualizacionRendimiento) {

    public static JugadorResponse desde(Jugador jugador) {
        return new JugadorResponse(
                jugador.getId(),
                jugador.getNombre(),
                jugador.getClub(),
                jugador.getPosicion(),
                jugador.getFechaNacimiento(),
                jugador.getNacionalidad(),
                jugador.getEstado(),
                jugador.tokensDisponibles(),
                jugador.getFechaUltimaActualizacionRendimiento());
    }
}
