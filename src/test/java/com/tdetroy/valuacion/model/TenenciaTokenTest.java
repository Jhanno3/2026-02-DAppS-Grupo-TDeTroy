package com.tdetroy.valuacion.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdetroy.valuacion.common.exceptions.TenenciaInsuficienteException;
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
        tenencia.acreditar(10);
        tenencia.reservar(4);

        assertThat(tenencia.disponible()).isEqualTo(6);
    }

    // ---- acreditar ----

    @Test
    void acreditar_sumaALaCantidad() {
        TenenciaToken tenencia = tenenciaVacia();

        tenencia.acreditar(5);
        tenencia.acreditar(3);

        assertThat(tenencia.getCantidad()).isEqualTo(8);
    }

    @Test
    void acreditar_cantidadCero_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.acreditar(0));
    }

    @Test
    void acreditar_cantidadNegativa_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.acreditar(-1));
    }

    // ---- debitar ----

    @Test
    void debitar_conDisponibleExacto_dejaCantidadEnCero() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);

        tenencia.debitar(10);

        assertThat(tenencia.getCantidad()).isZero();
    }

    @Test
    void debitar_queSuperaDisponible_lanzaTenenciaInsuficienteExceptionYNoMutaEstado() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);
        tenencia.reservar(4);

        assertThatThrownBy(() -> tenencia.debitar(7))
                .isInstanceOf(TenenciaInsuficienteException.class);
        assertThat(tenencia.getCantidad()).isEqualTo(10);
        assertThat(tenencia.getCantidadReservada()).isEqualTo(4);
    }

    @Test
    void debitar_cantidadCero_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.debitar(0));
    }

    // ---- reservar ----

    @Test
    void reservar_sumaACantidadReservada() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);

        tenencia.reservar(6);

        assertThat(tenencia.getCantidadReservada()).isEqualTo(6);
        assertThat(tenencia.getCantidad()).isEqualTo(10);
    }

    @Test
    void reservar_hastaExactamenteLoDisponible_permitido() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);

        tenencia.reservar(10);

        assertThat(tenencia.getCantidadReservada()).isEqualTo(10);
        assertThat(tenencia.disponible()).isZero();
    }

    @Test
    void reservar_queSuperaDisponible_lanzaTenenciaInsuficienteExceptionYNoMutaEstado() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);

        assertThatThrownBy(() -> tenencia.reservar(11))
                .isInstanceOf(TenenciaInsuficienteException.class);
        assertThat(tenencia.getCantidadReservada()).isZero();
    }

    @Test
    void reservar_cantidadCero_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.reservar(0));
    }

    // ---- liberarReserva ----

    @Test
    void liberarReserva_restaDeCantidadReservadaSinTocarCantidad() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);
        tenencia.reservar(7);

        tenencia.liberarReserva(3);

        assertThat(tenencia.getCantidadReservada()).isEqualTo(4);
        assertThat(tenencia.getCantidad()).isEqualTo(10);
    }

    @Test
    void liberarReserva_queSuperaCantidadReservada_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);
        tenencia.reservar(5);

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.liberarReserva(6));
        assertThat(tenencia.getCantidadReservada()).isEqualTo(5);
    }

    @Test
    void liberarReserva_cantidadCero_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);
        tenencia.reservar(5);

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.liberarReserva(0));
    }

    // ---- ejecutarReserva ----

    @Test
    void ejecutarReserva_restaDeCantidadYDeCantidadReservadaALaVez() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);
        tenencia.reservar(7);

        tenencia.ejecutarReserva(4);

        assertThat(tenencia.getCantidad()).isEqualTo(6);
        assertThat(tenencia.getCantidadReservada()).isEqualTo(3);
    }

    @Test
    void ejecutarReserva_todaLaReserva_dejaAmbosCamposConsistentes() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);
        tenencia.reservar(10);

        tenencia.ejecutarReserva(10);

        assertThat(tenencia.getCantidad()).isZero();
        assertThat(tenencia.getCantidadReservada()).isZero();
    }

    @Test
    void ejecutarReserva_queSuperaCantidadReservada_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);
        tenencia.reservar(5);

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.ejecutarReserva(6));
        assertThat(tenencia.getCantidad()).isEqualTo(10);
        assertThat(tenencia.getCantidadReservada()).isEqualTo(5);
    }

    @Test
    void ejecutarReserva_cantidadCero_lanzaExcepcion() {
        TenenciaToken tenencia = tenenciaVacia();
        tenencia.acreditar(10);
        tenencia.reservar(5);

        assertThatIllegalArgumentException().isThrownBy(() -> tenencia.ejecutarReserva(0));
    }

    // ---- invariante general: cantidadReservada nunca supera cantidad ----

    @Test
    void secuenciaDeOperaciones_nuncaDejaCantidadReservadaPorEncimaDeCantidad() {
        TenenciaToken tenencia = tenenciaVacia();

        tenencia.acreditar(20);
        tenencia.reservar(15);
        tenencia.ejecutarReserva(5);
        tenencia.liberarReserva(2);
        tenencia.debitar(3);

        assertThat(tenencia.getCantidadReservada()).isLessThanOrEqualTo(tenencia.getCantidad());
    }
}
