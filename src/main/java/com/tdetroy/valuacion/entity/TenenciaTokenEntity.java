package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.TenenciaToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA sobre la tabla {@code tenencias_token} (plan.md §2.5). Refleja el esquema 1:1, sin
 * lógica de negocio ni invariantes propias — eso vive en {@link TenenciaToken} (constitution.md
 * §2).
 */
@Entity
@Table(name = "tenencias_token")
@Getter
@Setter
@NoArgsConstructor
public class TenenciaTokenEntity {

    @Id private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(nullable = false, updatable = false)
    private UUID jugadorId;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false)
    private int cantidadReservada;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal costoTotal;

    /** Traduce {@code tenencia} a la fila que persiste {@code repositories/}. */
    public static TenenciaTokenEntity desde(TenenciaToken tenencia) {
        TenenciaTokenEntity entity = new TenenciaTokenEntity();
        entity.id = tenencia.getId();
        entity.usuarioId = tenencia.getUsuarioId();
        entity.jugadorId = tenencia.getJugadorId();
        entity.cantidad = tenencia.getCantidad();
        entity.cantidadReservada = tenencia.getCantidadReservada();
        entity.costoTotal = tenencia.getCostoTotal();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public TenenciaToken aModelo() {
        return TenenciaToken.reconstruir(
                id, usuarioId, jugadorId, cantidad, cantidadReservada, costoTotal);
    }
}
