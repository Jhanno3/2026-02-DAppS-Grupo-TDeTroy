package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.CotizacionHistorica;
import com.tdetroy.valuacion.model.OrigenCotizacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA sobre la tabla {@code cotizaciones_historicas} (plan.md §2.4). Refleja el esquema
 * 1:1, sin lógica de negocio ni invariantes propias — eso vive en {@link CotizacionHistorica}
 * (constitution.md §2).
 */
@Entity
@Table(name = "cotizaciones_historicas")
@Getter
@Setter
@NoArgsConstructor
public class CotizacionHistoricaEntity {

    @Id private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID jugadorId;

    @Column(nullable = false, updatable = false, length = 8)
    private String semana;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private OrigenCotizacion origen;

    @Column(nullable = false, updatable = false)
    private Instant fechaCalculo;

    /** Traduce {@code cotizacion} a la fila que persiste {@code repositories/}. */
    public static CotizacionHistoricaEntity desde(CotizacionHistorica cotizacion) {
        CotizacionHistoricaEntity entity = new CotizacionHistoricaEntity();
        entity.id = cotizacion.getId();
        entity.jugadorId = cotizacion.getJugadorId();
        entity.semana = cotizacion.getSemana();
        entity.valor = cotizacion.getValor();
        entity.origen = cotizacion.getOrigen();
        entity.fechaCalculo = cotizacion.getFechaCalculo();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public CotizacionHistorica aModelo() {
        return CotizacionHistorica.reconstruir(id, jugadorId, semana, valor, origen, fechaCalculo);
    }
}
