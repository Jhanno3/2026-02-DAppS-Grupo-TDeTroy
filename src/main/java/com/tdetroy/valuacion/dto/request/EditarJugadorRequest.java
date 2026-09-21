package com.tdetroy.valuacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

/**
 * {@code PUT /jugadores/{id}} (UC-04). Mismo shape que {@link CrearJugadorRequest} — la edición
 * identificatoria exige los mismos campos completos, nunca un patch parcial (spec.md UC-04).
 */
public record EditarJugadorRequest(
        @NotBlank String nombre,
        @NotBlank String club,
        @NotBlank String posicion,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotBlank String nacionalidad) {}
