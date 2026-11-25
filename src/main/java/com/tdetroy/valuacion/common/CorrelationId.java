package com.tdetroy.valuacion.common;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

/** Id que comparten todos los logs de una misma request o ejecución de un job. */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String CLAVE_MDC = "correlationId";

    private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private CorrelationId() {}

    /** Uno inválido se reemplaza: llega de afuera y termina en los logs. */
    public static String aceptarOGenerar(String recibido) {
        return recibido != null && VALIDO.matcher(recibido).matches() ? recibido : generar();
    }

    public static String generar() {
        return UUID.randomUUID().toString();
    }

    public static Optional<String> actual() {
        return Optional.ofNullable(MDC.get(CLAVE_MDC));
    }
}
