package com.tdetroy.valuacion.model.cotizacion;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;

/**
 * Peso de una métrica en la cotización, ajustable en runtime. {@code clave} debe coincidir con una
 * key de {@code RendimientoPartido.metricas}; sólo las activas participan del cálculo.
 */
@Getter
public class PesoMetrica {

    private final String clave;
    private BigDecimal peso;
    private boolean activo;
    private final SentidoMetrica sentido;

    private PesoMetrica(String clave, BigDecimal peso, boolean activo, SentidoMetrica sentido) {
        validarClave(clave);
        validarPeso(peso);
        Objects.requireNonNull(sentido, "sentido no puede ser null");

        this.clave = clave;
        this.peso = peso;
        this.activo = activo;
        this.sentido = sentido;
    }

    public static PesoMetrica configurar(
            String clave, BigDecimal peso, boolean activo, SentidoMetrica sentido) {
        return new PesoMetrica(clave, peso, activo, sentido);
    }

    /** Reconstruye desde persistencia; valida igual que {@link #configurar}. */
    public static PesoMetrica reconstruir(
            String clave, BigDecimal peso, boolean activo, SentidoMetrica sentido) {
        return configurar(clave, peso, activo, sentido);
    }

    /**
     * @throws IllegalArgumentException si {@code nuevoPeso} no es mayor a cero
     */
    public void actualizarPeso(BigDecimal nuevoPeso) {
        validarPeso(nuevoPeso);
        this.peso = nuevoPeso;
    }

    public void activar() {
        this.activo = true;
    }

    /** Conserva el peso configurado para poder reactivarla. */
    public void desactivar() {
        this.activo = false;
    }

    private static void validarClave(String clave) {
        Objects.requireNonNull(clave, "clave no puede ser null");
        if (clave.isBlank()) {
            throw new IllegalArgumentException("clave no puede estar vacía ni ser blanco");
        }
    }

    private static void validarPeso(BigDecimal peso) {
        Objects.requireNonNull(peso, "peso no puede ser null");
        if (peso.signum() <= 0) {
            throw new IllegalArgumentException("peso debe ser mayor a cero, fue " + peso);
        }
    }
}
