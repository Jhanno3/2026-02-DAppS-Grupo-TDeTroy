package com.tdetroy.valuacion.repositories.support;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para tests de integración de {@code repositories/} contra Postgres real vía Testcontainers
 * (constitution.md §1: prohibido H2 u otro motor in-memory para tests de integración; tasks.md
 * T0.7).
 *
 * <p>{@code @DataJpaTest} arranca sólo el slice de JPA (repositorios Spring Data, {@code
 * EntityManager}, Flyway) — más liviano que un {@code @SpringBootTest} completo, y suficiente
 * porque {@code repositories/} no depende de la capa web ni de seguridad. {@code Replace.NONE}
 * evita que Spring Boot reemplace el datasource configurado por uno embebido (su comportamiento por
 * defecto en este slice), y {@link #datasourceProperties} redirige el datasource al contenedor real
 * levantado acá.
 *
 * <p>El contenedor se arranca una única vez de forma manual (patrón "singleton container"
 * recomendado por Testcontainers para compartirlo entre varias clases de test dentro del mismo
 * proceso de Surefire) en vez de usar {@code @Container}/{@code @Testcontainers}, que reiniciaría
 * un contenedor por cada clase de test que herede de esta base. Lo cierra Testcontainers Ryuk al
 * terminar la JVM, no hace falta detenerlo a mano.
 *
 * <p>La imagen ({@code postgres:18.6-alpine}) es la misma que usa {@code docker-compose.yml} para
 * desarrollo/uso manual, para no tener una tercera versión de Postgres flotando en el proyecto.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public abstract class PostgresIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES =
                new PostgreSQLContainer<>("postgres:18.6-alpine")
                        .withDatabaseName("valuacion_it")
                        .withUsername("valuacion_it")
                        .withPassword("valuacion_it");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
