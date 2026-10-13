package com.tdetroy.valuacion.dto.request;

import jakarta.validation.constraints.Positive;

/**
 * {@code POST /jugadores/{id}/ventas} (UC-10). Bean Validation en el borde del Controller
 * (constitution.md §4, plan.md §4: "{@code @Positive} en montos y cantidades") — el resto de las
 * invariantes (tenencia disponible suficiente) las protege {@code TokenService} directamente, no
 * este DTO.
 */
public record VenderTokensRequest(@Positive int cantidad) {}
