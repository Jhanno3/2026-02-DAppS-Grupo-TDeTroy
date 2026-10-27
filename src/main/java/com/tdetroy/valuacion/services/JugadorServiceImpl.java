package com.tdetroy.valuacion.services;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.common.exceptions.RecursoNoEncontradoException;
import com.tdetroy.valuacion.entity.CotizacionHistoricaEntity;
import com.tdetroy.valuacion.entity.JugadorEntity;
import com.tdetroy.valuacion.entity.MovimientoEntity;
import com.tdetroy.valuacion.entity.OfertaP2PEntity;
import com.tdetroy.valuacion.entity.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.UsuarioEntity;
import com.tdetroy.valuacion.model.EstadoOferta;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.model.JugadorConPrecio;
import com.tdetroy.valuacion.model.Movimiento;
import com.tdetroy.valuacion.model.OfertaP2P;
import com.tdetroy.valuacion.model.OrdenMercado;
import com.tdetroy.valuacion.model.PaginaMercado;
import com.tdetroy.valuacion.model.TenenciaToken;
import com.tdetroy.valuacion.model.TipoMovimiento;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.repositories.CotizacionHistoricaRepository;
import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.MovimientoRepository;
import com.tdetroy.valuacion.repositories.OfertaP2PRepository;
import com.tdetroy.valuacion.repositories.TenenciaTokenRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import com.tdetroy.valuacion.repositories.ValorPorJugadorProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@inheritDoc} */
@Service
public class JugadorServiceImpl implements JugadorService {

    private static final String ENTIDAD_AUDITADA = "Jugador";
    private static final String ENTIDAD_USUARIO = "Usuario";
    private static final String ACCION_BAJA = "BAJA_JUGADOR";

    /**
     * Tope de {@code tamanio} de {@link #listarMercado} -- evita que un cliente pida una página
     * absurdamente grande por query param (plan.md §3bis); 50 (el default pedido por producto)
     * queda muy por debajo.
     */
    private static final int TAMANIO_PAGINA_MAXIMO = 100;

    private final JugadorRepository jugadorRepository;
    private final OfertaP2PRepository ofertaP2PRepository;
    private final TenenciaTokenRepository tenenciaTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final CotizacionHistoricaRepository cotizacionHistoricaRepository;
    private final MovimientoRepository movimientoRepository;
    private final AuditoriaService auditoriaService;
    private final BigDecimal valorInicial;

    public JugadorServiceImpl(
            JugadorRepository jugadorRepository,
            OfertaP2PRepository ofertaP2PRepository,
            TenenciaTokenRepository tenenciaTokenRepository,
            UsuarioRepository usuarioRepository,
            CotizacionHistoricaRepository cotizacionHistoricaRepository,
            MovimientoRepository movimientoRepository,
            AuditoriaService auditoriaService,
            @Value("${app.cotizacion.valor-inicial}") BigDecimal valorInicial) {
        this.jugadorRepository = jugadorRepository;
        this.ofertaP2PRepository = ofertaP2PRepository;
        this.tenenciaTokenRepository = tenenciaTokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.cotizacionHistoricaRepository = cotizacionHistoricaRepository;
        this.movimientoRepository = movimientoRepository;
        this.auditoriaService = auditoriaService;
        this.valorInicial = valorInicial;
    }

