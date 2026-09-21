package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.model.Jugador;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Catálogo de jugadores (plan.md §1, UC-03/UC-04/UC-07). La invariante de máximo 100 tokens
 * emitidos y la baja completa (UC-15, cancelación de ofertas + compensación de tenencias) no son
 * responsabilidad de este Service en su forma actual — {@code darDeBaja()} sobre la propia entidad
 * protege la primera, y la orquestación de la baja llega en T6.6.
 */
public interface JugadorService {

    /** Catálogo completo (UC-07), incluidos los jugadores {@code INACTIVO} (spec.md UC-07). */
    List<Jugador> listar();

    /**
     * Detalle de un jugador puntual (UC-07).
     *
     * @throws RecursoNoEncontradoException si no existe un jugador con {@code jugadorId}
     */
    Jugador obtenerPorId(UUID jugadorId);

    /**
     * Da de alta un jugador {@code ACTIVO} con emisión disponible de hasta 100 tokens y sin
     * cotización calculada, y audita la operación (UC-03).
     *
     * @param actorId quién ejecutó el alta (el ADMIN autenticado)
     * @throws IllegalArgumentException si algún dato identificatorio es inválido (null o blanco)
     */
    Jugador darAlta(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            UUID actorId);

    /**
     * Edita los datos identificatorios de un jugador existente y audita la operación (UC-04). Nunca
     * modifica {@code estado}, {@code tokensEmitidos} ni la cotización vigente.
     *
     * @param actorId quién ejecutó la edición (el ADMIN autenticado)
     * @throws RecursoNoEncontradoException si no existe un jugador con {@code jugadorId}
     * @throws IllegalArgumentException si algún dato identificatorio es inválido (null o blanco)
     */
    Jugador editar(
            UUID jugadorId,
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            UUID actorId);
}
