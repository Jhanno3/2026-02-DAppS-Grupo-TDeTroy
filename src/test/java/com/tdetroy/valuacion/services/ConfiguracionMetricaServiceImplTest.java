package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.dto.request.ActualizarMetricasRequest;
import com.tdetroy.valuacion.dto.response.ConfiguracionMetricaResponse;
import com.tdetroy.valuacion.entity.PesoMetricaEntity;
import com.tdetroy.valuacion.entity.PesoMetricaId;
import com.tdetroy.valuacion.model.PesoMetrica;
import com.tdetroy.valuacion.model.SentidoMetrica;
import com.tdetroy.valuacion.repositories.PesoMetricaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfiguracionMetricaServiceImplTest {

    @Mock private PesoMetricaRepository pesoMetricaRepository;

    @Mock private AuditoriaService auditoriaService;

    private ConfiguracionMetricaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ConfiguracionMetricaServiceImpl(pesoMetricaRepository, auditoriaService);
    }

    @Test
    void obtenerConfiguracion_agrupaPorClaveYDevuelveGeneralYPorPosicion() {
        PesoMetricaEntity general = new PesoMetricaEntity();
        general.setClave("goals");
        general.setPosicion(PesoMetrica.POSICION_GENERAL);
        general.setPeso(new BigDecimal("0.5000"));
        general.setActivo(true);
        general.setSentido(SentidoMetrica.POSITIVO);

        PesoMetricaEntity delantero = new PesoMetricaEntity();
        delantero.setClave("goals");
        delantero.setPosicion("Delantero");
        delantero.setPeso(new BigDecimal("0.5000"));
        delantero.setActivo(true);
        delantero.setSentido(SentidoMetrica.POSITIVO);

        PesoMetricaEntity defensor = new PesoMetricaEntity();
        defensor.setClave("goals");
        defensor.setPosicion("Defensor");
        defensor.setPeso(new BigDecimal("1.5000"));
        defensor.setActivo(true);
        defensor.setSentido(SentidoMetrica.POSITIVO);

        when(pesoMetricaRepository.findAll()).thenReturn(List.of(general, delantero, defensor));

        List<ConfiguracionMetricaResponse> resultado = service.obtenerConfiguracion();

        assertThat(resultado).hasSize(1);
        ConfiguracionMetricaResponse item = resultado.getFirst();
        assertThat(item.clave()).isEqualTo("goals");
        assertThat(item.nombreLegible()).isEqualTo("Goles");
        assertThat(item.pesoGeneral()).isEqualByComparingTo("0.5000");
        assertThat(item.activo()).isTrue();
        assertThat(item.pesosPorPosicion()).containsEntry("Delantero", new BigDecimal("0.5000"));
        assertThat(item.pesosPorPosicion()).containsEntry("Defensor", new BigDecimal("1.5000"));
    }

    @Test
    void actualizarConfiguracion_guardaEntidadesYRegistraAuditoria() {
        UUID actorId = UUID.randomUUID();

        when(pesoMetricaRepository.findById(
                        new PesoMetricaId("goals", PesoMetrica.POSICION_GENERAL)))
                .thenReturn(Optional.empty());
        when(pesoMetricaRepository.findById(new PesoMetricaId("goals", "Delantero")))
                .thenReturn(Optional.empty());

        ActualizarMetricasRequest request =
                new ActualizarMetricasRequest(
                        List.of(
                                new ActualizarMetricasRequest.ItemActualizarMetrica(
                                        "goals",
                                        new BigDecimal("0.2500"),
                                        true,
                                        Map.of("Delantero", new BigDecimal("0.2500")))));

        service.actualizarConfiguracion(request, actorId);

        ArgumentCaptor<PesoMetricaEntity> captor = ArgumentCaptor.forClass(PesoMetricaEntity.class);
        verify(pesoMetricaRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());

        List<PesoMetricaEntity> guardados = captor.getAllValues();
        assertThat(guardados)
                .anyMatch(
                        e ->
                                e.getClave().equals("goals")
                                        && e.getPosicion().equals(PesoMetrica.POSICION_GENERAL)
                                        && e.getPeso().compareTo(new BigDecimal("0.2500")) == 0);
        assertThat(guardados)
                .anyMatch(
                        e ->
                                e.getClave().equals("goals")
                                        && e.getPosicion().equals("Delantero")
                                        && e.getPeso().compareTo(new BigDecimal("0.2500")) == 0);

        verify(auditoriaService)
                .registrar(
                        eq(actorId),
                        eq("ACTUALIZAR_PESOS_METRICA"),
                        eq("PESOS_METRICA"),
                        eq(ConfiguracionMetricaServiceImpl.ENTIDAD_ID),
                        eq(null),
                        eq(request));
    }
}