    @Override
    public List<Jugador> listar() {
        return jugadorRepository.findAll().stream().map(JugadorEntity::aModelo).toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Trae el catálogo completo y el valor vigente/inicial de *todos* los jugadores en 3
     * consultas en total (nunca una por jugador) y filtra/ordena/pagina en memoria -- al tamaño
     * actual del catálogo (del orden de miles de filas) eso es un trabajo trivial para el proceso
     * Java, y evita la complejidad de expresar "posición/búsqueda + orden por cotización vigente +
     * LIMIT/OFFSET" como una única consulta SQL contra una cotización que vive en una tabla
     * separada. Si el catálogo creciera a un volumen donde esto deje de ser trivial, el siguiente
     * paso sería mover el filtro+orden+paginado a SQL -- no antes, para no anticipar una necesidad
     * que todavía no existe (constitution.md: no sobre-diseñar).
     */
    @Override
    public PaginaMercado listarMercado(
            int pagina, int tamanio, String orden, String posicion, String busqueda) {
        int paginaNormalizada = Math.max(pagina, 0);
        int tamanioNormalizado = Math.min(Math.max(tamanio, 1), TAMANIO_PAGINA_MAXIMO);

        List<Jugador> catalogoCompleto =
                jugadorRepository.findAll().stream().map(JugadorEntity::aModelo).toList();
        Map<UUID, ValorPorJugadorProjection> vigentes =
                porJugador(cotizacionHistoricaRepository.findValoresVigentes());
        Map<UUID, ValorPorJugadorProjection> iniciales =
                porJugador(cotizacionHistoricaRepository.findValoresIniciales());

        List<String> posicionesDisponibles =
                catalogoCompleto.stream()
                        .map(Jugador::getPosicion)
                        .filter(p -> p != null && !p.isBlank() && !"null".equals(p))
                        .collect(Collectors.toCollection(LinkedHashSet::new))
                        .stream()
                        .sorted()
                        .toList();

        List<JugadorConPrecio> conPrecio =
                catalogoCompleto.stream()
                        .map(j -> aJugadorConPrecio(j, vigentes, iniciales))
                        .filter(item -> coincidePosicion(item.jugador(), posicion))
                        .filter(item -> coincideBusqueda(item.jugador(), busqueda))
                        .sorted(comparadorPara(OrdenMercado.desde(orden)))
                        .toList();

        long totalElementos = conPrecio.size();
        int totalPaginas = (int) Math.ceil(totalElementos / (double) tamanioNormalizado);
        int desde = Math.min(paginaNormalizada * tamanioNormalizado, conPrecio.size());
        int hasta = Math.min(desde + tamanioNormalizado, conPrecio.size());

        return new PaginaMercado(
                conPrecio.subList(desde, hasta),
                paginaNormalizada,
                tamanioNormalizado,
                totalElementos,
                totalPaginas,
                posicionesDisponibles);
    }

    private static Map<UUID, ValorPorJugadorProjection> porJugador(
            List<ValorPorJugadorProjection> filas) {
        return filas.stream()
                .collect(
                        Collectors.toMap(
                                ValorPorJugadorProjection::getJugadorId, Function.identity()));
    }

    private static JugadorConPrecio aJugadorConPrecio(
            Jugador jugador,
            Map<UUID, ValorPorJugadorProjection> vigentes,
            Map<UUID, ValorPorJugadorProjection> iniciales) {
        ValorPorJugadorProjection vigente = vigentes.get(jugador.getId());
        ValorPorJugadorProjection inicial = iniciales.get(jugador.getId());
        BigDecimal precio = vigente == null ? null : vigente.getValor();
        BigDecimal variacion = variacion(vigente, inicial);
        return new JugadorConPrecio(jugador, precio, variacion);
    }

    /**
     * {@code null} si el jugador nunca tuvo cotización, o si tuvo una sola (vigente == inicial,
     * mismo {@code fechaCalculo}: no hay desde dónde medir variación) -- mismo criterio que tenía
     * el cálculo que antes hacía el frontend con el historial completo (plan.md §3bis).
     */
    private static BigDecimal variacion(
            ValorPorJugadorProjection vigente, ValorPorJugadorProjection inicial) {
        if (vigente == null || inicial == null) {
            return null;
        }
        if (vigente.getFechaCalculo().equals(inicial.getFechaCalculo())) {
            return null;
        }
        if (inicial.getValor().signum() == 0) {
            return null;
        }
        return vigente.getValor()
                .subtract(inicial.getValor())
                .divide(inicial.getValor(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private static boolean coincidePosicion(Jugador jugador, String posicion) {
        if (posicion == null || posicion.isBlank() || "Todos".equals(posicion)) {
            return true;
        }
        return posicion.equals(jugador.getPosicion());
    }

    private static boolean coincideBusqueda(Jugador jugador, String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return true;
        }
        String q = busqueda.trim().toLowerCase();
        return jugador.getNombre().toLowerCase().contains(q)
                || jugador.getClub().toLowerCase().contains(q);
    }

    /**
     * {@code nullsLast} se compone *por fuera* de {@code reverseOrder()} (nunca {@code .reversed()}
     * sobre el comparator ya armado) a propósito: invertir el comparator entero movería los {@code
     * null} (precio/variación ausente) al principio en los órdenes descendentes, cuando siempre
     * deben quedar al final sin importar la dirección.
     */
    private static Comparator<JugadorConPrecio> comparadorPara(OrdenMercado orden) {
        return switch (orden) {
            case NOMBRE -> Comparator.comparing(item -> item.jugador().getNombre());
            case PRECIO_ASC ->
                    Comparator.comparing(
                            JugadorConPrecio::precio,
                            Comparator.nullsLast(Comparator.naturalOrder()));
            case PRECIO_DESC ->
                    Comparator.comparing(
                            JugadorConPrecio::precio,
                            Comparator.nullsLast(Comparator.reverseOrder()));
            case SUBA_DESC ->
                    Comparator.comparing(
                            JugadorConPrecio::variacionPorcentual,
                            Comparator.nullsLast(Comparator.reverseOrder()));
            case DISPONIBLES_DESC ->
                    Comparator.comparingInt(
                                    (JugadorConPrecio item) -> item.jugador().tokensDisponibles())
                            .reversed();
        };
    }

    @Override
    public Jugador obtenerPorId(UUID jugadorId) {
        return jugadorRepository
                .findById(jugadorId)
                .map(JugadorEntity::aModelo)
                .orElseThrow(() -> new RecursoNoEncontradoException(ENTIDAD_AUDITADA, jugadorId));
    }

    @Override
    @Transactional
    public Jugador darAlta(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            String liga,
            UUID actorId) {
        Jugador jugador =
                jugadorRepository
                        .save(
                                JugadorEntity.desde(
                                        Jugador.darAlta(
                                                nombre,
                                                club,
                                                posicion,
                                                fechaNacimiento,
                                                nacionalidad,
                                                liga)))
                        .aModelo();

        auditoriaService.registrar(
                actorId,
                "ALTA_JUGADOR",
                ENTIDAD_AUDITADA,
                jugador.getId(),
                null,
                datosIdentificatoriosDe(jugador));

        return jugador;
    }

    @Override
    @Transactional
    public Jugador editar(
            UUID jugadorId,
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            String liga,
            UUID actorId) {
        Jugador jugador =
                jugadorRepository
                        .findById(jugadorId)
                        .map(JugadorEntity::aModelo)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                ENTIDAD_AUDITADA, jugadorId));

        DatosIdentificatoriosAuditoria antes = datosIdentificatoriosDe(jugador);
        jugador.actualizarDatosIdentificatorios(
                nombre, club, posicion, fechaNacimiento, nacionalidad, liga);
        jugadorRepository.save(JugadorEntity.desde(jugador));

        auditoriaService.registrar(
                actorId,
                "EDICION_JUGADOR",
                ENTIDAD_AUDITADA,
                jugadorId,
                antes,
                datosIdentificatoriosDe(jugador));

        return jugador;
    }

    @Override
    @Transactional
    public Jugador darDeBaja(UUID jugadorId, UUID actorId) {
        Jugador jugador =
                jugadorRepository
                        .findById(jugadorId)
                        .map(JugadorEntity::aModelo)
                        .orElseThrow(
                                () ->
                                        new RecursoNoEncontradoException(
                                                ENTIDAD_AUDITADA, jugadorId));

        cancelarOfertasAbiertasDe(jugadorId);
        BigDecimal cotizacionReferencia = cotizacionVigenteDe(jugadorId);
        List<CompensacionBaja> compensaciones =
                compensarTenenciasDe(jugadorId, cotizacionReferencia);

        jugador.darDeBaja();
        jugadorRepository.save(JugadorEntity.desde(jugador));

        auditoriaService.registrar(
                actorId,
                ACCION_BAJA,
                ENTIDAD_AUDITADA,
                jugadorId,
                null,
                new BajaJugadorAuditoria(cotizacionReferencia, compensaciones));

        return jugador;
    }

    /**
     * Paso 1 de la baja (plan.md §5/§9.5): cada oferta {@code ABIERTA} de este jugador se cancela y
     * la cantidad que tenía reservada se reintegra a la tenencia de su vendedor — antes de
     * compensar, para que esa tenencia quede completa al valorizarla.
     */
    private void cancelarOfertasAbiertasDe(UUID jugadorId) {
        for (OfertaP2PEntity ofertaEntity :
                ofertaP2PRepository.findByJugadorIdAndEstado(jugadorId, EstadoOferta.ABIERTA)) {
            // Re-lee bajo @Lock(PESSIMISTIC_WRITE) (igual que OfertaServiceImpl.ejecutar/cancelar,
            // plan.md §9.3): evita pisar una compra concurrente que esté ejecutándose sobre esta
            // misma oferta en este preciso momento.
            OfertaP2P oferta =
                    ofertaP2PRepository.findById(ofertaEntity.getId()).orElseThrow().aModelo();

            TenenciaToken tenenciaVendedor =
                    obtenerOAbrirTenencia(oferta.getVendedorId(), jugadorId);
            tenenciaVendedor.liberarReserva(oferta.getCantidadDisponible());
            tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenenciaVendedor));

            oferta.cancelar();
            ofertaP2PRepository.save(OfertaP2PEntity.desde(oferta));
        }
    }

    /**
     * Paso 2 de la baja (plan.md §5, UC-15): cada tenencia con {@code cantidad > 0} de este jugador
     * (ya sin reserva pendiente, tras {@link #cancelarOfertasAbiertasDe}) se pone a cero, su
     * usuario recibe saldo virtual equivalente a {@code cotizacionReferencia}, y queda registrado
     * un {@link Movimiento} {@code COMPENSACION_BAJA} — nunca se libera la emisión del jugador
     * (constitution.md §2, spec.md regla 9: a diferencia de una venta, estos tokens se retiran de
     * circulación de forma permanente).
     */
    private List<CompensacionBaja> compensarTenenciasDe(
            UUID jugadorId, BigDecimal cotizacionReferencia) {
        List<CompensacionBaja> compensaciones = new ArrayList<>();
        for (TenenciaTokenEntity tenenciaEntity :
                tenenciaTokenRepository.findByJugadorIdAndCantidadGreaterThan(jugadorId, 0)) {
            TenenciaToken tenencia = tenenciaEntity.aModelo();
            UUID usuarioId = tenencia.getUsuarioId();
            int cantidadRetirada = tenencia.getCantidad();
            tenencia.debitar(cantidadRetirada);
            tenenciaTokenRepository.save(TenenciaTokenEntity.desde(tenencia));

            Usuario usuario = obtenerUsuario(usuarioId);
            BigDecimal monto =
                    Monetario.escalar(
                            cotizacionReferencia.multiply(BigDecimal.valueOf(cantidadRetirada)));
            usuario.acreditarSaldo(monto);
            usuarioRepository.save(UsuarioEntity.desde(usuario));

            movimientoRepository.save(
                    MovimientoEntity.desde(
                            Movimiento.registrar(
                                    usuarioId,
                                    jugadorId,
                                    TipoMovimiento.COMPENSACION_BAJA,
                                    cantidadRetirada,
                                    cotizacionReferencia,
                                    monto,
                                    null)));

            compensaciones.add(new CompensacionBaja(usuarioId, cantidadRetirada, monto));
        }
        return compensaciones;
    }

    private Usuario obtenerUsuario(UUID usuarioId) {
        return usuarioRepository
                .findById(usuarioId)
                .map(UsuarioEntity::aModelo)
                .orElseThrow(() -> new RecursoNoEncontradoException(ENTIDAD_USUARIO, usuarioId));
    }

    private TenenciaToken obtenerOAbrirTenencia(UUID usuarioId, UUID jugadorId) {
        return tenenciaTokenRepository
                .findByUsuarioIdAndJugadorId(usuarioId, jugadorId)
                .map(TenenciaTokenEntity::aModelo)
                .orElseGet(() -> TenenciaToken.abrir(usuarioId, jugadorId));
    }

    private BigDecimal cotizacionVigenteDe(UUID jugadorId) {
        return cotizacionHistoricaRepository
                .findTopByJugadorIdOrderByFechaCalculoDesc(jugadorId)
                .map(CotizacionHistoricaEntity::getValor)
                .orElse(valorInicial);
    }

    private static DatosIdentificatoriosAuditoria datosIdentificatoriosDe(Jugador jugador) {
        return new DatosIdentificatoriosAuditoria(
                jugador.getNombre(),
                jugador.getClub(),
                jugador.getPosicion(),
                jugador.getFechaNacimiento(),
                jugador.getNacionalidad(),
                jugador.getLiga());
    }

    private record DatosIdentificatoriosAuditoria(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            String liga) {}

    private record CompensacionBaja(UUID usuarioId, int cantidadCompensada, BigDecimal monto) {}

    private record BajaJugadorAuditoria(
            BigDecimal cotizacionReferencia, List<CompensacionBaja> compensaciones) {}
}
