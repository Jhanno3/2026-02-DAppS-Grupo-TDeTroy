package com.tdetroy.valuacion.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Configuración centralizada de escala y redondeo para todo valor monetario o de valuación
 * del sistema (saldo virtual, cotización, montos de movimientos).
 *
 * <p>Constitution.md §1 prohíbe {@code float}/{@code double} para dinero y exige
 * {@link BigDecimal} "con escala y RoundingMode definidos de forma centralizada y consistente
 * en todo el sistema" — este es ese único punto. Ningún Service ni entidad de
 * {@link com.tdetroy.valuacion.model} debe declarar su propia escala o su propio
 * {@link RoundingMode}; todos usan {@link #escalar(BigDecimal)}.
 *
 * <p>Valores fijados por plan.md: escala 2 (ej. {@code saldoVirtual: BigDecimal(19,2)},
 * §2.1; {@code CotizacionHistorica.valor: BigDecimal(19,2)}, §2.4) y
 * {@link RoundingMode#HALF_UP} (fórmula de recotización, plan.md §6.1).
 */
public final class Monetario {

    /** Escala (cantidad de decimales) de todo valor monetario/de valuación del sistema. */
    public static final int ESCALA = 2;

    /** Modo de redondeo único para todo cálculo que produzca un valor monetario/de valuación. */
    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    /** Cero monetario, ya normalizado a {@link #ESCALA}. Saldo inicial de alta, plan.md §2.1. */
    public static final BigDecimal CERO = BigDecimal.ZERO.setScale(ESCALA, REDONDEO);

    private Monetario() {
        // utilidad estática, no instanciable
    }

    /**
     * Normaliza {@code valor} a la escala y el redondeo únicos del sistema.
     *
     * @throws NullPointerException si {@code valor} es {@code null}
     */
    public static BigDecimal escalar(BigDecimal valor) {
        Objects.requireNonNull(valor, "valor no puede ser null");
        return valor.setScale(ESCALA, REDONDEO);
    }
}
