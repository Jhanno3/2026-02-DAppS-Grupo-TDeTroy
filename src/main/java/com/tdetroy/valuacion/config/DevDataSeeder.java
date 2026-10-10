package com.tdetroy.valuacion.config;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.entity.UsuarioEntity;
import com.tdetroy.valuacion.model.RolUsuario;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import com.tdetroy.valuacion.services.JugadorService;
import com.tdetroy.valuacion.services.UsuarioService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Carga datos de prueba al levantar la app en desarrollo local (requisito de infra de Entrega 2,
 * sin task propia en tasks.md). Gateado por {@code @Profile("dev")} -- nunca corre en {@code test}
 * (surefire/failsafe fuerzan {@code spring.profiles.active=test}, ver {@code pom.xml}) ni en ningun
 * ambiente donde se fije explicitamente otro profile (prod, etc.); {@code dev} es el profile por
 * defecto de {@code application.properties} (@code spring.profiles.default}), asi que un {@code mvn
 * spring-boot:run} o {@code java -jar} local sin variables de entorno adicionales lo activa solo.
 *
 * <p>Idempotente: sólo siembra si el catálogo de jugadores está vacío (chequeo único, cubre el caso
 * común de reiniciar la app contra la misma base ya sembrada) -- nunca duplica datos en cada
 * reinicio.
 *
 * <p>El usuario {@code ADMIN} se crea escribiendo directamente {@link UsuarioRepository} en vez de
 * {@link UsuarioService#registrar}: esa vía de alta pública deliberadamente sólo crea cuentas
 * {@link RolUsuario#USER} (ver su javadoc, "no hay alta pública de ADMIN en el MVP") -- es una
 * regla de negocio real, no una limitación técnica, así que este seeder la bypassea explícitamente
 * acá (herramienta de desarrollo, nunca un flujo de negocio ni un endpoint) en vez de debilitarla
 * para el resto de la app.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final String PASSWORD_DEV = "Password123!";

    private final JugadorRepository jugadorRepository;
    private final JugadorService jugadorService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
            JugadorRepository jugadorRepository,
            JugadorService jugadorService,
            UsuarioRepository usuarioRepository,
            UsuarioService usuarioService,
            PasswordEncoder passwordEncoder) {
        this.jugadorRepository = jugadorRepository;
        this.jugadorService = jugadorService;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (jugadorRepository.count() > 0) {
            log.info("Datos de prueba: el catálogo ya tiene jugadores, se omite la carga.");
            return;
        }

        seedUsuarios();
        seedJugadores();
        log.info(
                "Datos de prueba cargados: usuario admin@example.com / user@example.com"
                        + " (password '{}'), catálogo de jugadores de ejemplo.",
                PASSWORD_DEV);
    }

    private void seedUsuarios() {
        usuarioService.registrar("user@example.com", passwordEncoder.encode(PASSWORD_DEV));

        Usuario admin =
                Usuario.reconstruir(
                        UUID.randomUUID(),
                        "admin@example.com",
                        passwordEncoder.encode(PASSWORD_DEV),
                        RolUsuario.ADMIN,
                        Monetario.CERO,
                        Instant.now());
        usuarioRepository.save(UsuarioEntity.desde(admin));
    }

    private void seedJugadores() {
        jugadorService.darAlta(
                "Leonel Messi",
                "Inter Miami CF",
                "Delantero",
                LocalDate.of(1987, 6, 24),
                "Argentina",
                null);
        jugadorService.darAlta(
                "Kylian Mbappé",
                "Real Madrid CF",
                "Delantero",
                LocalDate.of(1998, 12, 20),
                "Francia",
                null);
        jugadorService.darAlta(
                "Erling Haaland",
                "Manchester City",
                "Delantero",
                LocalDate.of(2000, 7, 21),
                "Noruega",
                null);
    }
}
