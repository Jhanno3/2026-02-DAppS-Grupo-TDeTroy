package com.tdetroy.valuacion.config.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tdetroy.valuacion.config.seguridad.UsuarioPrincipal;
import com.tdetroy.valuacion.model.usuario.RolUsuario;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

class AuditoriaWebAspectTest {

    private final Logger logger =
            (Logger) LoggerFactory.getLogger(AuditoriaWebAspect.NOMBRE_LOGGER);
    private final ListAppender<ILoggingEvent> eventos = new ListAppender<>();
    private final UsuarioPrincipal principal =
            UsuarioPrincipal.desde(UUID.randomUUID(), "ana@example.com", RolUsuario.USER);
    private ControllerDePrueba controller;

    @BeforeEach
    void setUp() {
        eventos.start();
        logger.addAppender(eventos);
        var fabrica = new AspectJProxyFactory(new ControllerDePrueba());
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(
                new AuditoriaWebAspect(new EnmascaradorParametros(JsonMapper.builder().build())));
        controller = fabrica.getProxy();
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(eventos);
        SecurityContextHolder.clearContext();
    }

    private Map<String, Object> registro() {
        assertThat(eventos.list).hasSize(1);
        Map<String, Object> campos = new HashMap<>();
        eventos.list.getFirst().getKeyValuePairs().forEach(par -> campos.put(par.key, par.value));
        return campos;
    }

    @Test
    void unaLlamadaExitosa_registraLosDatosPedidos() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()));

        assertThat(controller.operar(7, "secreta", principal)).isEqualTo("listo");

        assertThat(registro())
                .containsEntry("usuario", principal.getUsuarioId().toString())
                .containsEntry("metodo", "ControllerDePrueba.operar")
                .containsEntry("parametros", Map.of("id", 7, "password", "***"))
                .containsEntry("resultado", "OK")
                .containsKey("duracionMs");
        assertThat(eventos.list.getFirst().getInstant()).isNotNull();
    }

    @Test
    void unaLlamadaFallida_registraElTipoYPropagaElError() {
        assertThatThrownBy(() -> controller.fallar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("dato interno");

        assertThat(registro())
                .containsEntry("usuario", "anonimo")
                .containsEntry("resultado", "IllegalStateException");
        assertThat(registro().toString()).doesNotContain("dato interno");
    }

    @Test
    void unParametroNoConvertible_noRompeLaLlamada() {
        assertThat(controller.recibir(new NoConvertible())).isEqualTo("listo");

        assertThat(registro()).containsEntry("parametros", Map.of("valor", "[no disponible]"));
    }

    @RestController
    static class ControllerDePrueba {
        public String operar(int id, String password, UsuarioPrincipal principal) {
            return "listo";
        }

        public String fallar() {
            throw new IllegalStateException("dato interno");
        }

        public String recibir(NoConvertible valor) {
            return "listo";
        }
    }

    static class NoConvertible {
        public String getDato() {
            throw new IllegalStateException("no se puede leer");
        }
    }
}
