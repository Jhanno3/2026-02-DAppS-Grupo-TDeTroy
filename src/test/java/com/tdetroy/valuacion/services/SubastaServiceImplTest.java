package com.tdetroy.valuacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.common.exceptions.OperacionSubastaInvalidaException;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.common.exceptions.SaldoInsuficienteException;
import com.tdetroy.valuacion.common.exceptions.TenenciaInsuficienteException;
import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.entity.MovimientoEntity;
import com.tdetroy.valuacion.entity.PujaSubastaEntity;
import com.tdetroy.valuacion.entity.SubastaEntity;
import com.tdetroy.valuacion.entity.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.UsuarioEntity;
import com.tdetroy.valuacion.model.EstadoSubasta;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.Subasta;
import com.tdetroy.valuacion.model.TenenciaToken;
import com.tdetroy.valuacion.model.TipoMovimiento;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.MovimientoRepository;
import com.tdetroy.valuacion.repositories.PujaSubastaRepository;
import com.tdetroy.valuacion.repositories.SubastaRepository;
import com.tdetroy.valuacion.repositories.TenenciaTokenRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre {@link SubastaServiceImpl} (UC-18) contra repositorios en memoria (cada {@code save} pisa
 * la fila y cada {@code findById} devuelve la última guardada): reserva de tokens al crear,
 * descuento/devolución de saldo en cada puja, cancelación, adjudicación y subasta desierta.
 */
@ExtendWith(MockitoExtension.class)
class SubastaServiceImplTest {

    @Mock private SubastaRepository subastaRepository;
    @Mock private PujaSubastaRepository pujaSubastaRepository;
    @Mock private TenenciaTokenRepository tenenciaTokenRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JugadorRepository jugadorRepository;
    @Mock private MovimientoRepository movimientoRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private SubastaService self;

    private final Map<UUID, UsuarioEntity> usuarios = new HashMap<>();
    private final Map<String, TenenciaTokenEntity> tenencias = new HashMap<>();
    private final Map<UUID, SubastaEntity> subastas = new HashMap<>();
    private final List<MovimientoEntity> movimientos = new ArrayList<>();

    private SubastaServiceImpl service;
    private UUID jugadorId;
    private UUID vendedorId;
    private UUID postorA;
    private UUID postorB;

