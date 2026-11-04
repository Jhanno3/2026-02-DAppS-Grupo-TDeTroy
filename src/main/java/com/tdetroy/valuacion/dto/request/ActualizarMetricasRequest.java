package com.tdetroy.valuacion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Request del administrador para actualizar la ponderación de una o más métricas. */
public record ActualizarMetricasRequest(@NotEmpty List<@Valid ItemActualizarMetrica> metricas) {

    public record ItemActualizarMetrica(
            @NotBlank String clave,
            @NotNull @DecimalMin(value = "0.0001", message = "El peso general debe ser mayor a 0")
                    BigDecimal pesoGeneral,
            boolean activo,
            Map<
                            String,
                            @NotNull
                            @DecimalMin(
                                    value = "0.0001",
                                    message = "El peso por posición debe ser mayor a 0")
                            BigDecimal>
                    pesosPorPosicion) {}
}
