package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.EstadoOferta;
import com.tdetroy.valuacion.model.OfertaP2P;
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
 * Entidad JPA sobre la tabla {@code ofertas_p2p} (plan.md §2.7). Refleja el esquema 1:1, sin lógica
 * de negocio ni invariantes propias — eso vive en {@link OfertaP2P} (constitution.md §2).
 */
@Entity
@Table(name = "ofertas_p2p")
@Getter
@Setter
@NoArgsConstructor
public class OfertaP2PEntity {

    @Id private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID vendedorId;

    @Column(nullable = false, updatable = false)
    private UUID jugadorId;

    @Column(nullable = false, updatable = false)
    private int cantidadPublicada;

    @Column(nullable = false)
    private int cantidadDisponible;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal precioUnitario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoOferta estado;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(nullable = false, updatable = false)
    private Instant fechaVencimiento;

    /** Traduce {@code oferta} a la fila que persiste {@code repositories/}. */
    public static OfertaP2PEntity desde(OfertaP2P oferta) {
        OfertaP2PEntity entity = new OfertaP2PEntity();
        entity.id = oferta.getId();
        entity.vendedorId = oferta.getVendedorId();
        entity.jugadorId = oferta.getJugadorId();
        entity.cantidadPublicada = oferta.getCantidadPublicada();
        entity.cantidadDisponible = oferta.getCantidadDisponible();
        entity.precioUnitario = oferta.getPrecioUnitario();
        entity.estado = oferta.getEstado();
        entity.fechaCreacion = oferta.getFechaCreacion();
        entity.fechaVencimiento = oferta.getFechaVencimiento();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public OfertaP2P aModelo() {
        return OfertaP2P.reconstruir(
                id,
                vendedorId,
                jugadorId,
                cantidadPublicada,
                cantidadDisponible,
                precioUnitario,
                estado,
                fechaCreacion,
                fechaVencimiento);
    }
}
