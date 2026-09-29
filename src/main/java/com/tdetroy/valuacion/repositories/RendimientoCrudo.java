package com.tdetroy.valuacion.repositories;

import java.time.LocalDate;
import java.util.Map;

/**
 * Rendimiento crudo de un jugador en un partido puntual, tal como lo traduce una implementación de
 * {@link RendimientoExternoRepository} desde el formato propio de su fuente externa (WhoScored,
 * plan.md §8.1) — todavía sin persistir como {@link
 * com.tdetroy.valuacion.model.RendimientoPartido}, eso es responsabilidad de {@code
 * RendimientoService} (tasks.md T3.5). {@code metricas} llega ya como mapa clave/valor sin curar
 * (spec.md UC-05); {@code RendimientoService} lo serializa a JSON recién al construir la entidad,
 * para no acoplar este contrato de transporte a ninguna librería JSON concreta.
 */
public record RendimientoCrudo(
        String partidoExternoId, LocalDate fechaPartido, Map<String, Object> metricas) {}
