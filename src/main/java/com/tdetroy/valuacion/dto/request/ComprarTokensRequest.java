package com.tdetroy.valuacion.dto.request;

import jakarta.validation.constraints.Positive;

/**
 * {@code POST /jugadores/{id}/compras} (UC-09). Bean Validation en el borde del Controller
 * (constitution.md §4, plan.md §4: "{@code @Positive} en montos y cantidades") — el resto de las
 * invariantes (saldo suficiente, tokens disponibles) las protege {@code TokenService} directamente,
 * no este DTO.
 */
public record ComprarTokensRequest(@Positive int cantidad) {}
