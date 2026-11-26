package com.tdetroy.valuacion.entity.cotizacion;

import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Clave compuesta {@code (clave, posicion)} para {@link PesoMetricaEntity}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PesoMetricaId implements Serializable {

    private String clave;
    private String posicion;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PesoMetricaId that)) return false;
        return Objects.equals(clave, that.clave) && Objects.equals(posicion, that.posicion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clave, posicion);
    }
}
