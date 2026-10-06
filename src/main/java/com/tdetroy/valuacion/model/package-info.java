/**
 * Objetos de dominio puros (Jugador, Usuario, TenenciaToken, CotizacionHistorica, Movimiento,
 * OfertaP2P, RegistroAuditoria, ...), sin ninguna anotación de persistencia — eso vive en {@link
 * com.tdetroy.valuacion.entity} (constitution.md §2).
 *
 * <p>Los objetos con invariantes financieras exponen métodos propios que validan la operación (p.
 * ej. {@code acreditarTokens}, {@code debitarTokens}) en lugar de setters públicos que permitan
 * mutación arbitraria del estado (constitution.md §2). Nunca se exponen directamente como respuesta
 * de la API: se convierten a {@link com.tdetroy.valuacion.dto} antes de cruzar el borde del
 * Controller.
 */
package com.tdetroy.valuacion.model;
