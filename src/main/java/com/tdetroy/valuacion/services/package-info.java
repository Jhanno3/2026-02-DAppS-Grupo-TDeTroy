/**
 * Capa de lógica de negocio: interfaz {@code <Entidad>Service} + implementación {@code <Entidad>ServiceImpl},
 * inyectada por constructor.
 *
 * <p>Orquesta operaciones, aplica reglas de dominio, coordina transacciones ({@code @Transactional})
 * y llama a uno o varios {@link com.tdetroy.valuacion.repositories}. No conoce nada de HTTP
 * (constitution.md §2). Cada Service tiene responsabilidad acotada a una entidad/proceso concreto
 * (prohibido el patrón "God Service").
 */
package com.tdetroy.valuacion.services;
