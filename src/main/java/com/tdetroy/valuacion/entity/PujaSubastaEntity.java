package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.PujaSubasta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entidad JPA sobre la tabla append-only {@code pujas_subasta} (UC-18). */
@Entity
@Table(name = "pujas_subasta")
@Getter
@Setter
@NoArgsConstructor
public class PujaSubastaEntity {

    @Id private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID subastaId;

    @Column(nullable = false, updatable = false)
    private UUID postorId;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    public static PujaSubastaEntity desde(PujaSubasta puja) {
        PujaSubastaEntity entity = new PujaSubastaEntity();
        entity.id = puja.id();
        entity.subastaId = puja.subastaId();
        entity.postorId = puja.postorId();
        entity.precioUnitario = puja.precioUnitario();
        entity.fecha = puja.fecha();
        return entity;
    }

    public PujaSubasta aModelo() {
        return new PujaSubasta(id, subastaId, postorId, precioUnitario, fecha);
    }
}
