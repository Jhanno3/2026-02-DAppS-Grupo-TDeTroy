package com.tdetroy.valuacion.controllers;

import com.tdetroy.valuacion.config.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.request.CrearJugadorRequest;
import com.tdetroy.valuacion.dto.request.EditarJugadorRequest;
import com.tdetroy.valuacion.dto.response.CotizacionResponse;
import com.tdetroy.valuacion.dto.response.EstadisticasTemporadaResponse;
import com.tdetroy.valuacion.dto.response.JugadorResponse;
import com.tdetroy.valuacion.dto.response.PaginaMercadoResponse;
import com.tdetroy.valuacion.model.EstrategiaCotizacion;
import com.tdetroy.valuacion.model.Jugador;
import com.tdetroy.valuacion.services.CotizacionService;
import com.tdetroy.valuacion.services.JugadorService;
import com.tdetroy.valuacion.services.RendimientoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo de jugadores (plan.md §3, UC-03/UC-04/UC-07/UC-08/UC-15). {@code GET} es público (UC-07/
 * UC-08, ver {@code config/SecurityConfig}); {@code POST}/{@code PUT}/baja quedan restringidos a
 * {@code ADMIN} vía {@link PreAuthorize}, nunca con un chequeo de rol a mano (constitution.md §4).
 */
@RestController
@RequestMapping("/api/v1/jugadores")
public class JugadorController {

    private final JugadorService jugadorService;
    private final CotizacionService cotizacionService;
    private final RendimientoService rendimientoService;

    public JugadorController(
            JugadorService jugadorService,
            CotizacionService cotizacionService,
            RendimientoService rendimientoService) {
        this.jugadorService = jugadorService;
        this.cotizacionService = cotizacionService;
        this.rendimientoService = rendimientoService;
    }

    /** UC-07: catálogo completo, incluidos los jugadores dados de baja (spec.md UC-07). */
    @GetMapping
    public List<JugadorResponse> listar() {
        return jugadorService.listar().stream().map(JugadorResponse::desde).toList();
    }

    /**
     * UC-07: catálogo paginado con cotización vigente embebida (plan.md §3bis) -- la pantalla de
     * Mercado usa este endpoint, no {@link #listar}, justamente para no tener que pedir la
     * cotización de a un jugador por vez. {@code orden} acepta los mismos valores que ya ofrecía el
     * filtro del frontend ({@code nombre}/{@code precio-desc}/{@code precio-asc}/{@code suba}/
     * {@code disponibles}); cualquier otro valor (incluido ausente) cae en {@code precio-desc} —
     * ver {@code model.OrdenMercado#desde}.
     */
    @GetMapping("/mercado")
    public PaginaMercadoResponse mercado(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanio,
            @RequestParam(defaultValue = "precio-desc") String orden,
            @RequestParam(required = false) String posicion,
            @RequestParam(required = false) String busqueda) {
        return PaginaMercadoResponse.desde(
                jugadorService.listarMercado(pagina, tamanio, orden, posicion, busqueda));
    }

    /** UC-07: detalle de un jugador puntual; 404 vía {@code RecursoNoEncontradoException}. */
    @GetMapping("/{id}")
    public JugadorResponse obtenerPorId(@PathVariable UUID id) {
        return JugadorResponse.desde(jugadorService.obtenerPorId(id));
    }

    /**
     * UC-08: historial cronológico de cotizaciones de un jugador puntual, incluidos los dados de
     * baja (su cotización queda congelada en el último registro, UC-15). 404 vía {@code
     * RecursoNoEncontradoException} si el jugador no existe. Permite filtrar opcionalmente por
     * {@link EstrategiaCotizacion} vía {@code ?estrategia=...}.
     */
    @GetMapping("/{id}/cotizaciones")
    public List<CotizacionResponse> obtenerCotizaciones(
            @PathVariable UUID id,
            @RequestParam(required = false) EstrategiaCotizacion estrategia) {
        List<com.tdetroy.valuacion.model.CotizacionHistorica> historial =
                estrategia != null
                        ? cotizacionService.obtenerHistorial(id, estrategia)
                        : cotizacionService.obtenerHistorial(id);
        return historial.stream().map(CotizacionResponse::desde).toList();
    }

    /**
     * Cotización de un jugador en una semana ISO puntual ({@code yyyy-Www}, ej. {@code 2026-W44}):
     * la que resultó del rendimiento de esa semana. Si se calculó más de una vez en esa semana,
     * devuelve la última. {@code ?estrategia=} opcional. 400 si la semana no tiene ese formato, 404
     * si el jugador no existe o no tiene cotización en esa semana.
     */
    @GetMapping("/{id}/cotizaciones/{semana}")
    public CotizacionResponse obtenerCotizacionDeSemana(
            @PathVariable UUID id,
            @PathVariable @Pattern(regexp = "^\\d{4}-W\\d{2}$") String semana,
            @RequestParam(required = false) EstrategiaCotizacion estrategia) {
        return CotizacionResponse.desde(cotizacionService.obtenerDeSemana(id, semana, estrategia));
    }

    /**
     * UC-07: acumulado de temporada del jugador (último snapshot de WhoScored). {@code 204} si
     * todavía no hay datos de rendimiento; 404 vía {@code RecursoNoEncontradoException} si el
     * jugador no existe.
     */
    @GetMapping("/{id}/estadisticas")
    public ResponseEntity<EstadisticasTemporadaResponse> obtenerEstadisticas(
            @PathVariable UUID id) {
        return rendimientoService
                .obtenerEstadisticasTemporada(id)
                .map(EstadisticasTemporadaResponse::desde)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** UC-03. {@code actorId} sale del JWT ya validado, nunca del body de la request. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JugadorResponse> darAlta(
            @Valid @RequestBody CrearJugadorRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Jugador jugador =
                jugadorService.darAlta(
                        request.nombre(),
                        request.club(),
                        request.posicion(),
                        request.fechaNacimiento(),
                        request.nacionalidad(),
                        request.liga(),
                        principal.getUsuarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(JugadorResponse.desde(jugador));
    }

    /** UC-04. Nunca toca cotización ni límite de tokens — eso lo garantiza {@code Jugador}. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public JugadorResponse editar(
            @PathVariable UUID id,
            @Valid @RequestBody EditarJugadorRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Jugador jugador =
                jugadorService.editar(
                        id,
                        request.nombre(),
                        request.club(),
                        request.posicion(),
                        request.fechaNacimiento(),
                        request.nacionalidad(),
                        request.liga(),
                        principal.getUsuarioId());
        return JugadorResponse.desde(jugador);
    }

    /**
     * UC-15. Inactivación + compensación de cada usuario afectado (no es un {@code DELETE} físico,
     * plan.md §3). {@code actorId} sale del JWT ya validado, nunca del body de la request.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('ADMIN')")
    public JugadorResponse darDeBaja(
            @PathVariable UUID id, @AuthenticationPrincipal UsuarioPrincipal principal) {
        Jugador jugador = jugadorService.darDeBaja(id, principal.getUsuarioId());
        return JugadorResponse.desde(jugador);
    }
}
