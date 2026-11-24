package com.tdetroy.valuacion.services.jugador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.entity.jugador.JugadorEntity;
import com.tdetroy.valuacion.model.jugador.Jugador;
import com.tdetroy.valuacion.model.jugador.JugadorConPrecio;
import com.tdetroy.valuacion.repositories.jugador.FilaRankingProjection;
import com.tdetroy.valuacion.repositories.jugador.JugadorRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankingServiceImplTest {

    @Mock private JugadorRepository jugadorRepository;

    private RankingServiceImpl rankingService;

    @BeforeEach
    void setUp() {
        rankingService =
                new RankingServiceImpl(
                        jugadorRepository,
                        List.of(
                                new EstrategiaRankingCotizacion(jugadorRepository),
                                new EstrategiaRankingVariacion(jugadorRepository)));
    }

    @Test
    void obtenerRanking_conservaElOrdenDeLaConsultaYCalculaLaVariacion() {
        Jugador primero = jugador("Primero");
        Jugador segundo = jugador("Segundo");
        List<FilaRankingProjection> filas =
                List.of(fila(primero, "150.00", "100.00"), fila(segundo, null, null));
        when(jugadorRepository.buscarTopPorCotizacion(20)).thenReturn(filas);
        when(jugadorRepository.findAllById(any()))
                .thenReturn(List.of(JugadorEntity.desde(segundo), JugadorEntity.desde(primero)));

        List<JugadorConPrecio> resultado = rankingService.obtenerRanking("cotizacion");

        assertThat(resultado)
                .extracting(elemento -> elemento.jugador().getNombre())
                .containsExactly("Primero", "Segundo");
        assertThat(resultado.get(0).precio()).isEqualByComparingTo("150.00");
        assertThat(resultado.get(0).variacionPorcentual()).isEqualByComparingTo("50");
        assertThat(resultado.get(1).precio()).isNull();
        assertThat(resultado.get(1).variacionPorcentual()).isNull();
    }

    @Test
    void obtenerRanking_conEstrategiaVariacion_consultaPorVariacion() {
        rankingService.obtenerRanking(" Variacion-Semanal ");

        verify(jugadorRepository).buscarTopPorVariacion(20);
    }

    @Test
    void obtenerRanking_conEstrategiaNulaODesconocida_usaCotizacion() {
        rankingService.obtenerRanking(null);
        rankingService.obtenerRanking("estrategia-inexistente");

        verify(jugadorRepository, times(2)).buscarTopPorCotizacion(20);
    }

    private static Jugador jugador(String nombre) {
        return Jugador.darAlta(
                nombre, "Club", "Delantero", LocalDate.of(1987, 6, 24), "Argentina", "Liga");
    }

    private static FilaRankingProjection fila(Jugador jugador, String vigente, String inicial) {
        FilaRankingProjection fila = mock(FilaRankingProjection.class);
        when(fila.getJugadorId()).thenReturn(jugador.getId());
        if (vigente != null) {
            when(fila.getValorVigente()).thenReturn(new BigDecimal(vigente));
            when(fila.getFechaVigente()).thenReturn(Instant.parse("2026-02-01T00:00:00Z"));
            when(fila.getValorInicial()).thenReturn(new BigDecimal(inicial));
            when(fila.getFechaInicial()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        }
        return fila;
    }
}
