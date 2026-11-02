package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdetroy.valuacion.common.exceptions.TenenciaInsuficienteException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Cubre la invariante propia de {@link TenenciaToken} (plan.md §2.5, constitution.md §2): {@code
 * cantidadReservada ≤ cantidad} bajo ninguna operación, protegida en {@link #acreditar}, {@link
 * #debitar}, {@link #reservar}, {@link #liberarReserva} y {@link #ejecutarReserva}, no sólo en
 * {@code OfertaService}/{@code TokenService}.
 */
class TenenciaTokenTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID JUGADOR_ID = UUID.randomUUID();

    private static TenenciaToken tenenciaVacia() {
        return TenenciaToken.abrir(USUARIO_ID, JUGADOR_ID);
    }

    // ---- abrir ----

    @Test
    void abrir_creaConCantidadYReservaEnCero() {
        TenenciaToken tenencia = tenenciaVacia();

        assertThat(tenencia.getId()).isNotNull();
        assertThat(tenencia.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(tenencia.getJugadorId()).isEqualTo(JUGADOR_ID);
        assertThat(tenencia.getCantidad()).isZero();
        assertThat(tenencia.getCantidadReservada()).isZero();
        assertThat(tenencia.disponible()).isZero();
    }

    @Test
    void abrir_conUsuarioIdNulo_lanzaExcepcion() {
        assertThatNullPointerException().isThrownBy(() -> TenenciaToken.abrir(null, JUGADOR_ID));
    }

    @Test
    void abrir_conJugadorIdNulo_lanzaExcepcion() {
        assertThatNullPointerException().isThrownBy(() -> TenenciaToken.abrir(USUARIO_ID, null));
    }

    // ---- disponible ----

    @Test
    void disponible_esCantidadMenosReservada() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);
        tenencia.reservar(4);

        assertThat(tenencia.disponible()).isEqualTo(6);
    }

    // ---- acreditar ----

    @Test
    void acreditar_sumaALaCantidad() {
        TenenciaToken tenencia = tenenciaVacia();

        tenencia.acreditar(5, BigDecimal.TEN);
        tenencia.acreditar(3, BigDecimal.TEN);

        assertThat(tenencia.getCantidad()).isEqualTo(8);
    }

    @Test
    void acreditar_cantidadCero_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> tenencia.acreditar(0, BigDecimal.TEN));
    }

    @Test
    void acreditar_cantidadNegativa_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> tenencia.acreditar(-1, BigDecimal.TEN));
    }

    // ---- debitar ----

    @Test
    void debitar_conDisponibleExacto_dejaCantidadEnCero() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);

        tenencia.debitar(10);

        assertThat(tenencia.getCantidad()).isZero();
    }

    @Test
    void debitar_queSuperaDisponible_lanzaTenenciaInsuficienteExceptionYNoMutaEstado() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);
        tenencia.reservar(4);

        assertThatThrownBy(() -> tenencia.debitar(7))
                .isInstanceOf(TenenciaInsuficienteException.class);
        assertThat(tenencia.getCantidad()).isEqualTo(10);
        assertThat(tenencia.getCantidadReservada()).isEqualTo(4);
    }

    @Test
    void debitar_cantidadCero_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.debitar(0));
    }

    // ---- reservar ----

    @Test
    void reservar_sumaACantidadReservada() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);

        tenencia.reservar(6);

        assertThat(tenencia.getCantidadReservada()).isEqualTo(6);
        assertThat(tenencia.getCantidad()).isEqualTo(10);
    }

    @Test
    void reservar_hastaExactamenteLoDisponible_permitido() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);

        tenencia.reservar(10);

        assertThat(tenencia.getCantidadReservada()).isEqualTo(10);
        assertThat(tenencia.disponible()).isZero();
    }

    @Test
    void reservar_queSuperaDisponible_lanzaTenenciaInsuficienteExceptionYNoMutaEstado() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);

        assertThatThrownBy(() -> tenencia.reservar(11))
                .isInstanceOf(TenenciaInsuficienteException.class);
        assertThat(tenencia.getCantidadReservada()).isZero();
    }

    @Test
    void reservar_cantidadCero_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.reservar(0));
    }

    // ---- liberarReserva ----

    @Test
    void liberarReserva_restaDeCantidadReservadaSinTocarCantidad() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);
        tenencia.reservar(7);

        tenencia.liberarReserva(3);

        assertThat(tenencia.getCantidadReservada()).isEqualTo(4);
        assertThat(tenencia.getCantidad()).isEqualTo(10);
    }

    @Test
    void liberarReserva_queSuperaCantidadReservada_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);
        tenencia.reservar(5);

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.liberarReserva(6));
        assertThat(tenencia.getCantidadReservada()).isEqualTo(5);
    }

    @Test
    void liberarReserva_cantidadCero_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);
        tenencia.reservar(5);

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.liberarReserva(0));
    }

    // ---- ejecutarReserva ----

    @Test
    void ejecutarReserva_restaDeCantidadYDeCantidadReservadaALaVez() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);
        tenencia.reservar(7);

        tenencia.ejecutarReserva(4);

        assertThat(tenencia.getCantidad()).isEqualTo(6);
        assertThat(tenencia.getCantidadReservada()).isEqualTo(3);
    }

    @Test
    void ejecutarReserva_todaLaReserva_dejaAmbosCamposConsistentes() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);
        tenencia.reservar(10);

        tenencia.ejecutarReserva(10);

        assertThat(tenencia.getCantidad()).isZero();
        assertThat(tenencia.getCantidadReservada()).isZero();
    }

    @Test
    void ejecutarReserva_queSuperaCantidadReservada_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);
        tenencia.reservar(5);

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.ejecutarReserva(6));
        assertThat(tenencia.getCantidad()).isEqualTo(10);
        assertThat(tenencia.getCantidadReservada()).isEqualTo(5);
    }

    @Test
    void ejecutarReserva_cantidadCero_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, BigDecimal.TEN);
        tenencia.reservar(5);

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.ejecutarReserva(0));
    }

    // ---- invariante general: cantidadReservada nunca supera cantidad ----

    @Test
    void secuenciaDeOperaciones_nuncaDejaCantidadReservadaPorEncimaDeCantidad() {
        TenenciaToken tenencia = tenenciaVacia();

        tenencia.acreditar(20, BigDecimal.TEN);
        tenencia.reservar(15);
        tenencia.ejecutarReserva(5);
        tenencia.liberarReserva(2);
        tenencia.debitar(3);

        assertThat(tenencia.getCantidadReservada()).isLessThanOrEqualTo(tenencia.getCantidad());
    }

    // ---- costo de adquisición: precio promedio y ganancia/pérdida ----

    @Test
    void acreditar_sumaElCostoDeCompra() {
        TenenciaToken tenencia = tenenciaVacia();

        tenencia.acreditar(10, new BigDecimal("100"));

        assertThat(tenencia.getCostoTotal()).isEqualByComparingTo("1000.00");
        assertThat(tenencia.precioPromedioCompra()).isEqualByComparingTo("100.00");
    }

    @Test
    void acreditar_variasCompras_promediaPonderandoPorCantidad() {
        TenenciaToken tenencia = tenenciaVacia();

        tenencia.acreditar(10, new BigDecimal("100"));
        tenencia.acreditar(10, new BigDecimal("120"));

        // (1000 + 1200) / 20 = 110
        assertThat(tenencia.precioPromedioCompra()).isEqualByComparingTo("110.00");
    }

    @Test
    void acreditar_conPrecioCeroONegativo_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> tenencia.acreditar(1, BigDecimal.ZERO));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tenencia.acreditar(1, new BigDecimal("-1")));
    }

    @Test
    void gananciaPerdida_ejemploDelEnunciado() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, new BigDecimal("100"));

        // Sube a 120: la posición vale 1200, ganancia 200 (+20%).
        assertThat(tenencia.valorDisponible(new BigDecimal("120"))).isEqualByComparingTo("1200.00");
        assertThat(tenencia.gananciaPerdida(new BigDecimal("120"))).isEqualByComparingTo("200.00");
        assertThat(tenencia.rentabilidad(new BigDecimal("120"))).isEqualByComparingTo("20.00");
        // Baja a 90: la posición vale 900, pérdida 100 (-10%).
        assertThat(tenencia.gananciaPerdida(new BigDecimal("90"))).isEqualByComparingTo("-100.00");
        assertThat(tenencia.rentabilidad(new BigDecimal("90"))).isEqualByComparingTo("-10.00");
    }

    @Test
    void debitar_noCambiaElPrecioPromedioDeLoQueQueda() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, new BigDecimal("100"));
        tenencia.acreditar(10, new BigDecimal("120"));

        tenencia.debitar(5);

        assertThat(tenencia.getCostoTotal()).isEqualByComparingTo("1650.00");
        assertThat(tenencia.precioPromedioCompra()).isEqualByComparingTo("110.00");
    }

    @Test
    void ejecutarReserva_descuentaElCostoProporcional() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, new BigDecimal("100"));
        tenencia.reservar(4);

        tenencia.ejecutarReserva(4);

        assertThat(tenencia.getCostoTotal()).isEqualByComparingTo("600.00");
        assertThat(tenencia.precioPromedioCompra()).isEqualByComparingTo("100.00");
    }

    @Test
    void venderTodo_dejaElCostoEnCeroExacto() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(3, new BigDecimal("3.33"));
        tenencia.acreditar(1, new BigDecimal("0.01"));

        tenencia.debitar(1);
        tenencia.debitar(3);

        assertThat(tenencia.getCostoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(tenencia.precioPromedioCompra()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void gananciaPerdida_sinRedondeoDelPromedio() {
        // 3 tokens por 10.00 en total: el promedio redondeado es 3.33, pero el costo es 10.00.
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(1, new BigDecimal("3.34"));
        tenencia.acreditar(2, new BigDecimal("3.33"));

        assertThat(tenencia.gananciaPerdida(new BigDecimal("4"))).isEqualByComparingTo("2.00");
    }

    @Test
    void reservar_noCuentaEnLaGananciaDeLoDisponible() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10, new BigDecimal("100"));
        tenencia.reservar(4);

        // Disponibles: 6 tokens que costaron 600 y valen 720.
        assertThat(tenencia.gananciaPerdida(new BigDecimal("120"))).isEqualByComparingTo("120.00");
        assertThat(tenencia.valorReservado(new BigDecimal("120"))).isEqualByComparingTo("480.00");
    }
}
