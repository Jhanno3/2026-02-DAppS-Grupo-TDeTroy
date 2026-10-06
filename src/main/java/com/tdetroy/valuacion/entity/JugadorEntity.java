package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.EstadoJugador;
import com.tdetroy.valuacion.model.Jugador;
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

/**
 * Entidad JPA sobre la tabla {@code jugadores} (plan.md §2.2). Refleja el esquema 1:1, sin lógica
 * de negocio ni invariantes propias — eso vive en {@link Jugador} (constitution.md §2).
 */
@Entity
@Table(name = "jugadores")
@Getter
@Setter
@NoArgsConstructor
public class JugadorEntity {

    @Id private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String club;

    @Column(nullable = false)
    private String posicion;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false)
    private String nacionalidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoJugador estado;

    @Column(nullable = false)
    private int tokensEmitidos;

    private UUID cotizacionVigenteId;

    private Instant fechaUltimaActualizacionRendimiento;

    /** Traduce {@code jugador} a la fila que persiste {@code repositories/}. */
    public static JugadorEntity desde(Jugador jugador) {
        JugadorEntity entity = new JugadorEntity();
        entity.id = jugador.getId();
        entity.nombre = jugador.getNombre();
        entity.club = jugador.getClub();
        entity.posicion = jugador.getPosicion();
        entity.fechaNacimiento = jugador.getFechaNacimiento();
        entity.nacionalidad = jugador.getNacionalidad();
        entity.estado = jugador.getEstado();
        entity.tokensEmitidos = jugador.getTokensEmitidos();
        entity.cotizacionVigenteId = jugador.getCotizacionVigenteId();
        entity.fechaUltimaActualizacionRendimiento =
                jugador.getFechaUltimaActualizacionRendimiento();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public Jugador aModelo() {
        return Jugador.reconstruir(
                id,
                nombre,
                club,
                posicion,
                fechaNacimiento,
                nacionalidad,
                estado,
                tokensEmitidos,
                cotizacionVigenteId,
                fechaUltimaActualizacionRendimiento);
    }
}
