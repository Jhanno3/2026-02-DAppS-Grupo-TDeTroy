package com.tdetroy.valuacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

/**
 * {@code POST /jugadores} (UC-03). Bean Validation en el borde del Controller (constitution.md §4)
 * — el resto de las invariantes (emisión inicial en 0, estado {@code ACTIVO}) las protege {@code
 * Jugador.darAlta} directamente, no este DTO.
 */
public record CrearJugadorRequest(
        @NotBlank String nombre,
        @NotBlank String club,
        @NotBlank String posicion,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotBlank String nacionalidad) {}
