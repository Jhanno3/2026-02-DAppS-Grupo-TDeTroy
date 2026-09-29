package com.tdetroy.valuacion.repositories;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@inheritDoc}
 *
 * <p>Consume la API oficial de Football-Data.org (constitution.md §1) vía {@code GET
 * /v4/matches?dateFrom=&dateTo=&status=FINISHED} y traduce su respuesta a {@link PartidoCrudo}, el
 * modelo interno de transporte — {@code RendimientoService} (tasks.md T3.5) nunca ve un {@code
 * PartidoExternoDto} ni ninguna otra forma propia de esta API (constitution.md §1/§2).
 *
 * <p>Aísla sus propios errores: cualquier falla de red, HTTP o de parseo se loguea acá y devuelve
 * lista vacía en lugar de propagar la excepción — una caída de Football-Data.org nunca debe tumbar
 * el resto del sistema ni al flujo de WhoScored (constitution.md §1, "Manejo explícito de
 * fallos/indisponibilidad por fuente").
 */
@Repository
public class FootballDataRepositoryImpl implements FixtureRepository {

    private static final Logger log = LoggerFactory.getLogger(FootballDataRepositoryImpl.class);

    private final RestClient restClient;

    public FootballDataRepositoryImpl(
            RestClient.Builder restClientBuilder,
            @Value("${app.football-data.base-url}") String baseUrl,
            @Value("${app.football-data.api-key}") String apiKey) {
        this.restClient =
                restClientBuilder.baseUrl(baseUrl).defaultHeader("X-Auth-Token", apiKey).build();
    }

    @Override
    public List<PartidoCrudo> obtenerPartidosDisputados(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        try {
            RespuestaPartidos respuesta =
                    restClient
                            .get()
                            .uri(
                                    uriBuilder ->
                                            uriBuilder
                                                    .path("/v4/matches")
                                                    .queryParam("dateFrom", desde)
                                                    .queryParam("dateTo", hasta)
                                                    .queryParam("status", "FINISHED")
                                                    .build())
                            .retrieve()
                            .body(RespuestaPartidos.class);
            return traducir(respuesta);
        } catch (RestClientException | IllegalStateException ex) {
            log.error(
                    "Fallo al consultar Football-Data.org para el rango {} a {}: {}",
                    desde,
                    hasta,
                    ex.getMessage(),
                    ex);
            return List.of();
        }
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        Objects.requireNonNull(desde, "desde no puede ser null");
        Objects.requireNonNull(hasta, "hasta no puede ser null");
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException(
                    "hasta (%s) no puede ser anterior a desde (%s)".formatted(hasta, desde));
        }
    }

    private static List<PartidoCrudo> traducir(RespuestaPartidos respuesta) {
        if (respuesta == null || respuesta.matches() == null) {
            return List.of();
        }
        return respuesta.matches().stream().map(FootballDataRepositoryImpl::traducir).toList();
    }

    private static PartidoCrudo traducir(PartidoExternoDto dto) {
        MarcadorTiempoDto tiempoCompleto = dto.score() == null ? null : dto.score().fullTime();
        return new PartidoCrudo(
                String.valueOf(dto.id()),
                dto.utcDate().atZone(ZoneOffset.UTC).toLocalDate(),
                dto.status(),
                dto.homeTeam() == null ? null : dto.homeTeam().name(),
                dto.awayTeam() == null ? null : dto.awayTeam().name(),
                tiempoCompleto == null ? null : tiempoCompleto.home(),
                tiempoCompleto == null ? null : tiempoCompleto.away());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaPartidos(List<PartidoExternoDto> matches) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PartidoExternoDto(
            long id,
            Instant utcDate,
            String status,
            EquipoDto homeTeam,
            EquipoDto awayTeam,
            MarcadorDto score) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EquipoDto(long id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MarcadorDto(MarcadorTiempoDto fullTime) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MarcadorTiempoDto(Integer home, Integer away) {}
}
