package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.RendimientoAcumuladoWhoScored;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad JPA sobre la tabla {@code rendimientos_acumulados_whoscored} (plan.md §8.1). Refleja el
 * esquema 1:1, sin lógica de negocio ni invariantes propias — eso vive en {@link
 * RendimientoAcumuladoWhoScored} (constitution.md §2). {@code jugadorId} es la clave primaria
 * natural (snapshot único por jugador, nunca histórico).
 */
@Entity
@Table(name = "rendimientos_acumulados_whoscored")
@Getter
@Setter
@NoArgsConstructor
public class RendimientoAcumuladoWhoScoredEntity {

    @Id private UUID jugadorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String metricas;

    @Column(nullable = false)
    private Instant fechaActualizacion;

    /** Traduce {@code snapshot} a la fila que persiste {@code repositories/}. */
    public static RendimientoAcumuladoWhoScoredEntity desde(
            RendimientoAcumuladoWhoScored snapshot) {
        RendimientoAcumuladoWhoScoredEntity entity = new RendimientoAcumuladoWhoScoredEntity();
        entity.jugadorId = snapshot.getJugadorId();
        entity.metricas = snapshot.getMetricas();
        entity.fechaActualizacion = snapshot.getFechaActualizacion();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public RendimientoAcumuladoWhoScored aModelo() {
        return RendimientoAcumuladoWhoScored.reconstruir(jugadorId, metricas, fechaActualizacion);
    }
}
