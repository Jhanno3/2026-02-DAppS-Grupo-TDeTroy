package com.tdetroy.valuacion.entity.cotizacion;

import com.tdetroy.valuacion.model.cotizacion.ConfiguracionCotizacion;
import com.tdetroy.valuacion.model.cotizacion.EstrategiaCotizacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA sobre la tabla {@code configuracion_cotizacion}. Refleja el esquema 1:1, sin lógica
 * de negocio — eso vive en {@link ConfiguracionCotizacion} (constitution.md §2).
 */
@Entity
@Table(name = "configuracion_cotizacion")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionCotizacionEntity {

    @Id private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "estrategia_default", nullable = false, length = 30)
    private EstrategiaCotizacion estrategiaPorDefecto;

    @Column(nullable = false)
    private Instant fechaActualizacion;

    /** Traduce {@code configuracion} a la fila única que persiste {@code repositories/}. */
    public static ConfiguracionCotizacionEntity desde(ConfiguracionCotizacion configuracion) {
        ConfiguracionCotizacionEntity entity = new ConfiguracionCotizacionEntity();
        entity.id = ConfiguracionCotizacion.ID;
        entity.estrategiaPorDefecto = configuracion.getEstrategiaPorDefecto();
        entity.fechaActualizacion = configuracion.getFechaActualizacion();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public ConfiguracionCotizacion aModelo() {
        return ConfiguracionCotizacion.reconstruir(estrategiaPorDefecto, fechaActualizacion);
    }
}
