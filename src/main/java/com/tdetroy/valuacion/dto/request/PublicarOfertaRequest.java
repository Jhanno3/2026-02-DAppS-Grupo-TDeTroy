package com.tdetroy.valuacion.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code POST /ofertas} (UC-17). Bean Validation en el borde del Controller (constitution.md §4,
 * plan.md §4: "{@code @Positive} en montos y cantidades") — el resto de las invariantes (tenencia
 * no reservada suficiente) las protege {@code OfertaService} directamente, no este DTO. {@code
 * precioUnitario} es el precio por token que el propio vendedor fija libremente (fijo durante toda
 * la vida de la oferta); {@code horasLimite} es cuántas horas antes de que la oferta venza
 * automáticamente si nadie la compra.
 */
public record PublicarOfertaRequest(
        @NotNull UUID jugadorId,
        @Positive int cantidad,
        @NotNull @Positive BigDecimal precioUnitario,
        @Positive long horasLimite) {}
