package com.tdetroy.valuacion.config.auditoria;

import com.tdetroy.valuacion.config.seguridad.UsuarioPrincipal;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Registra cada invocación a un controller. Va por fuera de {@code @PreAuthorize}, para incluir
 * también las rechazadas por rol.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class AuditoriaWebAspect {

    static final String NOMBRE_LOGGER = "auditoria.web";

    private static final Logger AUDITORIA = LoggerFactory.getLogger(NOMBRE_LOGGER);
    private static final Logger LOG = LoggerFactory.getLogger(AuditoriaWebAspect.class);
    private static final String ANONIMO = "anonimo";

    private final EnmascaradorParametros enmascarador;

    public AuditoriaWebAspect(EnmascaradorParametros enmascarador) {
        this.enmascarador = enmascarador;
    }

    @Around(
            "within(com.tdetroy.valuacion..*)"
                    + " && @within(org.springframework.web.bind.annotation.RestController)"
                    + " && execution(public * *(..))")
    public Object auditar(ProceedingJoinPoint punto) throws Throwable {
        long inicio = System.nanoTime();
        // Antes de ejecutar: el método podría cambiar el usuario o los parámetros.
        String usuario = usuarioActual();
        Map<String, Object> parametros = parametros(punto);
        try {
            Object resultado = punto.proceed();
            registrar(punto, usuario, parametros, "OK", inicio);
            return resultado;
        } catch (Throwable error) {
            // Sólo el tipo: el mensaje puede traer datos sensibles.
            registrar(punto, usuario, parametros, error.getClass().getSimpleName(), inicio);
            throw error;
        }
    }

    private void registrar(
            ProceedingJoinPoint punto,
            String usuario,
            Map<String, Object> parametros,
            String resultado,
            long inicio) {
        String metodo =
                punto.getSignature().getDeclaringType().getSimpleName()
                        + "."
                        + punto.getSignature().getName();
        AUDITORIA
                .atInfo()
                .addKeyValue("usuario", usuario)
                .addKeyValue("metodo", metodo)
                .addKeyValue("parametros", parametros)
                .addKeyValue("resultado", resultado)
                .addKeyValue(
                        "duracionMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio))
                .log("Invocacion a web service");
    }

    private String usuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null
                        && autenticacion.getPrincipal() instanceof UsuarioPrincipal principal
                ? principal.getUsuarioId().toString()
                : ANONIMO;
    }

    private Map<String, Object> parametros(ProceedingJoinPoint punto) {
        String[] nombres = ((MethodSignature) punto.getSignature()).getParameterNames();
        Object[] valores = punto.getArgs();
        Map<String, Object> parametros = new LinkedHashMap<>();
        for (int i = 0; i < valores.length; i++) {
            if (!esDeInfraestructura(valores[i])) {
                String nombre = nombres == null ? "arg" + i : nombres[i];
                parametros.put(nombre, enmascararSinFallar(nombre, valores[i]));
            }
        }
        return parametros;
    }

    /** Un parámetro que no se puede convertir no debe romper la request. */
    private Object enmascararSinFallar(String nombre, Object valor) {
        try {
            return enmascarador.enmascarar(nombre, valor);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo auditar el parametro {}", nombre);
            return "[no disponible]";
        }
    }

    private static boolean esDeInfraestructura(Object valor) {
        return valor instanceof UserDetails
                || valor instanceof Principal
                || valor instanceof ServletRequest
                || valor instanceof ServletResponse;
    }
}
