package com.tdetroy.valuacion.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code POST /subastas} (UC-18). {@code precioInicial} es el precio por token desde el que arranca
 * la subasta; {@code duracionMinutos} es el plazo para la primera puja (como mucho 7 días). La
 * tenencia disponible suficiente la valida {@code SubastaService}, no este DTO.
 */
public record CrearSubastaRequest(
        @NotNull UUID jugadorId,
        @Positive int cantidad,
        @NotNull @Positive BigDecimal precioInicial,
        @Positive @Max(10_080) long duracionMinutos) {}
