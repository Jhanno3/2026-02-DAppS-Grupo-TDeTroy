package com.tdetroy.valuacion.config.auditoria;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Convierte parámetros a una forma logueable, ocultando los datos sensibles. */
@Component
public class EnmascaradorParametros {

    static final String OCULTO = "***";
    private static final int LARGO_MAXIMO = 500;

    private static final List<String> FRAGMENTOS_SENSIBLES =
            List.of(
                    "password",
                    "passwd",
                    "contrasen",
                    "secret",
                    "apikey",
                    "tarjeta",
                    "card",
                    "cvv",
                    "cvc",
                    "codigoseguridad",
                    "email",
                    "authorization");

    /** Por nombre exacto: "token" también es un concepto del negocio (ej. cantidadTokens). */
    private static final Set<String> NOMBRES_SENSIBLES =
            Set.of("token", "accesstoken", "refreshtoken", "jwt", "pan", "titular");

    /** Cubre una tarjeta que llegue en un campo con otro nombre. */
    private static final Pattern NUMERO_TARJETA = Pattern.compile("\\d(?:[ -]?\\d){12,18}");

    private final JsonMapper jsonMapper;

    public EnmascaradorParametros(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public Object enmascarar(String nombre, Object valor) {
        if (esSensible(nombre)) {
            return OCULTO;
        }
        return limpiar(jsonMapper.convertValue(valor, Object.class));
    }

    private Object limpiar(Object valor) {
        return switch (valor) {
            case Map<?, ?> mapa -> limpiarMapa(mapa);
            case List<?> lista -> lista.stream().map(this::limpiar).toList();
            case String texto -> limpiarTexto(texto);
            case Number numero when esTarjeta(numero.toString()) -> OCULTO;
            case null -> "null";
            default -> valor;
        };
    }

    private Map<String, Object> limpiarMapa(Map<?, ?> mapa) {
        Map<String, Object> limpio = new LinkedHashMap<>();
        mapa.forEach(
                (clave, valor) -> {
                    String nombre = String.valueOf(clave);
                    limpio.put(nombre, esSensible(nombre) ? OCULTO : limpiar(valor));
                });
        return limpio;
    }

    private String limpiarTexto(String texto) {
        if (esTarjeta(texto.trim())) {
            return OCULTO;
        }
        return texto.length() > LARGO_MAXIMO ? texto.substring(0, LARGO_MAXIMO) + "..." : texto;
    }

    private static boolean esTarjeta(String texto) {
        return NUMERO_TARJETA.matcher(texto).matches();
    }

    private static boolean esSensible(String nombre) {
        String normalizado =
                Normalizer.normalize(nombre, Normalizer.Form.NFD)
                        .replaceAll("[^A-Za-z0-9]", "")
                        .toLowerCase(Locale.ROOT);
        return NOMBRES_SENSIBLES.contains(normalizado)
                || FRAGMENTOS_SENSIBLES.stream().anyMatch(normalizado::contains);
    }
}
