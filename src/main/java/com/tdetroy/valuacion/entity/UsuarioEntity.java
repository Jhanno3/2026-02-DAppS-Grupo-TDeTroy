package com.tdetroy.valuacion.entity;

import com.tdetroy.valuacion.model.RolUsuario;
import com.tdetroy.valuacion.model.Usuario;
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
 * Entidad JPA sobre la tabla {@code usuarios} (plan.md §2.1). Refleja el esquema 1:1, sin lógica de
 * negocio ni invariantes propias — eso vive en {@link Usuario} (constitution.md §2).
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioEntity {

    @Id private UUID id;

    @Column(nullable = false, updatable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private RolUsuario rol;

    @Column(name = "saldo_virtual", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoVirtual;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    /** Traduce {@code usuario} a la fila que persiste {@code repositories/}. */
    public static UsuarioEntity desde(Usuario usuario) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.id = usuario.getId();
        entity.email = usuario.getEmail();
        entity.passwordHash = usuario.getPasswordHash();
        entity.rol = usuario.getRol();
        entity.saldoVirtual = usuario.getSaldoVirtual();
        entity.fechaCreacion = usuario.getFechaCreacion();
        return entity;
    }

    /** Reconstruye el objeto de dominio a partir de esta fila ya persistida. */
    public Usuario aModelo() {
        return Usuario.reconstruir(id, email, passwordHash, rol, saldoVirtual, fechaCreacion);
    }
}
