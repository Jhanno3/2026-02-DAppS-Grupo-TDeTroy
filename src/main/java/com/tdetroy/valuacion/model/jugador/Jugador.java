package com.tdetroy.valuacion.model.jugador;

import com.tdetroy.valuacion.common.exceptions.EmisionMaximaSuperadaException;
import com.tdetroy.valuacion.model.mercado.Token;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Jugador tokenizable del catálogo. Es el punto de entrada para emitir y liberar tokens; el máximo
 * lo valida {@link Token}.
 */
@Getter
public class Jugador {

    private final UUID id;
    private String nombre;
    private String club;
    private String posicion;
    private LocalDate fechaNacimiento;
    private String nacionalidad;
    private String liga;
    private EstadoJugador estado;

    @Getter(AccessLevel.NONE)
    private Token token;

    private UUID cotizacionVigenteId;
    private Instant fechaUltimaActualizacionRendimiento;
    private String fotoUrl;

    private Jugador(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            String liga) {
        validarCampos(nombre, club, posicion, fechaNacimiento, nacionalidad, liga);

        this.id = UUID.randomUUID();
        this.nombre = nombre;
        this.club = club;
        this.posicion = posicion;
        this.fechaNacimiento = fechaNacimiento;
        this.nacionalidad = nacionalidad;
        this.liga = liga;
        this.estado = EstadoJugador.ACTIVO;
        this.token = Token.nueva();
        this.cotizacionVigenteId = null;
        this.fechaUltimaActualizacionRendimiento = null;
        this.fotoUrl = null;
    }

    public static Jugador darAlta(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            String liga) {
        return new Jugador(nombre, club, posicion, fechaNacimiento, nacionalidad, liga);
    }

    private Jugador(
            UUID id,
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            String liga,
            EstadoJugador estado,
            int tokensEmitidos,
            UUID cotizacionVigenteId,
            Instant fechaUltimaActualizacionRendimiento,
            String fotoUrl) {
        this.id = id;
        this.nombre = nombre;
        this.club = club;
        this.posicion = posicion;
        this.fechaNacimiento = fechaNacimiento;
        this.nacionalidad = nacionalidad;
        this.liga = liga;
        this.estado = estado;
        this.token = Token.reconstruir(tokensEmitidos);
        this.cotizacionVigenteId = cotizacionVigenteId;
        this.fechaUltimaActualizacionRendimiento = fechaUltimaActualizacionRendimiento;
        this.fotoUrl = fotoUrl;
    }

    /** Reconstruye desde persistencia; no valida las invariantes de alta. */
    public static Jugador reconstruir(
            UUID id,
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            String liga,
            EstadoJugador estado,
            int tokensEmitidos,
            UUID cotizacionVigenteId,
            Instant fechaUltimaActualizacionRendimiento,
            String fotoUrl) {
        return new Jugador(
                id,
                nombre,
                club,
                posicion,
                fechaNacimiento,
                nacionalidad,
                liga,
                estado,
                tokensEmitidos,
                cotizacionVigenteId,
                fechaUltimaActualizacionRendimiento,
                fotoUrl);
    }

    /**
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero
     * @throws EmisionMaximaSuperadaException si se supera el máximo de 100 tokens emitidos
     */
    public void emitirTokens(int cantidad) {
        token.emitir(cantidad);
    }

    /**
     * @throws IllegalArgumentException si {@code cantidad} no es mayor a cero, o si supera la
     *     emitida
     */
    public void liberarTokens(int cantidad) {
        token.liberar(cantidad);
    }

    public int getTokensEmitidos() {
        return token.getCantidadEmitida();
    }

    /** Siempre 0 si está inactivo: sus tokens salieron de circulación en la baja. */
    public int tokensDisponibles() {
        return estado == EstadoJugador.INACTIVO ? 0 : token.disponibles();
    }

    /** Nunca toca estado, tokens emitidos ni cotización. */
    public void actualizarDatosIdentificatorios(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            String liga) {
        validarCampos(nombre, club, posicion, fechaNacimiento, nacionalidad, liga);

        this.nombre = nombre;
        this.club = club;
        this.posicion = posicion;
        this.fechaNacimiento = fechaNacimiento;
        this.nacionalidad = nacionalidad;
        this.liga = liga;
    }

    /** La emisión queda congelada. Cancelar ofertas y compensar tenencias es tarea del Service. */
    public void darDeBaja() {
        this.estado = EstadoJugador.INACTIVO;
    }

    public void registrarActualizacionRendimiento(Instant fecha) {
        Objects.requireNonNull(fecha, "fecha no puede ser null");
        this.fechaUltimaActualizacionRendimiento = fecha;
    }

    /** La foto viene de WhoScored en la ingesta; no es editable por un administrador. */
    public void actualizarFoto(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public void actualizarCotizacionVigente(UUID cotizacionId) {
        Objects.requireNonNull(cotizacionId, "cotizacionId no puede ser null");
        this.cotizacionVigenteId = cotizacionId;
    }

    private static void validarCampos(
            String nombre,
            String club,
            String posicion,
            LocalDate fechaNacimiento,
            String nacionalidad,
            String liga) {
        Objects.requireNonNull(nombre, "nombre no puede ser null");
        Objects.requireNonNull(club, "club no puede ser null");
        Objects.requireNonNull(posicion, "posicion no puede ser null");
        Objects.requireNonNull(fechaNacimiento, "fechaNacimiento no puede ser null");
        Objects.requireNonNull(nacionalidad, "nacionalidad no puede ser null");
        Objects.requireNonNull(liga, "liga no puede ser null");
        exigirNoVacio(nombre, "nombre");
        exigirNoVacio(club, "club");
        exigirNoVacio(posicion, "posicion");
        exigirNoVacio(nacionalidad, "nacionalidad");
        exigirNoVacio(liga, "liga");
    }

    private static void exigirNoVacio(String valor, String nombreCampo) {
        if (valor.isBlank()) {
            throw new IllegalArgumentException(nombreCampo + " no puede estar vacío ni ser blanco");
        }
    }
}
