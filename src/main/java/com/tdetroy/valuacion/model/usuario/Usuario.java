package com.tdetroy.valuacion.model.usuario;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.SaldoInsuficienteException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Cuenta con saldo virtual, que sólo se mueve con {@link #debitarSaldo} y {@link #acreditarSaldo}.
 */
@Getter
public class Usuario {

    private final UUID id;
    private final String email;
    private String passwordHash;
    private final RolUsuario rol;
    private BigDecimal saldoVirtual;
    private final Instant fechaCreacion;

    private Usuario(String email, String passwordHash, RolUsuario rol, Instant fechaCreacion) {
        validarCampos(email, passwordHash, rol, fechaCreacion);

        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.saldoVirtual = Monetario.CERO;
        this.fechaCreacion = fechaCreacion;
    }

    /** Da de alta una cuenta {@link RolUsuario#USER} con saldo cero. */
    public static Usuario registrar(String email, String passwordHash) {
        return registrar(email, passwordHash, Instant.now());
    }

    public static Usuario registrar(String email, String passwordHash, Instant fechaCreacion) {
        return new Usuario(email, passwordHash, RolUsuario.USER, fechaCreacion);
    }

    private Usuario(
            UUID id,
            String email,
            String passwordHash,
            RolUsuario rol,
            BigDecimal saldoVirtual,
            Instant fechaCreacion) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.saldoVirtual = saldoVirtual;
        this.fechaCreacion = fechaCreacion;
    }

    /** Reconstruye desde persistencia; no valida las invariantes de alta. */
    public static Usuario reconstruir(
            UUID id,
            String email,
            String passwordHash,
            RolUsuario rol,
            BigDecimal saldoVirtual,
            Instant fechaCreacion) {
        return new Usuario(id, email, passwordHash, rol, saldoVirtual, fechaCreacion);
    }

    /**
     * @throws IllegalArgumentException si {@code monto} no es mayor a cero
     * @throws SaldoInsuficienteException si el saldo disponible es menor a {@code monto}
     */
    public void debitarSaldo(BigDecimal monto) {
        BigDecimal montoEscalado = exigirMontoPositivo(monto);
        if (saldoVirtual.compareTo(montoEscalado) < 0) {
            throw new SaldoInsuficienteException(montoEscalado, saldoVirtual);
        }
        this.saldoVirtual = saldoVirtual.subtract(montoEscalado);
    }

    /**
     * @throws IllegalArgumentException si {@code monto} no es mayor a cero
     */
    public void acreditarSaldo(BigDecimal monto) {
        BigDecimal montoEscalado = exigirMontoPositivo(monto);
        this.saldoVirtual = saldoVirtual.add(montoEscalado);
    }

    private static void validarCampos(
            String email, String passwordHash, RolUsuario rol, Instant fechaCreacion) {
        Objects.requireNonNull(email, "email no puede ser null");
        Objects.requireNonNull(passwordHash, "passwordHash no puede ser null");
        Objects.requireNonNull(rol, "rol no puede ser null");
        Objects.requireNonNull(fechaCreacion, "fechaCreacion no puede ser null");
        exigirNoVacio(email, "email");
        exigirNoVacio(passwordHash, "passwordHash");
    }

    private static BigDecimal exigirMontoPositivo(BigDecimal monto) {
        Objects.requireNonNull(monto, "monto no puede ser null");
        if (monto.signum() <= 0) {
            throw new IllegalArgumentException("monto debe ser mayor a cero, fue " + monto);
        }
        return Monetario.escalar(monto);
    }

    private static void exigirNoVacio(String valor, String nombreCampo) {
        if (valor.isBlank()) {
            throw new IllegalArgumentException(nombreCampo + " no puede estar vacío ni ser blanco");
        }
    }
}
