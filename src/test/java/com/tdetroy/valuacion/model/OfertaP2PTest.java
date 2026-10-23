package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Cubre la invariante propia de {@link OfertaP2P} (plan.md §2.7, §9.3, constitution.md §2): "nunca
 * se vende más cantidad que la publicada" (spec.md UC-17), protegida en {@link
 * #ejecutarContra(int)}, no sólo en {@code OfertaService} (T6.4), y el vencimiento por horas fijado
 * libremente por el vendedor (UC-17), protegido en {@link #estaVencida(Instant)}.
 */
class OfertaP2PTest {

    private static final UUID VENDEDOR_ID = UUID.randomUUID();
    private static final UUID JUGADOR_ID = UUID.randomUUID();
    private static final BigDecimal PRECIO_UNITARIO = new BigDecimal("50.00");

    private static OfertaP2P ofertaValida(int cantidadPublicada) {
        return OfertaP2P.publicar(VENDEDOR_ID, JUGADOR_ID, cantidadPublicada, PRECIO_UNITARIO, 24L);
    }

    // ---- publicar ----

    @Test
    void publicar_creaAbiertaConCantidadDisponibleIgualALaPublicada() {
        OfertaP2P oferta = ofertaValida(10);

        assertThat(oferta.getId()).isNotNull();
        assertThat(oferta.getVendedorId()).isEqualTo(VENDEDOR_ID);
        assertThat(oferta.getJugadorId()).isEqualTo(JUGADOR_ID);
        assertThat(oferta.getCantidadPublicada()).isEqualTo(10);
        assertThat(oferta.getCantidadDisponible()).isEqualTo(10);
        assertThat(oferta.getPrecioUnitario()).isEqualTo(PRECIO_UNITARIO);
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ABIERTA);
        assertThat(oferta.getFechaCreacion()).isNotNull();
        assertThat(oferta.getFechaVencimiento()).isAfter(oferta.getFechaCreacion());
    }

    @Test
    void publicar_conVendedorIdNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(() -> OfertaP2P.publicar(null, JUGADOR_ID, 10, PRECIO_UNITARIO, 24L));
    }

    @Test
    void publicar_conJugadorIdNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(() -> OfertaP2P.publicar(VENDEDOR_ID, null, 10, PRECIO_UNITARIO, 24L));
    }

    @Test
    void publicar_conFechaCreacionNula_lanzaExcepcion() {
        Instant fechaVencimiento = Instant.parse("2026-01-16T10:00:00Z");
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                OfertaP2P.publicar(
                                        VENDEDOR_ID,
                                        JUGADOR_ID,
                                        10,
                                        PRECIO_UNITARIO,
                                        null,
                                        fechaVencimiento));
    }

    @Test
    void publicar_conFechaCreacionYVencimientoExplicitas_lasRespeta() {
        Instant fechaCreacion = Instant.parse("2026-01-15T10:00:00Z");
        Instant fechaVencimiento = Instant.parse("2026-01-16T10:00:00Z");

        OfertaP2P oferta =
                OfertaP2P.publicar(
                        VENDEDOR_ID,
                        JUGADOR_ID,
                        10,
                        PRECIO_UNITARIO,
                        fechaCreacion,
                        fechaVencimiento);

        assertThat(oferta.getFechaCreacion()).isEqualTo(fechaCreacion);
        assertThat(oferta.getFechaVencimiento()).isEqualTo(fechaVencimiento);
    }

    @Test
    void publicar_conFechaVencimientoNoPosteriorAFechaCreacion_lanzaExcepcion() {
        Instant fechaCreacion = Instant.parse("2026-01-15T10:00:00Z");

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                OfertaP2P.publicar(
                                        VENDEDOR_ID,
                                        JUGADOR_ID,
                                        10,
                                        PRECIO_UNITARIO,
                                        fechaCreacion,
                                        fechaCreacion));
    }

    @Test
    void publicar_conCantidadPublicadaCero_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> OfertaP2P.publicar(VENDEDOR_ID, JUGADOR_ID, 0, PRECIO_UNITARIO, 24L));
    }

    @Test
    void publicar_conCantidadPublicadaNegativa_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                OfertaP2P.publicar(
                                        VENDEDOR_ID, JUGADOR_ID, -1, PRECIO_UNITARIO, 24L));
    }

    @Test
    void publicar_conPrecioUnitarioNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(() -> OfertaP2P.publicar(VENDEDOR_ID, JUGADOR_ID, 10, null, 24L));
    }

    @Test
    void publicar_conPrecioUnitarioCero_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                OfertaP2P.publicar(
                                        VENDEDOR_ID, JUGADOR_ID, 10, BigDecimal.ZERO, 24L));
    }

    @Test
    void publicar_conPrecioUnitarioNegativo_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                OfertaP2P.publicar(
                                        VENDEDOR_ID, JUGADOR_ID, 10, new BigDecimal("-1"), 24L));
    }

    @Test
    void publicar_conHorasLimiteCero_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> OfertaP2P.publicar(VENDEDOR_ID, JUGADOR_ID, 10, PRECIO_UNITARIO, 0L));
    }

    @Test
    void publicar_conHorasLimiteNegativa_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                OfertaP2P.publicar(
                                        VENDEDOR_ID, JUGADOR_ID, 10, PRECIO_UNITARIO, -1L));
    }

    @Test
    void dosOfertasPublicadasPorSeparado_tienenIdsDistintos() {
        OfertaP2P una = ofertaValida(5);
        OfertaP2P otra = ofertaValida(5);

        assertThat(una.getId()).isNotEqualTo(otra.getId());
    }

    // ---- ejecutarContra ----

    @Test
    void ejecutarContra_conCantidadMenorALaDisponible_laDescuentaYQuedaAbierta() {
        OfertaP2P oferta = ofertaValida(10);

        int ejecutada = oferta.ejecutarContra(4);

        assertThat(ejecutada).isEqualTo(4);
        assertThat(oferta.getCantidadDisponible()).isEqualTo(6);
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ABIERTA);
    }

    @Test
    void ejecutarContra_conCantidadIgualALaDisponible_laAgotaYCambiaEstadoAAgotada() {
        OfertaP2P oferta = ofertaValida(10);

        oferta.ejecutarContra(10);

        assertThat(oferta.getCantidadDisponible()).isZero();
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.AGOTADA);
    }

    @Test
    void ejecutarContra_variasVecesSeguidas_vaDescontandoAcumulativamente() {
        OfertaP2P oferta = ofertaValida(10);

        oferta.ejecutarContra(3);
        oferta.ejecutarContra(5);

        assertThat(oferta.getCantidadDisponible()).isEqualTo(2);
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ABIERTA);
    }

    @Test
    void ejecutarContra_queSuperaCantidadDisponible_lanzaExcepcionYNoMutaEstado() {
        OfertaP2P oferta = ofertaValida(10);
        oferta.ejecutarContra(6);

        assertThatThrownBy(() -> oferta.ejecutarContra(5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(oferta.getCantidadDisponible()).isEqualTo(4);
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ABIERTA);
    }

    @Test
    void ejecutarContra_sobreOfertaYaAgotada_lanzaExcepcion() {
        OfertaP2P oferta = ofertaValida(10);
        oferta.ejecutarContra(10);

        assertThatIllegalArgumentException().isThrownBy(() -> oferta.ejecutarContra(1));
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.AGOTADA);
    }

    @Test
    void ejecutarContra_cantidadCero_lanzaExcepcion() {
        OfertaP2P oferta = ofertaValida(10);

        assertThatIllegalArgumentException().isThrownBy(() -> oferta.ejecutarContra(0));
    }

    @Test
    void ejecutarContra_cantidadNegativa_lanzaExcepcion() {
        OfertaP2P oferta = ofertaValida(10);

        assertThatIllegalArgumentException().isThrownBy(() -> oferta.ejecutarContra(-1));
    }

    // ---- cancelar ----

    @Test
    void cancelar_dejaCantidadDisponibleEnCeroYEstadoCancelada() {
        OfertaP2P oferta = ofertaValida(10);

        oferta.cancelar();

        assertThat(oferta.getCantidadDisponible()).isZero();
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.CANCELADA);
    }

    @Test
    void cancelar_conVentaParcialPrevia_tambienDejaCantidadDisponibleEnCero() {
        OfertaP2P oferta = ofertaValida(10);
        oferta.ejecutarContra(4);

        oferta.cancelar();

        assertThat(oferta.getCantidadDisponible()).isZero();
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.CANCELADA);
    }

    // ---- estaVencida ----

    @Test
    void estaVencida_conAhoraAntesDeFechaVencimiento_esFalso() {
        Instant fechaCreacion = Instant.parse("2026-01-15T10:00:00Z");
        Instant fechaVencimiento = Instant.parse("2026-01-16T10:00:00Z");
        OfertaP2P oferta =
                OfertaP2P.publicar(
                        VENDEDOR_ID,
                        JUGADOR_ID,
                        10,
                        PRECIO_UNITARIO,
                        fechaCreacion,
                        fechaVencimiento);

        assertThat(oferta.estaVencida(fechaVencimiento.minusSeconds(1))).isFalse();
    }

    @Test
    void estaVencida_conAhoraDespuesDeFechaVencimiento_esTrue() {
        Instant fechaCreacion = Instant.parse("2026-01-15T10:00:00Z");
        Instant fechaVencimiento = Instant.parse("2026-01-16T10:00:00Z");
        OfertaP2P oferta =
                OfertaP2P.publicar(
                        VENDEDOR_ID,
                        JUGADOR_ID,
                        10,
                        PRECIO_UNITARIO,
                        fechaCreacion,
                        fechaVencimiento);

        assertThat(oferta.estaVencida(fechaVencimiento.plusSeconds(1))).isTrue();
    }

    @Test
    void estaVencida_conAhoraNulo_lanzaExcepcion() {
        OfertaP2P oferta = ofertaValida(10);

        assertThatNullPointerException().isThrownBy(() -> oferta.estaVencida(null));
    }
}
