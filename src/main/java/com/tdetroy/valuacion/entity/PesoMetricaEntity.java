package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.PesoMetrica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA sobre la tabla {@code pesos_metrica} (plan.md §2.9). Refleja el esquema 1:1, sin
 * lógica de negocio ni invariantes propias — eso vive en {@link PesoMetrica} (constitution.md §2).
 * {@code clave} es la clave primaria natural (debe matchear una key dentro de {@code
 * RendimientoPartido.metricas}), no hay un {@code id} de tipo {@code UUID} separado.
 */
@Entity
@Table(name = "pesos_metrica")
@Getter
@Setter
@NoArgsConstructor
public class PesoMetricaEntity {

    @Id private String clave;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal peso;

    @Column(nullable = false)
    private boolean activo;

    /** Traduce {@code pesoMetrica} a la fila que persiste {@code repositories/}. */
    public static PesoMetricaEntity desde(PesoMetrica pesoMetrica) {
        PesoMetricaEntity entity = new PesoMetricaEntity();
        entity.clave = pesoMetrica.getClave();
        entity.peso = pesoMetrica.getPeso();
        entity.activo = pesoMetrica.isActivo();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public PesoMetrica aModelo() {
        return PesoMetrica.reconstruir(clave, peso, activo);
    }
}
