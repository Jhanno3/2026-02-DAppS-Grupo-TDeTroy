package com.tdetroy.valuacion.entity;

import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Clave primaria compuesta de {@link PesoMetricaEntity}: {@code (clave, posicion)}. */
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
        if (o == null || getClass() != o.getClass()) return false;
        PesoMetricaId that = (PesoMetricaId) o;
        return Objects.equals(clave, that.clave) && Objects.equals(posicion, that.posicion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clave, posicion);
    }
}
