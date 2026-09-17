package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Cubre la invariante propia de {@link RegistroAuditoria} (plan.md §2.8/§10): campos siempre
 * obligatorios salvo {@code actorId} (null = SISTEMA) y {@code valoresAntes} (null = sin estado
 * previo, ej. alta de una entidad nueva).
 */
class RegistroAuditoriaTest {

    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID ENTIDAD_ID = UUID.randomUUID();
    private static final String ACCION = "BAJA_JUGADOR";
    private static final String ENTIDAD_AFECTADA = "Jugador";
    private static final String JSON_ANTES = "{\"estado\":\"ACTIVO\"}";
    private static final String JSON_DESPUES = "{\"estado\":\"INACTIVO\"}";
    private static final Instant FECHA_FIJA = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void registroValidoConActor_construyeCorrectamenteYGettersDevuelvenLoEsperado() {
        RegistroAuditoria registro = RegistroAuditoria.registrar(
                ACTOR_ID, ACCION, ENTIDAD_AFECTADA, ENTIDAD_ID, JSON_ANTES, JSON_DESPUES, FECHA_FIJA);

        assertThat(registro.getId()).isNotNull();
        assertThat(registro.getActorId()).isEqualTo(ACTOR_ID);
        assertThat(registro.getAccion()).isEqualTo(ACCION);
        assertThat(registro.getEntidadAfectada()).isEqualTo(ENTIDAD_AFECTADA);
        assertThat(registro.getEntidadId()).isEqualTo(ENTIDAD_ID);
        assertThat(registro.getValoresAntes()).isEqualTo(JSON_ANTES);
        assertThat(registro.getValoresDespues()).isEqualTo(JSON_DESPUES);
        assertThat(registro.getFecha()).isEqualTo(FECHA_FIJA);
    }

    @Test
    void actorIdNulo_representaSistemaYEsValido() {
        RegistroAuditoria registro = RegistroAuditoria.registrar(
                null, "RECALCULO_AUTOMATICO_COTIZACION", "Jugador", ENTIDAD_ID, null, JSON_DESPUES, FECHA_FIJA);

        assertThat(registro.getActorId()).isNull();
    }

    @Test
    void valoresAntesNulo_esValido_porEjemploUnAltaSinEstadoPrevio() {
        RegistroAuditoria registro = RegistroAuditoria.registrar(
                ACTOR_ID, "ALTA_JUGADOR", ENTIDAD_AFECTADA, ENTIDAD_ID, null, JSON_DESPUES, FECHA_FIJA);

        assertThat(registro.getValoresAntes()).isNull();
    }

    @Test
    void accionNula_lanzaExcepcion() {
        assertThatNullPointerException().isThrownBy(() -> RegistroAuditoria.registrar(
                ACTOR_ID, null, ENTIDAD_AFECTADA, ENTIDAD_ID, JSON_ANTES, JSON_DESPUES, FECHA_FIJA));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void accionVaciaOBlanco_lanzaExcepcion(String accionInvalida) {
        assertThatIllegalArgumentException().isThrownBy(() -> RegistroAuditoria.registrar(
                ACTOR_ID, accionInvalida, ENTIDAD_AFECTADA, ENTIDAD_ID, JSON_ANTES, JSON_DESPUES, FECHA_FIJA));
    }

    @Test
    void entidadAfectadaNula_lanzaExcepcion() {
        assertThatNullPointerException().isThrownBy(() -> RegistroAuditoria.registrar(
                ACTOR_ID, ACCION, null, ENTIDAD_ID, JSON_ANTES, JSON_DESPUES, FECHA_FIJA));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void entidadAfectadaVaciaOBlanco_lanzaExcepcion(String entidadInvalida) {
        assertThatIllegalArgumentException().isThrownBy(() -> RegistroAuditoria.registrar(
                ACTOR_ID, ACCION, entidadInvalida, ENTIDAD_ID, JSON_ANTES, JSON_DESPUES, FECHA_FIJA));
    }

    @Test
    void entidadIdNulo_lanzaExcepcion() {
        assertThatNullPointerException().isThrownBy(() -> RegistroAuditoria.registrar(
                ACTOR_ID, ACCION, ENTIDAD_AFECTADA, null, JSON_ANTES, JSON_DESPUES, FECHA_FIJA));
    }

    @Test
    void valoresDespuesNulo_lanzaExcepcion() {
        assertThatNullPointerException().isThrownBy(() -> RegistroAuditoria.registrar(
                ACTOR_ID, ACCION, ENTIDAD_AFECTADA, ENTIDAD_ID, JSON_ANTES, null, FECHA_FIJA));
    }

    @Test
    void fechaNula_lanzaExcepcion() {
        assertThatNullPointerException().isThrownBy(() -> RegistroAuditoria.registrar(
                ACTOR_ID, ACCION, ENTIDAD_AFECTADA, ENTIDAD_ID, JSON_ANTES, JSON_DESPUES, null));
    }

    @Test
    void registrarSinFechaExplicita_usaInstanteActual() {
        Instant antes = Instant.now();
        RegistroAuditoria registro =
                RegistroAuditoria.registrar(ACTOR_ID, ACCION, ENTIDAD_AFECTADA, ENTIDAD_ID, JSON_ANTES, JSON_DESPUES);
        Instant despues = Instant.now();

        assertThat(registro.getFecha()).isBetween(antes, despues);
    }

    @Test
    void dosRegistrosRegistradosPorSeparadoTienenIdsDistintos() {
        RegistroAuditoria uno = RegistroAuditoria.registrar(
                ACTOR_ID, ACCION, ENTIDAD_AFECTADA, ENTIDAD_ID, JSON_ANTES, JSON_DESPUES, FECHA_FIJA);
        RegistroAuditoria otro = RegistroAuditoria.registrar(
                ACTOR_ID, ACCION, ENTIDAD_AFECTADA, ENTIDAD_ID, JSON_ANTES, JSON_DESPUES, FECHA_FIJA);

        assertThat(uno.getId()).isNotEqualTo(otro.getId());
    }
}
