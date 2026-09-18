package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.common.exceptions.EmailYaRegistradoException;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.TipoMovimiento;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.MovimientoRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre {@link UsuarioServiceImpl}: {@code registrar} (UC-01, unicidad de email) y {@code
 * recargarSaldo} (UC-14) con los casos límite de plan.md §12 ("débito con saldo exacto, débito con
 * saldo insuficiente, recarga con monto ≤0" — acá aplicados a la recarga, la única mutación de
 * saldo que hace este Service).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    private static final String EMAIL = "persona@example.com";
    private static final String PASSWORD_HASH = "hash-bcrypt";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MovimientoRepository movimientoRepository;
    @Mock private AuditoriaService auditoriaService;

    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UsuarioServiceImpl(usuarioRepository, movimientoRepository, auditoriaService);
    }

    // ---- registrar (UC-01) ----

    @Test
    void registrar_conEmailNoRegistrado_guardaYRetornaElUsuario() {
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario creado = service.registrar(EMAIL, PASSWORD_HASH);

        assertThat(creado.getEmail()).isEqualTo(EMAIL);
        assertThat(creado.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(creado.getSaldoVirtual()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void registrar_conEmailYaRegistrado_lanzaExcepcionYNoGuardaNada() {
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(EMAIL, PASSWORD_HASH))
                .isInstanceOf(EmailYaRegistradoException.class)
                .hasMessageContaining(EMAIL);

        verify(usuarioRepository, never()).save(any());
    }

    // ---- recargarSaldo (UC-14) ----

    @Test
    void recargarSaldo_conUsuarioExistenteYMontoValido_acreditaRegistraMovimientoYAudita() {
        UUID usuarioId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        Usuario resultado = service.recargarSaldo(usuarioId, new BigDecimal("100.00"), actorId);

        assertThat(resultado.getSaldoVirtual()).isEqualByComparingTo("100.00");

        ArgumentCaptor<Movimiento> movimientoCaptor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository).save(movimientoCaptor.capture());
        Movimiento movimiento = movimientoCaptor.getValue();
        assertThat(movimiento.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimiento.RECARGA_SALDO);
        assertThat(movimiento.getMontoTotal()).isEqualByComparingTo("100.00");
        assertThat(movimiento.getJugadorId()).isNull();
        assertThat(movimiento.getContraparteUsuarioId()).isNull();

        verify(auditoriaService)
                .registrar(
                        eq(actorId),
                        eq("RECARGA_SALDO"),
                        eq("Usuario"),
                        eq(usuarioId),
                        any(),
                        any());
    }

    @Test
    void recargarSaldo_conUsuarioInexistente_lanzaExcepcionYNoTocaNiMovimientoNiAuditoria() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.recargarSaldo(
                                        usuarioId, new BigDecimal("50.00"), UUID.randomUUID()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(usuarioId.toString());

        verifyNoInteractions(movimientoRepository, auditoriaService);
    }

    @Test
    void recargarSaldo_montoCero_lanzaExcepcionYNoTocaNiMovimientoNiAuditoria() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> service.recargarSaldo(usuarioId, BigDecimal.ZERO, UUID.randomUUID()));

        assertThat(usuario.getSaldoVirtual()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(movimientoRepository, auditoriaService);
    }

    @Test
    void recargarSaldo_montoNegativo_lanzaExcepcionYNoTocaNiMovimientoNiAuditoria() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = Usuario.registrar(EMAIL, PASSWORD_HASH);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                service.recargarSaldo(
                                        usuarioId, new BigDecimal("-10.00"), UUID.randomUUID()));

        assertThat(usuario.getSaldoVirtual()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(movimientoRepository, auditoriaService);
    }
}
