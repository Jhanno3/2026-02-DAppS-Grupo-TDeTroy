package com.tdetroy.valuacion.model;

import com.tdetroy.valuacion.common.exceptions.EmisionMaximaSuperadaException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Jugador tokenizable del catálogo (plan.md §2.2, UC-03/UC-04/UC-15).
 *
 * <p>{@code tokensEmitidos} nunca se expone con un setter público: sólo se mueve a través de {@link
 * #emitirTokens} y {@link #liberarTokens}, que validan la invariante de emisión máxima en la propia
 * entidad (constitution.md §2) — nunca queda a criterio del Service que las invoca validar ese
 * máximo por su cuenta. Representa tokens en circulación (comprados y no devueltos); "tokens
 * disponibles para compra" = {@code 100 - tokensEmitidos}.
 *
 * <p>{@code estado} sólo se muta a través de {@link #darDeBaja()} (UC-15): al pasar a {@link
 * EstadoJugador#INACTIVO}, {@code tokensEmitidos} queda congelado como registro histórico y deja de
 * aceptar operaciones — eso lo valida el Service que orquesta la baja (T6.6), no esta entidad.
 */
@Getter
public class Jugador {

    private static final int MAXIMO_TOKENS_EMITIDOS = 100;

    private final UUID id;
    private String nombre;
    private String club;
    private String posicion;
    private LocalDate fechaNacimiento;
    private String nacionalidad;
    private EstadoJugador estado;
    private int tokensEmitidos;
    private UUID cotizacionVigenteId;
    private Instant fechaUltimaActualizacionRendimiento;

    private Jugador(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad) {
        validarCampos(nombre, club, posicion, fechaNacimiento, nacionalidad);

        this.id = UUID.randomUUID();
        this.nombre = nombre;
        this.club = club;
        this.posicion = posicion;
        this.fechaNacimiento = fechaNacimiento;
        this.nacionalidad = nacionalidad;
        this.estado = EstadoJugador.ACTIVO;
        this.tokensEmitidos = 0;
        this.cotizacionVigenteId = null;
        this.fechaUltimaActualizacionRendimiento = null;
    }

    /**
     * Da de alta un jugador {@link EstadoJugador#ACTIVO} con emisión disponible de hasta 100 tokens
     * y sin cotización calculada (UC-03). Único punto de creación de {@code Jugador}.
     */
    public static Jugador darAlta(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad) {
        return new Jugador(nombre, club, posicion, fechaNacimiento, nacionalidad);
    }

    private Jugador(
            UUID id,
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            EstadoJugador estado,
            int tokensEmitidos,
            UUID cotizacionVigenteId,
            Instant fechaUltimaActualizacionRendimiento) {
        this.id = id;
        this.nombre = nombre;
        this.club = club;
        this.posicion = posicion;
        this.fechaNacimiento = fechaNacimiento;
        this.nacionalidad = nacionalidad;
        this.estado = estado;
        this.tokensEmitidos = tokensEmitidos;
        this.cotizacionVigenteId = cotizacionVigenteId;
        this.fechaUltimaActualizacionRendimiento = fechaUltimaActualizacionRendimiento;
    }

    /**
     * Reconstruye un {@code Jugador} ya persistido a partir de sus datos crudos (usado por {@code
     * entity/JugadorEntity#aModelo()} en el límite con {@code repositories/}) — a diferencia de
     * {@link #darAlta}, no valida ni aplica defaults de alta: el estado ya fue validado cuando se
     * creó originalmente.
     */
    public static Jugador reconstruir(
            UUID id,
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            EstadoJugador estado,
            int tokensEmitidos,
            UUID cotizacionVigenteId,
            Instant fechaUltimaActualizacionRendimiento) {
        return new Jugador(
                id,
                nombre,
                club,
                posicion,
                fechaNacimiento,
                nacionalidad,
                estado,
                tokensEmitidos,
                cotizacionVigenteId,
                fechaUltimaActualizacionRendimiento);
    }

    /**
     * Emite {@code cantidad} tokens nuevos hacia circulación (compra al sistema, UC-09).
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero
     * @throws EmisionMaximaSuperadaException si {@code tokensEmitidos + cantidad} supera 100
     */
    public void emitirTokens(int cantidad) {
        requirePositivo(cantidad, "cantidad");
        if (tokensEmitidos + cantidad > MAXIMO_TOKENS_EMITIDOS) {
            throw new EmisionMaximaSuperadaException(
                    tokensEmitidos, cantidad, MAXIMO_TOKENS_EMITIDOS);
        }
        this.tokensEmitidos += cantidad;
    }

    /**
     * Libera {@code cantidad} tokens de circulación, devolviéndolos a la emisión disponible (venta
     * al sistema, UC-10; constitution.md §2, regla 8 de spec.md §5).
     *
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero, o si supera {@code
     *     tokensEmitidos}
     */
    public void liberarTokens(int cantidad) {
        requirePositivo(cantidad, "cantidad");
        if (cantidad > tokensEmitidos) {
            throw new IllegalArgumentException(
                    "cantidad a liberar (%d) no puede superar tokensEmitidos actual (%d)"
                            .formatted(cantidad, tokensEmitidos));
        }
        this.tokensEmitidos -= cantidad;
    }

    /**
     * Tokens disponibles para compra sobre el máximo de 100 (UC-07). Un jugador {@link
     * EstadoJugador#INACTIVO} siempre devuelve 0, aunque {@code tokensEmitidos} haya quedado
     * congelado en un valor menor a 100 (spec.md UC-07: sus tokens salieron de circulación de forma
     * permanente en la baja, UC-15).
     */
    public int tokensDisponibles() {
        return estado == EstadoJugador.INACTIVO ? 0 : MAXIMO_TOKENS_EMITIDOS - tokensEmitidos;
    }

    /**
     * Actualiza los datos identificatorios del jugador (UC-04). Nunca toca {@code estado}, {@code
     * tokensEmitidos} ni {@code cotizacionVigenteId} — la edición identificatoria jamás afecta el
     * límite de tokens ni la cotización (spec.md UC-04).
     */
    public void actualizarDatosIdentificatorios(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad) {
        validarCampos(nombre, club, posicion, fechaNacimiento, nacionalidad);

        this.nombre = nombre;
        this.club = club;
        this.posicion = posicion;
        this.fechaNacimiento = fechaNacimiento;
        this.nacionalidad = nacionalidad;
    }

    /**
     * Inactiva el jugador (UC-15). {@code tokensEmitidos} queda congelado como registro histórico;
     * la cancelación de ofertas abiertas y la compensación de tenencias son responsabilidad del
     * Service que orquesta la baja (T6.6), no de este método.
     */
    public void darDeBaja() {
        this.estado = EstadoJugador.INACTIVO;
    }

    /**
     * Registra {@code fecha} como el momento de la ingesta de rendimiento más reciente para este
     * jugador (UC-05, plan.md §8.1) — {@code RendimientoServiceImpl} (tasks.md T3.5) la invoca cada
     * vez que persiste un nuevo {@code RendimientoPartido} con datos de WhoScored para este
     * jugador.
     */
    public void registrarActualizacionRendimiento(Instant fecha) {
        Objects.requireNonNull(fecha, "fecha no puede ser null");
        this.fechaUltimaActualizacionRendimiento = fecha;
    }

    private static void validarCampos(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad) {
        Objects.requireNonNull(nombre, "nombre no puede ser null");
        Objects.requireNonNull(club, "club no puede ser null");
        Objects.requireNonNull(posicion, "posicion no puede ser null");
        Objects.requireNonNull(fechaNacimiento, "fechaNacimiento no puede ser null");
        Objects.requireNonNull(nacionalidad, "nacionalidad no puede ser null");
        requireNoBlank(nombre, "nombre");
        requireNoBlank(club, "club");
        requireNoBlank(posicion, "posicion");
        requireNoBlank(nacionalidad, "nacionalidad");
    }

    private static void requireNoBlank(String valor, String nombreCampo) {
        if (valor.isBlank()) {
            throw new IllegalArgumentException(nombreCampo + " no puede estar vacío ni ser blanco");
        }
    }

    private static void requirePositivo(int valor, String nombreCampo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(
                    nombreCampo + " debe ser mayor a cero, fue " + valor);
        }
    }
}
