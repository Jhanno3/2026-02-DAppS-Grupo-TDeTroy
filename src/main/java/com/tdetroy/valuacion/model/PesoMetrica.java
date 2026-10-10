package com.tdetroy.valuacion.model;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;

/**
 * Peso de una métrica de rendimiento en el motor de cotización (plan.md §2.9, §6.2) — tabla de
 * configuración ajustable en runtime, nunca hardcodeada en el algoritmo. {@code clave} debe
 * matchear una key presente dentro de {@code RendimientoPartido.metricas}; {@code
 * CotizacionServiceImpl} (T4.3) recorre únicamente las claves con {@link #isActivo()} al calcular
 * el puntaje ponderado de un jugador — una métrica nueva que WhoScored empiece a proveer queda
 * disponible como dato desde el día 1 (persistida en el JSON), pero sólo participa del cálculo una
 * vez que alguien le asigna un {@code PesoMetrica} activo.
 *
 * <p>A diferencia de {@link RendimientoPartido}/{@link CotizacionHistorica}, no es append-only:
 * {@code peso} y {@code activo} se ajustan in-place vía {@link #actualizarPeso}/{@link
 * #activar}/{@link #desactivar} — es exactamente el punto de "ajustable sin deploy" de plan.md
 * §6.2. Nunca expone un setter público crudo sobre esos campos (constitution.md §2).
 */
@Getter
public class PesoMetrica {

    private final String clave;
    private BigDecimal peso;
    private boolean activo;

    private PesoMetrica(String clave, BigDecimal peso, boolean activo) {
        validarClave(clave);
        validarPeso(peso);

        this.clave = clave;
        this.peso = peso;
        this.activo = activo;
    }

    /** Configura el peso de una métrica. Único punto de creación de {@code PesoMetrica}. */
    public static PesoMetrica configurar(String clave, BigDecimal peso, boolean activo) {
        return new PesoMetrica(clave, peso, activo);
    }

    /**
     * Reconstruye un {@code PesoMetrica} ya persistido a partir de sus datos crudos (usado por
     * {@code entity/PesoMetricaEntity#aModelo()} en el límite con {@code repositories/}). A
     * diferencia de {@link RendimientoPartido}/{@link CotizacionHistorica}, no hay un {@code id}
     * autogenerado que distinga este camino del de {@link #configurar} — la validación de {@code
     * clave}/{@code peso} es la misma en ambos casos, así que delega directamente.
     */
    public static PesoMetrica reconstruir(String clave, BigDecimal peso, boolean activo) {
        return configurar(clave, peso, activo);
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

    /**
     * Deshabilita esta métrica del cálculo de cotización, sin perder el peso configurado (queda
     * disponible para reactivarla más adelante sin tener que recalibrarlo).
     */
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
