package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.JugadorConPrecio;
import com.tdetroy.valuacion.model.PaginaMercado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RankingServiceImplTest {

    @Mock private JugadorService jugadorService;

    private RankingServiceImpl rankingService;

    @BeforeEach
    void setUp() {
        rankingService =
                new RankingServiceImpl(
                        jugadorService,
                        List.of(new RankingCotizacionStrategy(), new RankingVariacionStrategy()));
    }

    @Test
    void obtenerRanking_conEstrategiaCotizacion_delegaEnListarMercadoConPrecioDesc() {
        Jugador jugador =
                Jugador.darAlta(
                        "Messi",
                        "Inter Miami",
                        "Delantero",
                        LocalDate.of(1987, 6, 24),
                        "Argentina",
                        "MLS");
        JugadorConPrecio conPrecio =
                new JugadorConPrecio(jugador, new BigDecimal("150.00"), new BigDecimal("5.00"));
        PaginaMercado pagina =
                new PaginaMercado(List.of(conPrecio), 0, 20, 1, 1, List.of("Delantero"));

        when(jugadorService.listarMercado(0, 20, "precio-desc", null, null)).thenReturn(pagina);

        List<JugadorConPrecio> resultado = rankingService.obtenerRanking("cotizacion");

        assertThat(resultado).containsExactly(conPrecio);
        verify(jugadorService).listarMercado(0, 20, "precio-desc", null, null);
    }

    @Test
    void obtenerRanking_conEstrategiaVariacion_delegaEnListarMercadoConSuba() {
        PaginaMercado pagina = new PaginaMercado(List.of(), 0, 20, 0, 0, List.of());
        when(jugadorService.listarMercado(0, 20, "suba", null, null)).thenReturn(pagina);

        List<JugadorConPrecio> resultado = rankingService.obtenerRanking("variacion-semanal");

        assertThat(resultado).isEmpty();
        verify(jugadorService).listarMercado(0, 20, "suba", null, null);
    }

    @Test
    void obtenerRanking_conEstrategiaNulaODesconocida_usaFallbackCotizacion() {
        PaginaMercado pagina = new PaginaMercado(List.of(), 0, 20, 0, 0, List.of());
        when(jugadorService.listarMercado(0, 20, "precio-desc", null, null)).thenReturn(pagina);

        rankingService.obtenerRanking(null);
        rankingService.obtenerRanking("estrategia-inexistente");

        verify(jugadorService, org.mockito.Mockito.times(2))
                .listarMercado(0, 20, "precio-desc", null, null);
    }

    @Test
    void estrategiasRanking_clavesYOrdenesSonCorrectos() {
        RankingCotizacionStrategy cotizacionStrategy = new RankingCotizacionStrategy();
        assertThat(cotizacionStrategy.clave()).isEqualTo("cotizacion");
        assertThat(cotizacionStrategy.ordenMercado()).isEqualTo("precio-desc");

        RankingVariacionStrategy variacionStrategy = new RankingVariacionStrategy();
        assertThat(variacionStrategy.clave()).isEqualTo("variacion-semanal");
        assertThat(variacionStrategy.ordenMercado()).isEqualTo("suba");
    }
}
