package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.dto.request.ActualizarMetricasRequest;
import com.tdetroy.valuacion.dto.response.ConfiguracionMetricaResponse;
import com.tdetroy.valuacion.entity.PesoMetricaEntity;
import com.tdetroy.valuacion.entity.PesoMetricaId;
import com.tdetroy.valuacion.model.PesoMetrica;
import com.tdetroy.valuacion.model.SentidoMetrica;
import com.tdetroy.valuacion.repositories.PesoMetricaRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación de {@link ConfiguracionMetricaService}. */
@Service
@Transactional
public class ConfiguracionMetricaServiceImpl implements ConfiguracionMetricaService {

    private static final String ACCION_ACTUALIZACION = "ACTUALIZAR_PESOS_METRICA";
    private static final String ENTIDAD = "PESOS_METRICA";
    public static final UUID ENTIDAD_ID = UUID.fromString("00000000-0000-0000-0000-000000000e70");

    public static final List<String> POSICIONES_VALIDAS =
            List.of("Delantero", "Centrocampista", "Defensor", "Arquero");

    private static final Map<String, String> NOMBRES_LEGIBLES =
            Map.of(
                    "goals", "Goles",
                    "assists", "Asistencias",
                    "manOfTheMatch", "Figura del partido",
                    "apps", "Partidos jugados",
                    "minsPlayed", "Minutos jugados",
                    "yellowCard", "Tarjetas amarillas",
                    "redCard", "Tarjetas rojas");

    private final PesoMetricaRepository pesoMetricaRepository;
    private final AuditoriaService auditoriaService;

    public ConfiguracionMetricaServiceImpl(
            PesoMetricaRepository pesoMetricaRepository, AuditoriaService auditoriaService) {
        this.pesoMetricaRepository = pesoMetricaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionMetricaResponse> obtenerConfiguracion() {
        List<PesoMetricaEntity> todas = pesoMetricaRepository.findAll();
        Map<String, List<PesoMetricaEntity>> agrupadas =
                todas.stream()
                        .collect(
                                Collectors.groupingBy(
                                        PesoMetricaEntity::getClave,
                                        LinkedHashMap::new,
                                        Collectors.toList()));

        List<ConfiguracionMetricaResponse> response = new ArrayList<>();
        for (Map.Entry<String, List<PesoMetricaEntity>> entry : agrupadas.entrySet()) {
            String clave = entry.getKey();
            List<PesoMetricaEntity> entidades = entry.getValue();

            PesoMetricaEntity general =
                    entidades.stream()
                            .filter(
                                    e ->
                                            PesoMetrica.POSICION_GENERAL.equalsIgnoreCase(
                                                    e.getPosicion()))
                            .findFirst()
                            .orElse(entidades.getFirst());

            Map<String, BigDecimal> porPosicion = new LinkedHashMap<>();
            for (String pos : POSICIONES_VALIDAS) {
                entidades.stream()
                        .filter(e -> pos.equalsIgnoreCase(e.getPosicion()))
                        .findFirst()
                        .ifPresent(e -> porPosicion.put(pos, e.getPeso()));
            }

            response.add(
                    new ConfiguracionMetricaResponse(
                            clave,
                            NOMBRES_LEGIBLES.getOrDefault(clave, clave),
                            general.getSentido(),
                            general.isActivo(),
                            general.getPeso(),
                            porPosicion));
        }
        return response;
    }

    @Override
    public List<ConfiguracionMetricaResponse> actualizarConfiguracion(
            ActualizarMetricasRequest request, UUID actorId) {
        for (ActualizarMetricasRequest.ItemActualizarMetrica item : request.metricas()) {
            // Actualizar o crear fila GENERAL
            PesoMetricaEntity general =
                    pesoMetricaRepository
                            .findById(new PesoMetricaId(item.clave(), PesoMetrica.POSICION_GENERAL))
                            .orElseGet(
                                    () -> {
                                        PesoMetricaEntity nueva = new PesoMetricaEntity();
                                        nueva.setClave(item.clave());
                                        nueva.setPosicion(PesoMetrica.POSICION_GENERAL);
                                        nueva.setSentido(SentidoMetrica.POSITIVO);
                                        return nueva;
                                    });
            general.setPeso(item.pesoGeneral());
            general.setActivo(item.activo());
            pesoMetricaRepository.save(general);

            // Actualizar o crear filas por posición
            if (item.pesosPorPosicion() != null) {
                for (Map.Entry<String, BigDecimal> posEntry : item.pesosPorPosicion().entrySet()) {
                    String pos = posEntry.getKey();
                    BigDecimal pesoPos = posEntry.getValue();
                    if (pesoPos == null) continue;

                    PesoMetricaEntity posEntity =
                            pesoMetricaRepository
                                    .findById(new PesoMetricaId(item.clave(), pos))
                                    .orElseGet(
                                            () -> {
                                                PesoMetricaEntity nueva = new PesoMetricaEntity();
                                                nueva.setClave(item.clave());
                                                nueva.setPosicion(pos);
                                                nueva.setSentido(general.getSentido());
                                                return nueva;
                                            });
                    posEntity.setPeso(pesoPos);
                    posEntity.setActivo(item.activo());
                    posEntity.setSentido(general.getSentido());
                    pesoMetricaRepository.save(posEntity);
                }
            }
        }

        auditoriaService.registrar(
                actorId, ACCION_ACTUALIZACION, ENTIDAD, ENTIDAD_ID, null, request);

        return obtenerConfiguracion();
    }
}
