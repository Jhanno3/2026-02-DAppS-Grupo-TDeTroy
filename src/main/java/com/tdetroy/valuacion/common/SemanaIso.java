package com.tdetroy.valuacion.common;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

/**
 * Semana ISO ({@code yyyy-Www}) compartida por la ingesta de rendimiento y el recálculo de
 * cotización (plan.md §8.2). Ambos jobs corren el lunes de madrugada, así que la semana que
 * procesan es la del día anterior -- la que acaba de terminar --, nunca la del día en que corren:
 * si cada uno tomara "hoy" por su cuenta, el lunes la cotización buscaría datos en una semana que
 * la ingesta todavía no llenó.
 */
public final class SemanaIso {

    private SemanaIso() {}

    public static LocalDate fechaDeReferencia(LocalDate hoy) {
        return hoy.minusDays(1);
    }

    public static String de(LocalDate fecha) {
        int anio = fecha.get(IsoFields.WEEK_BASED_YEAR);
        int semana = fecha.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return "%04d-W%02d".formatted(anio, semana);
    }
}
