package com.tdetroy.valuacion.model;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;

/**
 * Peso de una métrica de rendimiento en el motor de cotización (plan.md §2.9, §6.2) — tabla de
 * configuración ajustable en runtime, nunca hardcodeada en el algoritmo. {@code clave} debe
 * matchear una key presente dentro de {@code RendimientoPartido.metricas}; {@code posicion} permite
 * calibrar pesos diferenciales según la posición del jugador ("Delantero", "Defensor", etc.) o
 * aplicar el peso por defecto si es {@value #POSICION_GENERAL}.
 */
@Getter
public class PesoMetrica {

    public static final String POSICION_GENERAL = "GENERAL";

    private final String clave;
    private final String posicion;
    private BigDecimal peso;
    private boolean activo;
    private final SentidoMetrica sentido;

    private PesoMetrica(
            String clave,
            String posicion,
            BigDecimal peso,
            boolean activo,
            SentidoMetrica sentido) {
        validarClave(clave);
        validarPeso(peso);
        Objects.requireNonNull(sentido, "sentido no puede ser null");

        this.clave = clave;
        this.posicion =
                (posicion == null || posicion.isBlank()) ? POSICION_GENERAL : posicion.trim();
        this.peso = peso;
        this.activo = activo;
        this.sentido = sentido;
    }

    /** Configura el peso de una métrica para una posición específica. */
    public static PesoMetrica configurar(
            String clave,
            String posicion,
            BigDecimal peso,
            boolean activo,
            SentidoMetrica sentido) {
        return new PesoMetrica(clave, posicion, peso, activo, sentido);
    }

    /** Configura el peso general de una métrica (aplica a cualquier posición sin override). */
    public static PesoMetrica configurar(
            String clave, BigDecimal peso, boolean activo, SentidoMetrica sentido) {
        return new PesoMetrica(clave, POSICION_GENERAL, peso, activo, sentido);
    }

    /** Reconstruye un {@code PesoMetrica} ya persistido a partir de sus datos crudos. */
    public static PesoMetrica reconstruir(
            String clave,
            String posicion,
            BigDecimal peso,
            boolean activo,
            SentidoMetrica sentido) {
        return configurar(clave, posicion, peso, activo, sentido);
    }

    /** Reconstruye un {@code PesoMetrica} general ya persistido a partir de sus datos crudos. */
    public static PesoMetrica reconstruir(
            String clave, BigDecimal peso, boolean activo, SentidoMetrica sentido) {
        return configurar(clave, POSICION_GENERAL, peso, activo, sentido);
    }

    /**
     * Ajusta el peso relativo de esta métrica en el cálculo (plan.md §6.2: "ajustable en runtime
     * vía tabla, no vía deploy").
     *
     * @throws IllegalArgumentException si {@code nuevoPeso} no es mayor a cero
     */
    public void actualizarPeso(BigDecimal nuevoPeso) {
        validarPeso(nuevoPeso);
        this.peso = nuevoPeso;
    }

    /** Habilita esta métrica para participar del próximo cálculo de cotización. */
    public void activar() {
        this.activo = true;
    }

    /** Deshabilita esta métrica del cálculo de cotización, sin perder el peso configurado. */
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
