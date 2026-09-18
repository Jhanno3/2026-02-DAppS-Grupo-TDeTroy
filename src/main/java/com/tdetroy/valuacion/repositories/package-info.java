/**
 * Capa de acceso a datos: interfaces de Repository + implementaciones.
 *
 * <p>Incluye tanto los repositorios Spring Data JPA sobre {@link com.tdetroy.valuacion.model} como
 * las interfaces propias de las fuentes externas (WhoScored, Football-Data.org) y sus
 * implementaciones ({@code WhoScoredRepositoryImpl}, {@code FootballDataRepositoryImpl}), que
 * traducen la respuesta externa al modelo interno. Nunca contiene lógica de negocio ni conoce a
 * {@link com.tdetroy.valuacion.services} (constitution.md §1/§2).
 */
package com.tdetroy.valuacion.repositories;
