package com.tdetroy.valuacion.dto.request;

import jakarta.validation.constraints.Positive;

/**
 * {@code POST /ofertas/{id}/compras} (UC-17). Bean Validation en el borde del Controller
 * (constitution.md §4) — el resto de las invariantes (cantidad disponible, saldo suficiente) las
 * protege {@code OfertaService} directamente, no este DTO.
 */
public record ComprarOfertaRequest(@Positive int cantidad) {}
