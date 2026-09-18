package com.tdetroy.valuacion.model;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.SaldoInsuficienteException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Cuenta de usuario con saldo virtual interno (plan.md §2.1, UC-01/UC-02).
 *
 * <p>{@code saldoVirtual} nunca se expone con un setter público: sólo se mueve a través de {@link
 * #debitarSaldo} y {@link #acreditarSaldo}, que validan la operación en la propia entidad
 * (constitution.md §2) — nunca queda a criterio del Service que las invoca validar saldo suficiente
 * o monto positivo por su cuenta.
 */
@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class Usuario {

    @Id private final UUID id;

    @Column(nullable = false, updatable = false)
    private final String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private final RolUsuario rol;

    @Column(name = "saldo_virtual", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoVirtual;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private final Instant fechaCreacion;

    private Usuario(String email, String passwordHash, RolUsuario rol, Instant fechaCreacion) {
        Objects.requireNonNull(email, "email no puede ser null");
        Objects.requireNonNull(passwordHash, "passwordHash no puede ser null");
        Objects.requireNonNull(rol, "rol no puede ser null");
        Objects.requireNonNull(fechaCreacion, "fechaCreacion no puede ser null");
        requireNoBlank(email, "email");
        requireNoBlank(passwordHash, "passwordHash");

        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.saldoVirtual = Monetario.CERO;
        this.fechaCreacion = fechaCreacion;
    }

    /**
     * Da de alta una cuenta {@link RolUsuario#USER} con saldo inicial cero y {@code fechaCreacion =
     * Instant.now()} (UC-01). Único punto de creación de {@code Usuario} — no hay alta pública de
     * {@link RolUsuario#ADMIN} en el MVP.
     */
    public static Usuario registrar(String email, String passwordHash) {
        return registrar(email, passwordHash, Instant.now());
    }

    /**
     * Igual que {@link #registrar(String, String)}, con {@code fechaCreacion} explícita (tests,
     * reproducibilidad).
     */
    public static Usuario registrar(String email, String passwordHash, Instant fechaCreacion) {
        return new Usuario(email, passwordHash, RolUsuario.USER, fechaCreacion);
    }

    /**
     * Descuenta {@code monto} del saldo virtual.
     *
     * @throws IllegalArgumentException si {@code monto} no es mayor a cero
     * @throws SaldoInsuficienteException si el saldo disponible es menor a {@code monto}
     */
    public void debitarSaldo(BigDecimal monto) {
        BigDecimal montoEscalado = requireMontoPositivo(monto);
        if (saldoVirtual.compareTo(montoEscalado) < 0) {
            throw new SaldoInsuficienteException(montoEscalado, saldoVirtual);
        }
        this.saldoVirtual = saldoVirtual.subtract(montoEscalado);
    }

    /**
     * Acredita {@code monto} al saldo virtual (ej. recarga por ADMIN, UC-14; venta de tokens,
     * UC-10).
     *
     * @throws IllegalArgumentException si {@code monto} no es mayor a cero
     */
    public void acreditarSaldo(BigDecimal monto) {
        BigDecimal montoEscalado = requireMontoPositivo(monto);
        this.saldoVirtual = saldoVirtual.add(montoEscalado);
    }

    private static BigDecimal requireMontoPositivo(BigDecimal monto) {
        Objects.requireNonNull(monto, "monto no puede ser null");
        if (monto.signum() <= 0) {
            throw new IllegalArgumentException("monto debe ser mayor a cero, fue " + monto);
        }
        return Monetario.escalar(monto);
    }

    private static void requireNoBlank(String valor, String nombreCampo) {
        if (valor.isBlank()) {
            throw new IllegalArgumentException(nombreCampo + " no puede estar vacío ni ser blanco");
        }
    }
}
