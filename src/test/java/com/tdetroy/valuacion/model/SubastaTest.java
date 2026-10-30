package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdetroy.valuacion.common.exceptions.OperacionSubastaInvalidaException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Cubre {@link Subasta} (UC-18): creación, incrementos admitidos, quién puede pujar y hasta cuándo,
 * reinicio del plazo de 2 minutos, y las transiciones a adjudicada/desierta/cancelada.
 */
class SubastaTest {

    private static final Instant INICIO = Instant.parse("2026-10-29T20:00:00Z");
    private static final Duration DURACION = Duration.ofMinutes(30);

    private UUID vendedorId;
    private UUID postorA;
    private UUID postorB;
    private Subasta subasta;

    @BeforeEach
    void setUp() {
        vendedorId = UUID.randomUUID();
        postorA = UUID.randomUUID();
        postorB = UUID.randomUUID();
        subasta =
                Subasta.crear(
                        vendedorId,
                        UUID.randomUUID(),
                        2,
                        new BigDecimal("100.00"),
                        DURACION,
                        INICIO);
    }

    @Test
    void crear_arrancaActivaSinPujasYVenceAlFinDeLaDuracion() {
        assertThat(subasta.getEstado()).isEqualTo(EstadoSubasta.ACTIVA);
        assertThat(subasta.tienePujas()).isFalse();
        assertThat(subasta.getPrecioActual()).isNull();
        assertThat(subasta.montoComprometido()).isEqualByComparingTo("0.00");
        assertThat(subasta.cierraEn()).isEqualTo(INICIO.plus(DURACION));
    }

