package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.PesoMetrica;
import com.tdetroy.valuacion.model.SentidoMetrica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA sobre la tabla {@code pesos_metrica} (plan.md §2.9). Clave compuesta {@code (clave,
 * posicion)}.
 */
@Entity
@Table(name = "pesos_metrica")
@IdClass(PesoMetricaId.class)
@Getter
@Setter
@NoArgsConstructor
public class PesoMetricaEntity {

    @Id
    @Column(nullable = false, length = 100)
    private String clave;

    @Id
    @Column(nullable = false, length = 50)
    private String posicion = PesoMetrica.POSICION_GENERAL;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal peso;

    @Column(nullable = false)
    private boolean activo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SentidoMetrica sentido = SentidoMetrica.POSITIVO;

    /** Traduce {@code pesoMetrica} a la fila que persiste {@code repositories/}. */
    public static PesoMetricaEntity desde(PesoMetrica pesoMetrica) {
        PesoMetricaEntity entity = new PesoMetricaEntity();
        entity.clave = pesoMetrica.getClave();
        entity.posicion =
                pesoMetrica.getPosicion() != null
                        ? pesoMetrica.getPosicion()
                        : PesoMetrica.POSICION_GENERAL;
        entity.peso = pesoMetrica.getPeso();
        entity.activo = pesoMetrica.isActivo();
        entity.sentido = pesoMetrica.getSentido();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public PesoMetrica aModelo() {
        return PesoMetrica.reconstruir(clave, posicion, peso, activo, sentido);
    }
}
