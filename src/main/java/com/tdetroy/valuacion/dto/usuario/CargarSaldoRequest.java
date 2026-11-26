package com.tdetroy.valuacion.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CargarSaldoRequest(
        @NotNull @Positive BigDecimal monto,
        @NotBlank
                @Pattern(
                        regexp = "^\\d{16}$",
                        message = "El número de tarjeta debe tener 16 dígitos")
                String numeroTarjeta,
        @NotBlank @Size(min = 2, max = 100) String titular,
        @NotBlank
                @Pattern(
                        regexp = "^(0[1-9]|1[0-2])/\\d{2}$",
                        message = "El vencimiento debe tener formato MM/AA")
                String vencimiento,
        @NotBlank
                @Pattern(
                        regexp = "^\\d{3,4}$",
                        message = "El código de seguridad debe tener 3 o 4 dígitos")
                String codigoSeguridad) {}
