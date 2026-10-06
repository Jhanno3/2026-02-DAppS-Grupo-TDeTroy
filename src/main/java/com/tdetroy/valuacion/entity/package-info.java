/**
 * Entidades JPA (Jugador, Usuario, TenenciaToken, Movimiento, RegistroAuditoria,
 * RendimientoPartido, ...): reflejan el esquema de PostgreSQL 1:1 y no contienen lógica de negocio
 * ni invariantes propias — eso vive exclusivamente en {@link com.tdetroy.valuacion.model}
 * (constitution.md §2).
 *
 * <p>Cada entidad conoce su clase de {@link com.tdetroy.valuacion.model} equivalente y expone
 * {@code desde(Model)}/{@code aModelo()} para mapear en el límite entre {@code services/} y {@code
 * repositories/} — nunca al revés: {@code model/} no conoce esta capa.
 */
package com.tdetroy.valuacion.entity;
