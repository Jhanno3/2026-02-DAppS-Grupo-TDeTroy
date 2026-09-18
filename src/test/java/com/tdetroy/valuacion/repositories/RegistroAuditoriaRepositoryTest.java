package com.tdetroy.valuacion.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.tdetroy.valuacion.model.RegistroAuditoria;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Test de integración de {@link RegistroAuditoriaRepository} contra Postgres real (tasks.md T0.7,
 * constitution.md §1/§4). {@code RegistroAuditoriaRepository} todavía no tiene métodos de consulta
 * propios (se escribe siempre a través de {@code AuditoriaService} y se consulta directamente en
 * base, plan.md §10), así que este test cubre lo que hoy expone: persistir un {@link
 * RegistroAuditoria} válido vía {@link RegistroAuditoria#registrar} y leerlo de vuelta con los
 * mismos valores (incluidas las columnas {@code jsonb}), y que la entidad es append-only también a
 * nivel de persistencia.
 *
 * <p>Las columnas {@code jsonb} de Postgres no preservan el texto original: lo parsean y lo
 * reserializan (ej. agregan un espacio después de cada {@code :}), así que comparar {@code
 * valoresDespues} como string exacto es frágil contra la DB real. Se compara con {@link
 * org.skyscreamer.jsonassert.JSONAssert}, que compara semánticamente el JSON — esto es justamente
 * lo que un test contra Postgres real (y no H2) detecta y que un mock nunca hubiera mostrado.
 */
class RegistroAuditoriaRepositoryTest extends PostgresIntegrationTest {

    @Autowired private RegistroAuditoriaRepository registroAuditoriaRepository;

    @Autowired private TestEntityManager entityManager;

    @Test
    void guardarRegistroValido_persisteYSeLeeDeVueltaConLosValoresEsperados() throws JSONException {
        UUID actorId = UUID.randomUUID();
        UUID entidadId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-01-15T10:00:00Z");
        RegistroAuditoria registro =
                RegistroAuditoria.registrar(
                        actorId,
                        "ALTA_JUGADOR",
                        "Jugador",
                        entidadId,
                        null,
                        "{\"tokensEmitidos\":0}",
                        fecha);

        RegistroAuditoria guardado = registroAuditoriaRepository.saveAndFlush(registro);
        entityManager.clear();

        RegistroAuditoria leido =
                registroAuditoriaRepository.findById(guardado.getId()).orElseThrow();

        assertThat(leido.getId()).isEqualTo(registro.getId());
        assertThat(leido.getActorId()).isEqualTo(actorId);
        assertThat(leido.getAccion()).isEqualTo("ALTA_JUGADOR");
        assertThat(leido.getEntidadAfectada()).isEqualTo("Jugador");
        assertThat(leido.getEntidadId()).isEqualTo(entidadId);
        assertThat(leido.getValoresAntes()).isNull();
        assertEquals("{\"tokensEmitidos\":0}", leido.getValoresDespues(), JSONCompareMode.STRICT);
        assertThat(leido.getFecha()).isEqualTo(fecha);
    }

    @Test
    void dosRegistrosGuardadosPorSeparado_persistenComoFilasIndependientesSinSobrescribirse()
            throws JSONException {
        UUID entidadId = UUID.randomUUID();
        RegistroAuditoria primero =
                RegistroAuditoria.registrar(
                        null,
                        "RECALCULO_MANUAL_COTIZACION",
                        "Jugador",
                        entidadId,
                        "{\"valor\":100.00}",
                        "{\"valor\":105.00}");
        RegistroAuditoria segundo =
                RegistroAuditoria.registrar(
                        null,
                        "RECALCULO_MANUAL_COTIZACION",
                        "Jugador",
                        entidadId,
                        "{\"valor\":105.00}",
                        "{\"valor\":110.00}");

        registroAuditoriaRepository.saveAndFlush(primero);
        registroAuditoriaRepository.saveAndFlush(segundo);
        entityManager.clear();

        assertThat(registroAuditoriaRepository.count()).isEqualTo(2);

        RegistroAuditoria primeroLeido =
                registroAuditoriaRepository.findById(primero.getId()).orElseThrow();
        RegistroAuditoria segundoLeido =
                registroAuditoriaRepository.findById(segundo.getId()).orElseThrow();

        assertThat(primeroLeido.getId()).isNotEqualTo(segundoLeido.getId());
        assertEquals(
                "{\"valor\":105.00}", primeroLeido.getValoresDespues(), JSONCompareMode.STRICT);
        assertEquals(
                "{\"valor\":110.00}", segundoLeido.getValoresDespues(), JSONCompareMode.STRICT);
    }
}