    @BeforeEach
    void setUp() {
        service =
                new SubastaServiceImpl(
                        subastaRepository,
                        pujaSubastaRepository,
                        tenenciaTokenRepository,
                        usuarioRepository,
                        jugadorRepository,
                        movimientoRepository,
                        auditoriaService,
                        self);

        Jugador jugador =
                Jugador.darAlta(
                        "Matteo Politano",
                        "SSC Napoli",
                        "Delantero",
                        LocalDate.of(1993, 8, 3),
                        "Italy",
                        "Serie A");
        jugadorId = jugador.getId();
        lenient()
                .when(jugadorRepository.findById(jugadorId))
                .thenReturn(Optional.of(JugadorEntity.desde(jugador)));

        vendedorId = altaUsuario("vendedor@example.com", "0.00");
        postorA = altaUsuario("a@example.com", "1000.00");
        postorB = altaUsuario("b@example.com", "1000.00");
        darTokens(vendedorId, 3);

        lenient()
                .when(usuarioRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(usuarios.get(inv.<UUID>getArgument(0))));
        lenient()
                .when(usuarioRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            UsuarioEntity e = inv.getArgument(0);
                            usuarios.put(e.getId(), e);
                            return e;
                        });
        lenient()
                .when(tenenciaTokenRepository.findByUsuarioIdAndJugadorId(any(), any()))
                .thenAnswer(
                        inv ->
                                Optional.ofNullable(
                                        tenencias.get(
                                                clave(inv.getArgument(0), inv.getArgument(1)))));
        lenient()
                .when(tenenciaTokenRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            TenenciaTokenEntity e = inv.getArgument(0);
                            tenencias.put(clave(e.getUsuarioId(), e.getJugadorId()), e);
                            return e;
                        });
        lenient()
                .when(subastaRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(subastas.get(inv.<UUID>getArgument(0))));
        lenient()
                .when(subastaRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            SubastaEntity e = inv.getArgument(0);
                            subastas.put(e.getId(), e);
                            return e;
                        });
        lenient()
                .when(movimientoRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            MovimientoEntity e = inv.getArgument(0);
                            movimientos.add(e);
                            return e;
                        });
        lenient()
                .when(pujaSubastaRepository.save(any()))
                .thenAnswer(inv -> inv.<PujaSubastaEntity>getArgument(0));
    }

    @Test
    void crear_reservaLosTokensDelVendedor() {
        Subasta subasta = service.crear(vendedorId, jugadorId, 2, new BigDecimal("100.00"), 30);

        assertThat(subasta.getEstado()).isEqualTo(EstadoSubasta.ACTIVA);
        assertThat(tenencia(vendedorId).disponible()).isEqualTo(1);
        assertThat(tenencia(vendedorId).getCantidadReservada()).isEqualTo(2);
    }

    @Test
    void crear_conMasTokensQueLosDisponibles_lanzaExcepcionYNoGuardaLaSubasta() {
        assertThatThrownBy(
                        () -> service.crear(vendedorId, jugadorId, 4, new BigDecimal("10.00"), 30))
                .isInstanceOf(TenenciaInsuficienteException.class);
        verify(subastaRepository, never()).save(any());
    }

    @Test
    void crear_deUnJugadorDadoDeBaja_lanzaExcepcion() {
        Jugador inactivo =
                Jugador.darAlta(
                        "Retirado", "Club", "Delantero", LocalDate.of(1980, 1, 1), "AR", "Liga");
        inactivo.darDeBaja();
        when(jugadorRepository.findById(inactivo.getId()))
                .thenReturn(Optional.of(JugadorEntity.desde(inactivo)));

        assertThatThrownBy(
                        () ->
                                service.crear(
                                        vendedorId,
                                        inactivo.getId(),
                                        1,
                                        new BigDecimal("10.00"),
                                        30))
                .isInstanceOf(OperacionSubastaInvalidaException.class);
    }

    @Test
    void pujar_descuentaAlNuevoPostorYLeDevuelveAlSuperado() {
        UUID subastaId = crearSubasta();

        service.pujar(postorA, subastaId, 0);
        // 2 tokens × 100.00
        assertThat(saldo(postorA)).isEqualByComparingTo("800.00");

        service.pujar(postorB, subastaId, 10);
        // A recupera todo; B reserva 2 × 110.00
        assertThat(saldo(postorA)).isEqualByComparingTo("1000.00");
        assertThat(saldo(postorB)).isEqualByComparingTo("780.00");
        assertThat(subastas.get(subastaId).getMejorPostorId()).isEqualTo(postorB);
    }

    @Test
    void pujar_sinSaldoSuficiente_lanzaExcepcionSinTocarNiLaSubastaNiAlMejorPostor() {
        UUID subastaId = crearSubasta();
        UUID pobre = altaUsuario("pobre@example.com", "50.00");
        service.pujar(postorA, subastaId, 0);

        assertThatThrownBy(() -> service.pujar(pobre, subastaId, 5))
                .isInstanceOf(SaldoInsuficienteException.class);
        // La transacción revierte en producción; acá basta con que no se haya devuelto nada a A
        // ni registrado a "pobre" como mejor postor antes de fallar.
        assertThat(saldo(postorA)).isEqualByComparingTo("800.00");
        assertThat(subastas.get(subastaId).getMejorPostorId()).isEqualTo(postorA);
    }

    @Test
    void pujar_conPorcentajeInvalido_lanzaExcepcion() {
        UUID subastaId = crearSubasta();

        assertThatThrownBy(() -> service.pujar(postorA, subastaId, 15))
                .isInstanceOf(OperacionSubastaInvalidaException.class);
        assertThat(saldo(postorA)).isEqualByComparingTo("1000.00");
    }

    @Test
    void cancelar_devuelveTokensAlVendedorYSaldoAlMejorPostor() {
        UUID subastaId = crearSubasta();
        service.pujar(postorA, subastaId, 5);

        service.cancelar(vendedorId, subastaId);

        assertThat(subastas.get(subastaId).getEstado()).isEqualTo(EstadoSubasta.CANCELADA);
        assertThat(tenencia(vendedorId).disponible()).isEqualTo(3);
        assertThat(saldo(postorA)).isEqualByComparingTo("1000.00");
    }

    @Test
    void cancelar_porAlguienQueNoEsElVendedor_lanzaExcepcion() {
        UUID subastaId = crearSubasta();

        assertThatThrownBy(() -> service.cancelar(postorA, subastaId))
                .isInstanceOf(OperacionSubastaInvalidaException.class);
        assertThat(subastas.get(subastaId).getEstado()).isEqualTo(EstadoSubasta.ACTIVA);
    }

    @Test
    void cerrarUna_trasDosMinutosSinPujas_adjudicaTokensYSaldoYRegistraMovimientos() {
        UUID subastaId = crearSubasta();
        service.pujar(postorA, subastaId, 20);
        retrocederCierre(subastaId);

        service.cerrarUna(subastaId);

        assertThat(subastas.get(subastaId).getEstado()).isEqualTo(EstadoSubasta.ADJUDICADA);
        assertThat(tenencia(vendedorId).getCantidad()).isEqualTo(1);
        assertThat(tenencia(vendedorId).getCantidadReservada()).isZero();
        assertThat(tenencia(postorA).getCantidad()).isEqualTo(2);
        // 2 × 120.00: ya se le había descontado a A al pujar, ahora lo cobra el vendedor.
        assertThat(saldo(postorA)).isEqualByComparingTo("760.00");
        assertThat(saldo(vendedorId)).isEqualByComparingTo("240.00");
        assertThat(movimientos)
                .extracting(MovimientoEntity::getTipo)
                .containsExactlyInAnyOrder(
                        TipoMovimiento.VENTA_SUBASTA, TipoMovimiento.COMPRA_SUBASTA);
        verify(auditoriaService)
                .registrar(
                        isNull(),
                        eq("ADJUDICACION_SUBASTA"),
                        eq("Subasta"),
                        eq(subastaId),
                        isNull(),
                        any());
    }

    @Test
    void cerrarUna_sinPujasYConLaDuracionVencida_laDeclaraDesiertaYDevuelveLosTokens() {
        UUID subastaId = crearSubasta();
        SubastaEntity vencida = subastas.get(subastaId);
        Subasta modelo = vencida.aModelo();
        subastas.put(
                subastaId,
                SubastaEntity.desde(
                        Subasta.reconstruir(
                                modelo.getId(),
                                modelo.getVendedorId(),
                                modelo.getJugadorId(),
                                modelo.getCantidad(),
                                modelo.getPrecioInicial(),
                                null,
                                null,
                                0,
                                EstadoSubasta.ACTIVA,
                                modelo.getFechaCreacion().minus(Duration.ofHours(1)),
                                Instant.now().minusSeconds(1),
                                null)));

        service.cerrarUna(subastaId);

        assertThat(subastas.get(subastaId).getEstado()).isEqualTo(EstadoSubasta.DESIERTA);
        assertThat(tenencia(vendedorId).disponible()).isEqualTo(3);
        assertThat(movimientos).isEmpty();
    }

    @Test
    void cerrarUna_conElPlazoTodaviaCorriendo_noHaceNada() {
        UUID subastaId = crearSubasta();
        service.pujar(postorA, subastaId, 5);

        service.cerrarUna(subastaId);

        assertThat(subastas.get(subastaId).getEstado()).isEqualTo(EstadoSubasta.ACTIVA);
        assertThat(movimientos).isEmpty();
    }

    @Test
    void cancelarActivasDe_cancelaTodasLasActivasDelJugador() {
        UUID subastaId = crearSubasta();
        service.pujar(postorA, subastaId, 0);
        when(subastaRepository.findByJugadorIdAndEstadoOrderByFechaCreacionDesc(
                        jugadorId, EstadoSubasta.ACTIVA))
                .thenReturn(List.of(subastas.get(subastaId)));

        service.cancelarActivasDe(jugadorId);

        assertThat(subastas.get(subastaId).getEstado()).isEqualTo(EstadoSubasta.CANCELADA);
        assertThat(tenencia(vendedorId).disponible()).isEqualTo(3);
        assertThat(saldo(postorA)).isEqualByComparingTo("1000.00");
    }

    @Test
    void crear_deUnJugadorInexistente_lanzaExcepcion() {
        UUID inexistente = UUID.randomUUID();
        when(jugadorRepository.findById(inexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.crear(
                                        vendedorId, inexistente, 1, new BigDecimal("10.00"), 30))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void listarActivas_devuelveLasActivasDelRepositorio() {
        UUID subastaId = crearSubasta();
        when(subastaRepository.findByEstadoOrderByFechaCreacionDesc(EstadoSubasta.ACTIVA))
                .thenReturn(List.of(subastas.get(subastaId)));

        assertThat(service.listarActivas()).extracting(Subasta::getId).containsExactly(subastaId);
    }

    @Test
    void cerrarVencidas_soloCierraLasQueCorresponden_yUnaFallaNoFrenaALasDemas() {
        darTokens(vendedorId, 6);
        UUID enCurso = crearSubasta();
        UUID paraAdjudicar = crearSubasta();
        UUID queFalla = crearSubasta();
        service.pujar(postorA, enCurso, 5);
        service.pujar(postorA, paraAdjudicar, 5);
        service.pujar(postorB, queFalla, 5);
        retrocederCierre(paraAdjudicar);
        retrocederCierre(queFalla);
        when(subastaRepository.findByEstadoOrderByFechaCreacionDesc(EstadoSubasta.ACTIVA))
                .thenReturn(
                        List.of(
                                subastas.get(queFalla),
                                subastas.get(enCurso),
                                subastas.get(paraAdjudicar)));
        doThrow(new IllegalStateException("falla puntual")).when(self).cerrarUna(queFalla);

        service.cerrarVencidas();

        // La que falla se intenta igual, la que sigue en plazo ni se toca, y la vencida se cierra.
        verify(self).cerrarUna(queFalla);
        verify(self, never()).cerrarUna(enCurso);
        verify(self).cerrarUna(paraAdjudicar);
    }

    private UUID crearSubasta() {
        return service.crear(vendedorId, jugadorId, 2, new BigDecimal("100.00"), 30).getId();
    }

    /** Simula que ya pasaron los 2 minutos desde la última puja. */
    private void retrocederCierre(UUID subastaId) {
        SubastaEntity entity = subastas.get(subastaId);
        entity.setFechaCierre(Instant.now().minusSeconds(1));
    }

    private UUID altaUsuario(String email, String saldo) {
        Usuario usuario = Usuario.registrar(email, "hash");
        BigDecimal monto = new BigDecimal(saldo);
        if (monto.signum() > 0) {
            usuario.acreditarSaldo(monto);
        }
        usuarios.put(usuario.getId(), UsuarioEntity.desde(usuario));
        return usuario.getId();
    }

    private void darTokens(UUID usuarioId, int cantidad) {
        TenenciaToken tenencia = TenenciaToken.abrir(usuarioId, jugadorId);
        tenencia.acreditar(cantidad);
        tenencias.put(clave(usuarioId, jugadorId), TenenciaTokenEntity.desde(tenencia));
    }

    private BigDecimal saldo(UUID usuarioId) {
        return usuarios.get(usuarioId).getSaldoVirtual();
    }

    private TenenciaToken tenencia(UUID usuarioId) {
        return tenencias.get(clave(usuarioId, jugadorId)).aModelo();
    }

    private static String clave(UUID usuarioId, UUID jugadorId) {
        return usuarioId + "/" + jugadorId;
    }
}
