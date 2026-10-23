package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.model.Jugador;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Catálogo de jugadores (plan.md §1, UC-03/UC-04/UC-07/UC-15). La invariante de máximo 100 tokens
 * emitidos la protege {@code Jugador.emitirTokens} sobre la propia entidad; la orquestación
 * completa de la baja (UC-15: cancelación de ofertas abiertas + compensación de tenencias) vive en
 * {@link #darDeBaja} (T6.6).
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
            String liga,
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
            String liga,
            UUID actorId);

    /**
     * Da de baja un jugador (UC-15), en una única transacción: cancela cada oferta P2P {@code
     * ABIERTA} de ese jugador reintegrando lo reservado a su vendedor, compensa con saldo virtual
     * (a la cotización vigente al momento de la baja, "del día anterior" en los términos de spec.md
     * — este sistema no recalcula más de una vez por día) a cada usuario con tenencia {@code > 0}
     * de ese jugador (incluida la recién reintegrada), retira esos tokens de circulación de forma
     * permanente, marca el jugador {@code INACTIVO} y audita la operación completa.
     *
     * @param actorId quién ejecutó la baja (el ADMIN autenticado)
     * @throws RecursoNoEncontradoException si no existe un jugador con {@code jugadorId}
     */
    Jugador darDeBaja(UUID jugadorId, UUID actorId);
}
