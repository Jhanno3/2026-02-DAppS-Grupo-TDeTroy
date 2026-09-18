package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Cubre la invariante propia de {@link Movimiento} (plan.md §2.6): qué combinación de campos es
 * válida según {@link TipoMovimiento}, y los casos límite exigidos por la "Definición de hecho" de
 * tasks.md (cantidades/montos cero y negativos).
 */
class MovimientoTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID JUGADOR_ID = UUID.randomUUID();
    private static final UUID CONTRAPARTE_ID = UUID.randomUUID();
    private static final BigDecimal MONTO_VALIDO = new BigDecimal("50.00");
    private static final BigDecimal PRECIO_VALIDO = new BigDecimal("10.00");
    private static final Integer CANTIDAD_VALIDA = 5;
    private static final Instant FECHA_FIJA = Instant.parse("2026-01-01T00:00:00Z");

    // ---- RECARGA_SALDO: caso feliz y cada campo que debe quedar en null ----

    @Test
    void recargaSaldoValida_construyeCorrectamenteYGettersDevuelvenLoEsperado() {
        Movimiento mov =
                Movimiento.registrar(
                        USUARIO_ID,
                        null,
                        TipoMovimiento.RECARGA_SALDO,
                        null,
                        null,
                        MONTO_VALIDO,
                        null,
                        FECHA_FIJA);

        assertThat(mov.getId()).isNotNull();
        assertThat(mov.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(mov.getJugadorId()).isNull();
        assertThat(mov.getTipo()).isEqualTo(TipoMovimiento.RECARGA_SALDO);
        assertThat(mov.getCantidad()).isNull();
        assertThat(mov.getPrecioUnitario()).isNull();
        assertThat(mov.getMontoTotal()).isEqualByComparingTo(MONTO_VALIDO);
        assertThat(mov.getContraparteUsuarioId()).isNull();
        assertThat(mov.getFecha()).isEqualTo(FECHA_FIJA);
    }

    @Test
    void recargaSaldoConJugadorIdNoNulo_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        JUGADOR_ID,
                                        TipoMovimiento.RECARGA_SALDO,
                                        null,
                                        null,
                                        MONTO_VALIDO,
                                        null,
                                        FECHA_FIJA));
    }

    @Test
    void recargaSaldoConCantidadNoNula_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        null,
                                        TipoMovimiento.RECARGA_SALDO,
                                        CANTIDAD_VALIDA,
                                        null,
                                        MONTO_VALIDO,
                                        null,
                                        FECHA_FIJA));
    }

    @Test
    void recargaSaldoConPrecioUnitarioNoNulo_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        null,
                                        TipoMovimiento.RECARGA_SALDO,
                                        null,
                                        PRECIO_VALIDO,
                                        MONTO_VALIDO,
                                        null,
                                        FECHA_FIJA));
    }

    @Test
    void recargaSaldoConContraparteNoNula_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        null,
                                        TipoMovimiento.RECARGA_SALDO,
                                        null,
                                        null,
                                        MONTO_VALIDO,
                                        CONTRAPARTE_ID,
                                        FECHA_FIJA));
    }

    @Test
    void recargaSaldoConMontoCero_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        null,
                                        TipoMovimiento.RECARGA_SALDO,
                                        null,
                                        null,
                                        BigDecimal.ZERO,
                                        null,
                                        FECHA_FIJA));
    }

    @Test
    void recargaSaldoConMontoNegativo_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        null,
                                        TipoMovimiento.RECARGA_SALDO,
                                        null,
                                        null,
                                        new BigDecimal("-1.00"),
                                        null,
                                        FECHA_FIJA));
    }

    // ---- Tipos distintos de RECARGA_SALDO: jugadorId/cantidad/precioUnitario obligatorios ----

    @ParameterizedTest
    @EnumSource(
            value = TipoMovimiento.class,
            names = "RECARGA_SALDO",
            mode = EnumSource.Mode.EXCLUDE)
    void tipoConJugadorIdNulo_lanzaExcepcion(TipoMovimiento tipo) {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        null,
                                        tipo,
                                        CANTIDAD_VALIDA,
                                        PRECIO_VALIDO,
                                        MONTO_VALIDO,
                                        contraparteSiP2p(tipo),
                                        FECHA_FIJA));
    }

    @ParameterizedTest
    @EnumSource(
            value = TipoMovimiento.class,
            names = "RECARGA_SALDO",
            mode = EnumSource.Mode.EXCLUDE)
    void tipoConCantidadNula_lanzaExcepcion(TipoMovimiento tipo) {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        JUGADOR_ID,
                                        tipo,
                                        null,
                                        PRECIO_VALIDO,
                                        MONTO_VALIDO,
                                        contraparteSiP2p(tipo),
                                        FECHA_FIJA));
    }

    @ParameterizedTest
    @EnumSource(
            value = TipoMovimiento.class,
            names = "RECARGA_SALDO",
            mode = EnumSource.Mode.EXCLUDE)
    void tipoConPrecioUnitarioNulo_lanzaExcepcion(TipoMovimiento tipo) {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        JUGADOR_ID,
                                        tipo,
                                        CANTIDAD_VALIDA,
                                        null,
                                        MONTO_VALIDO,
                                        contraparteSiP2p(tipo),
                                        FECHA_FIJA));
    }

    @Test
    void cantidadCero_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        JUGADOR_ID,
                                        TipoMovimiento.COMPRA_SISTEMA,
                                        0,
                                        PRECIO_VALIDO,
                                        MONTO_VALIDO,
                                        null,
                                        FECHA_FIJA));
    }

    @Test
    void cantidadNegativa_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        JUGADOR_ID,
                                        TipoMovimiento.COMPRA_SISTEMA,
                                        -3,
                                        PRECIO_VALIDO,
                                        MONTO_VALIDO,
                                        null,
                                        FECHA_FIJA));
    }

    @Test
    void precioUnitarioCero_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        JUGADOR_ID,
                                        TipoMovimiento.COMPRA_SISTEMA,
                                        CANTIDAD_VALIDA,
                                        BigDecimal.ZERO,
                                        MONTO_VALIDO,
                                        null,
                                        FECHA_FIJA));
    }

    @Test
    void precioUnitarioNegativo_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        JUGADOR_ID,
                                        TipoMovimiento.COMPRA_SISTEMA,
                                        CANTIDAD_VALIDA,
                                        new BigDecimal("-5.00"),
                                        MONTO_VALIDO,
                                        null,
                                        FECHA_FIJA));
    }

    @Test
    void montoTotalCeroEnTipoNoRecarga_lanzaExcepcion() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        JUGADOR_ID,
                                        TipoMovimiento.VENTA_SISTEMA,
                                        CANTIDAD_VALIDA,
                                        PRECIO_VALIDO,
                                        BigDecimal.ZERO,
                                        null,
                                        FECHA_FIJA));
    }

    // ---- contraparteUsuarioId: obligatorio sólo en COMPRA_P2P/VENTA_P2P ----

    @ParameterizedTest
    @EnumSource(
            value = TipoMovimiento.class,
            names = {"COMPRA_P2P", "VENTA_P2P"})
    void p2pSinContraparte_lanzaExcepcion(TipoMovimiento tipo) {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        JUGADOR_ID,
                                        tipo,
                                        CANTIDAD_VALIDA,
                                        PRECIO_VALIDO,
                                        MONTO_VALIDO,
                                        null,
                                        FECHA_FIJA));
    }

    @ParameterizedTest
    @EnumSource(
            value = TipoMovimiento.class,
            names = {"COMPRA_P2P", "VENTA_P2P"})
    void p2pConContraparte_construyeCorrectamente(TipoMovimiento tipo) {
        Movimiento mov =
                Movimiento.registrar(
                        USUARIO_ID,
                        JUGADOR_ID,
                        tipo,
                        CANTIDAD_VALIDA,
                        PRECIO_VALIDO,
                        MONTO_VALIDO,
                        CONTRAPARTE_ID,
                        FECHA_FIJA);

        assertThat(mov.getContraparteUsuarioId()).isEqualTo(CONTRAPARTE_ID);
    }

    @ParameterizedTest
    @EnumSource(
            value = TipoMovimiento.class,
            names = {"COMPRA_SISTEMA", "VENTA_SISTEMA", "COMPENSACION_BAJA"})
    void tipoNoP2pConContraparteNoNula_lanzaExcepcion(TipoMovimiento tipo) {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        JUGADOR_ID,
                                        tipo,
                                        CANTIDAD_VALIDA,
                                        PRECIO_VALIDO,
                                        MONTO_VALIDO,
                                        CONTRAPARTE_ID,
                                        FECHA_FIJA));
    }

    @Test
    void compensacionBajaValida_construyeCorrectamente() {
        Movimiento mov =
                Movimiento.registrar(
                        USUARIO_ID,
                        JUGADOR_ID,
                        TipoMovimiento.COMPENSACION_BAJA,
                        CANTIDAD_VALIDA,
                        PRECIO_VALIDO,
                        MONTO_VALIDO,
                        null,
                        FECHA_FIJA);

        assertThat(mov.getTipo()).isEqualTo(TipoMovimiento.COMPENSACION_BAJA);
    }

    // ---- Campos siempre obligatorios, cualquiera sea el tipo ----

    @Test
    void usuarioIdNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        null,
                                        null,
                                        TipoMovimiento.RECARGA_SALDO,
                                        null,
                                        null,
                                        MONTO_VALIDO,
                                        null,
                                        FECHA_FIJA));
    }

    @Test
    void tipoNulo_lanzaExcepcion() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                Movimiento.registrar(
                                        USUARIO_ID,
                                        null,
                                        null,
                                        null,
                                        null,
                                        MONTO_VALIDO,
                                        null,
                                        FECHA_FIJA));
    }

    @Test
    void registrarSinFechaExplicita_usaInstanteActual() {
        Instant antes = Instant.now();
        Movimiento mov =
                Movimiento.registrar(
                        USUARIO_ID,
                        null,
                        TipoMovimiento.RECARGA_SALDO,
                        null,
                        null,
                        MONTO_VALIDO,
                        null);
        Instant despues = Instant.now();

        assertThat(mov.getFecha()).isBetween(antes, despues);
    }

    @Test
    void dosMovimientosRegistradosPorSeparadoTienenIdsDistintos() {
        Movimiento uno =
                Movimiento.registrar(
                        USUARIO_ID,
                        null,
                        TipoMovimiento.RECARGA_SALDO,
                        null,
                        null,
                        MONTO_VALIDO,
                        null,
                        FECHA_FIJA);
        Movimiento otro =
                Movimiento.registrar(
                        USUARIO_ID,
                        null,
                        TipoMovimiento.RECARGA_SALDO,
                        null,
                        null,
                        MONTO_VALIDO,
                        null,
                        FECHA_FIJA);

        assertThat(uno.getId()).isNotEqualTo(otro.getId());
    }

    private static UUID contraparteSiP2p(TipoMovimiento tipo) {
        return (tipo == TipoMovimiento.COMPRA_P2P || tipo == TipoMovimiento.VENTA_P2P)
                ? CONTRAPARTE_ID
                : null;
    }
}
