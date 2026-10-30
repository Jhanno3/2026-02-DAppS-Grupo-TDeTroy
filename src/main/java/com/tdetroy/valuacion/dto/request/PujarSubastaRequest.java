package com.tdetroy.valuacion.dto.request;

/**
 * {@code POST /subastas/{id}/pujas} (UC-18). {@code porcentaje} es el incremento sobre el precio
 * vigente: 5, 10 o 20 — o 0 para igualar el precio inicial como primera puja. El valor lo valida
 * {@code model/IncrementoPuja}, nunca se acepta un precio libre.
 */
public record PujarSubastaRequest(int porcentaje) {}
