/**
 * Capa de entrada HTTP: un {@code @RestController} por entidad/proceso de negocio.
 *
 * <p>Recibe la petición, valida forma de entrada (Bean Validation), mapea a DTOs/parámetros y
 * delega toda regla de negocio al {@link com.tdetroy.valuacion.services} correspondiente. No
 * contiene lógica de negocio ni accede a {@link com.tdetroy.valuacion.repositories} directamente
 * (constitution.md §2).
 *
 * <p>Agrupados por dominio, con los mismos nombres de subpaquete en todas las capas: {@code
 * usuario}, {@code jugador}, {@code cotizacion}, {@code rendimiento}, {@code mercado}, {@code
 * cartera} y {@code auditoria}.
 */
package com.tdetroy.valuacion.controllers;