    @Test
    void crear_conCantidadPrecioODuracionNoPositivos_lanzaExcepcion() {
        UUID jugadorId = UUID.randomUUID();
        BigDecimal precio = new BigDecimal("10.00");
        assertThatThrownBy(() -> Subasta.crear(vendedorId, jugadorId, 0, precio, DURACION, INICIO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                Subasta.crear(
                                        vendedorId,
                                        jugadorId,
                                        1,
                                        BigDecimal.ZERO,
                                        DURACION,
                                        INICIO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                Subasta.crear(
                                        vendedorId, jugadorId, 1, precio, Duration.ZERO, INICIO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void primeraPuja_puedeIgualarElPrecioInicial() {
        BigDecimal precio =
                subasta.pujar(postorA, IncrementoPuja.PRECIO_INICIAL, INICIO.plusSeconds(10));

        assertThat(precio).isEqualByComparingTo("100.00");
        assertThat(subasta.getMejorPostorId()).isEqualTo(postorA);
        assertThat(subasta.montoComprometido()).isEqualByComparingTo("200.00");
    }

    @Test
    void puja_subeElPorcentajeElegidoSobreElPrecioVigente() {
        subasta.pujar(postorA, IncrementoPuja.CINCO, INICIO.plusSeconds(10));
        assertThat(subasta.getPrecioActual()).isEqualByComparingTo("105.00");

        subasta.pujar(postorB, IncrementoPuja.DIEZ, INICIO.plusSeconds(20));
        assertThat(subasta.getPrecioActual()).isEqualByComparingTo("115.50");

        subasta.pujar(postorA, IncrementoPuja.VEINTE, INICIO.plusSeconds(30));
        // 115.50 × 1.20 = 138.60
        assertThat(subasta.getPrecioActual()).isEqualByComparingTo("138.60");
        assertThat(subasta.getCantidadPujas()).isEqualTo(3);
    }

    @Test
    void puja_redondeaAlCentavo() {
        Subasta conDecimales =
                Subasta.crear(
                        vendedorId,
                        UUID.randomUUID(),
                        1,
                        new BigDecimal("33.33"),
                        DURACION,
                        INICIO);

        // 33.33 × 1.05 = 34.9965 → 35.00
        assertThat(conDecimales.precioConIncremento(IncrementoPuja.CINCO))
                .isEqualByComparingTo("35.00");
    }

    @Test
    void igualarPrecioInicial_conPujasPrevias_lanzaExcepcion() {
        subasta.pujar(postorA, IncrementoPuja.CINCO, INICIO.plusSeconds(10));

        assertThatThrownBy(
                        () ->
                                subasta.pujar(
                                        postorB,
                                        IncrementoPuja.PRECIO_INICIAL,
                                        INICIO.plusSeconds(20)))
                .isInstanceOf(OperacionSubastaInvalidaException.class);
    }

    @Test
    void puja_delVendedor_lanzaExcepcion() {
        assertThatThrownBy(
                        () ->
                                subasta.pujar(
                                        vendedorId, IncrementoPuja.CINCO, INICIO.plusSeconds(10)))
                .isInstanceOf(OperacionSubastaInvalidaException.class)
                .hasMessageContaining("propia subasta");
    }

    @Test
    void puja_delMejorPostorSobreSiMismo_lanzaExcepcion() {
        subasta.pujar(postorA, IncrementoPuja.CINCO, INICIO.plusSeconds(10));

        assertThatThrownBy(
                        () -> subasta.pujar(postorA, IncrementoPuja.DIEZ, INICIO.plusSeconds(20)))
                .isInstanceOf(OperacionSubastaInvalidaException.class)
                .hasMessageContaining("mejor postor");
    }

    @Test
    void cadaPuja_reiniciaElPlazoDeDosMinutos_aunqueYaHayaVencidoLaDuracion() {
        // Primera puja 30s antes de que venza la duración (30 min).
        Instant primera = INICIO.plus(DURACION).minusSeconds(30);
        subasta.pujar(postorA, IncrementoPuja.CINCO, primera);
        assertThat(subasta.cierraEn()).isEqualTo(primera.plus(Subasta.ESPERA_ENTRE_PUJAS));

        // Con la duración ya vencida se sigue pujando mientras no pasen 2 min desde la última.
        Instant despuesDeLaDuracion = INICIO.plus(DURACION).plusSeconds(60);
        subasta.pujar(postorB, IncrementoPuja.CINCO, despuesDeLaDuracion);

        assertThat(subasta.cierraEn())
                .isEqualTo(despuesDeLaDuracion.plus(Subasta.ESPERA_ENTRE_PUJAS));
        assertThat(subasta.debeAdjudicarse(despuesDeLaDuracion.plusSeconds(119))).isFalse();
        assertThat(subasta.debeAdjudicarse(despuesDeLaDuracion.plusSeconds(120))).isTrue();
    }

    @Test
    void puja_despuesDelCierre_lanzaExcepcion() {
        Instant primera = INICIO.plusSeconds(10);
        subasta.pujar(postorA, IncrementoPuja.CINCO, primera);

        assertThatThrownBy(
                        () ->
                                subasta.pujar(
                                        postorB,
                                        IncrementoPuja.CINCO,
                                        primera.plus(Subasta.ESPERA_ENTRE_PUJAS)))
                .isInstanceOf(OperacionSubastaInvalidaException.class);
    }

    @Test
    void sinPujas_alVencerLaDuracion_quedaDesierta_yNoAceptaPujas() {
        Instant vencimiento = INICIO.plus(DURACION);
        assertThat(subasta.quedoDesierta(vencimiento.minusSeconds(1))).isFalse();
        assertThat(subasta.quedoDesierta(vencimiento)).isTrue();
        assertThat(subasta.debeAdjudicarse(vencimiento)).isFalse();

        assertThatThrownBy(() -> subasta.pujar(postorA, IncrementoPuja.CINCO, vencimiento))
                .isInstanceOf(OperacionSubastaInvalidaException.class);

        subasta.declararDesierta(vencimiento);
        assertThat(subasta.getEstado()).isEqualTo(EstadoSubasta.DESIERTA);
    }

    @Test
    void adjudicar_antesDeTiempo_lanzaExcepcion() {
        Instant primera = INICIO.plusSeconds(10);
        subasta.pujar(postorA, IncrementoPuja.CINCO, primera);

        assertThatThrownBy(() -> subasta.adjudicar(primera.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class);

        subasta.adjudicar(primera.plus(Subasta.ESPERA_ENTRE_PUJAS));
        assertThat(subasta.getEstado()).isEqualTo(EstadoSubasta.ADJUDICADA);
    }

    @Test
    void declararDesierta_antesDeVencerOConPujas_lanzaExcepcion() {
        assertThatThrownBy(() -> subasta.declararDesierta(INICIO.plusSeconds(10)))
                .isInstanceOf(IllegalStateException.class);

        subasta.pujar(postorA, IncrementoPuja.CINCO, INICIO.plusSeconds(10));
        Instant vencida = INICIO.plus(DURACION);
        assertThatThrownBy(() -> subasta.declararDesierta(vencida))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelar_soloSiEstaActiva() {
        subasta.pujar(postorA, IncrementoPuja.CINCO, INICIO.plusSeconds(10));
        subasta.cancelar();
        assertThat(subasta.getEstado()).isEqualTo(EstadoSubasta.CANCELADA);

        assertThatThrownBy(subasta::cancelar).isInstanceOf(OperacionSubastaInvalidaException.class);
        assertThatThrownBy(
                        () -> subasta.pujar(postorB, IncrementoPuja.CINCO, INICIO.plusSeconds(20)))
                .isInstanceOf(OperacionSubastaInvalidaException.class);
    }

    @Test
    void incremento_desdePorcentajeInvalido_lanzaExcepcion() {
        assertThat(IncrementoPuja.desdePorcentaje(10)).isEqualTo(IncrementoPuja.DIEZ);
        assertThatThrownBy(() -> IncrementoPuja.desdePorcentaje(15))
                .isInstanceOf(OperacionSubastaInvalidaException.class)
                .hasMessageContaining("fue 15%");
    }
}
