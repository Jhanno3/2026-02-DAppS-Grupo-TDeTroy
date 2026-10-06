package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.FuenteResultado;
import com.tdetroy.valuacion.model.RendimientoPartido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad JPA sobre la tabla {@code rendimientos_partido} (plan.md §2.3). Refleja el esquema 1:1,
 * sin lógica de negocio ni invariantes propias — eso vive en {@link RendimientoPartido}
 * (constitution.md §2).
 */
@Entity
@Table(name = "rendimientos_partido")
@Getter
@Setter
@NoArgsConstructor
public class RendimientoPartidoEntity {

    @Id private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID jugadorId;

    @Column(nullable = false, updatable = false)
    private String partidoExternoId;

    @Column(nullable = false, updatable = false)
    private LocalDate fechaPartido;

    @Column(nullable = false, updatable = false, length = 8)
    private String semanaCalculo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private String metricas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private FuenteResultado fuenteResultado;

    @Column(nullable = false, updatable = false)
    private Instant fechaIngesta;

    /** Traduce {@code rendimiento} a la fila que persiste {@code repositories/}. */
    public static RendimientoPartidoEntity desde(RendimientoPartido rendimiento) {
        RendimientoPartidoEntity entity = new RendimientoPartidoEntity();
        entity.id = rendimiento.getId();
        entity.jugadorId = rendimiento.getJugadorId();
        entity.partidoExternoId = rendimiento.getPartidoExternoId();
        entity.fechaPartido = rendimiento.getFechaPartido();
        entity.semanaCalculo = rendimiento.getSemanaCalculo();
        entity.metricas = rendimiento.getMetricas();
        entity.fuenteResultado = rendimiento.getFuenteResultado();
        entity.fechaIngesta = rendimiento.getFechaIngesta();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public RendimientoPartido aModelo() {
        return RendimientoPartido.reconstruir(
                id,
                jugadorId,
                partidoExternoId,
                fechaPartido,
                semanaCalculo,
                metricas,
                fuenteResultado,
                fechaIngesta);
    }
}
