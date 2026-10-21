package com.tdetroy.valuacion.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * {@code POST /usuarios/{id}/saldo} (UC-14). Bean Validation en el borde del Controller
 * (constitution.md §4, plan.md §4: "{@code @Positive} en montos y cantidades") — el resto de las
 * invariantes las protege {@code UsuarioService} directamente, no este DTO.
 */
public record RecargarSaldoRequest(@NotNull @Positive BigDecimal monto) {}
