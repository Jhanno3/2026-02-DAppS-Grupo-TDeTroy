package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tdetroy.valuacion.model.RegistroAuditoria;
import com.tdetroy.valuacion.repositories.RegistroAuditoriaRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link AuditoriaServiceImpl} es responsable de serializar {@code valoresAntes}/
 * {@code valoresDespues} a JSON antes de delegar en {@link RegistroAuditoriaRepository} — se usa
 * un {@link ObjectMapper} real (no mockeado, es una clase simple sin dependencias externas) para
 * verificar el contenido serializado de punta a punta.
 */
@ExtendWith(MockitoExtension.class)
class AuditoriaServiceImplTest {

    private record EstadoJugador(String estado) {
    }

    @Mock
    private RegistroAuditoriaRepository repository;

    private AuditoriaServiceImpl service;

    private final UUID actorId = UUID.randomUUID();
    private final UUID entidadId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AuditoriaServiceImpl(repository, new ObjectMapper());
    }

    @Test
    void registrarConValoresAntesYDespues_guardaConLosCamposYElJsonEsperado() {
        service.registrar(
                actorId, "BAJA_JUGADOR", "Jugador", entidadId, new EstadoJugador("ACTIVO"),
                new EstadoJugador("INACTIVO"));

        RegistroAuditoria guardado = capturarGuardado();
        assertThat(guardado.getActorId()).isEqualTo(actorId);
        assertThat(guardado.getAccion()).isEqualTo("BAJA_JUGADOR");
        assertThat(guardado.getEntidadAfectada()).isEqualTo("Jugador");
        assertThat(guardado.getEntidadId()).isEqualTo(entidadId);
        assertThat(guardado.getValoresAntes()).contains("\"estado\"").contains("ACTIVO");
        assertThat(guardado.getValoresDespues()).contains("\"estado\"").contains("INACTIVO");
    }

    @Test
    void registrarConValoresAntesNulo_guardaConValoresAntesNulo() {
        service.registrar(actorId, "ALTA_JUGADOR", "Jugador", entidadId, null, new EstadoJugador("ACTIVO"));

        RegistroAuditoria guardado = capturarGuardado();
        assertThat(guardado.getValoresAntes()).isNull();
        assertThat(guardado.getValoresDespues()).contains("ACTIVO");
    }

    @Test
    void registrarConActorIdNulo_guardaConActorIdNulo_representaSistema() {
        service.registrar(
                null, "RECALCULO_AUTOMATICO_COTIZACION", "Jugador", entidadId, null,
                new EstadoJugador("recalculado"));

        assertThat(capturarGuardado().getActorId()).isNull();
    }

    @Test
    void registrarConValoresDespuesNulo_lanzaExcepcionYNoGuardaNada() {
        assertThatNullPointerException().isThrownBy(() -> service.registrar(
                actorId, "BAJA_JUGADOR", "Jugador", entidadId, new EstadoJugador("ACTIVO"), null));

        verify(repository, never()).save(any());
    }

    @Test
    void constructorNoInteractuaConElRepositorioNiElObjectMapper() {
        verifyNoInteractions(repository);
    }

    private RegistroAuditoria capturarGuardado() {
        ArgumentCaptor<RegistroAuditoria> captor = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
