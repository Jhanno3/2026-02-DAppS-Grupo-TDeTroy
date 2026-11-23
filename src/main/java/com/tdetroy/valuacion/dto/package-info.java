/**
 * DTOs de la API ({@code record}, inmutables), agrupados por dominio.
 *
 * <p>Entrada: {@code <Acción><Entidad>Request}, validados con Bean Validation en el borde del
 * {@link com.tdetroy.valuacion.controllers} (constitution.md §2/§4).
 *
 * <p>Salida: {@code <Entidad>Response}. Nunca se exponen entidades de {@link
 * com.tdetroy.valuacion.model} directamente como respuesta de la API (constitution.md §1/§2).
 */
package com.tdetroy.valuacion.dto;
