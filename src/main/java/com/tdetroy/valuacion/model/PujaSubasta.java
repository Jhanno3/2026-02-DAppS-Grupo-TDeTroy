package com.tdetroy.valuacion.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro append-only de una puja aceptada en una {@link Subasta} (UC-18), para mostrar cómo
 * evolucionó el precio. No interviene en ningún cálculo: el precio vigente vive en {@link
 * Subasta#getPrecioActual()}.
 */
public record PujaSubasta(
        UUID id, UUID subastaId, UUID postorId, BigDecimal precioUnitario, Instant fecha) {

    public PujaSubasta {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(subastaId, "subastaId no puede ser null");
        Objects.requireNonNull(postorId, "postorId no puede ser null");
        Objects.requireNonNull(precioUnitario, "precioUnitario no puede ser null");
        Objects.requireNonNull(fecha, "fecha no puede ser null");
    }

    public static PujaSubasta registrar(
            UUID subastaId, UUID postorId, BigDecimal precioUnitario, Instant fecha) {
        return new PujaSubasta(UUID.randomUUID(), subastaId, postorId, precioUnitario, fecha);
    }
}
