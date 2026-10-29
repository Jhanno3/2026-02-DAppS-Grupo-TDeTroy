package com.tdetroy.valuacion.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SemanaIsoTest {

    @Test
    void elLunesLaReferenciaEsLaSemanaQueAcabaDeTerminar() {
        LocalDate lunes = LocalDate.of(2026, 11, 2);

        assertThat(SemanaIso.de(SemanaIso.fechaDeReferencia(lunes))).isEqualTo("2026-W44");
    }

    @Test
    void aMitadDeSemanaLaReferenciaEsLaSemanaEnCurso() {
        LocalDate jueves = LocalDate.of(2026, 10, 29);

        assertThat(SemanaIso.de(SemanaIso.fechaDeReferencia(jueves))).isEqualTo("2026-W44");
    }

    @Test
    void usaElAnioDeLaSemanaIsoYNoElCalendario() {
        LocalDate lunesDeEnero = LocalDate.of(2027, 1, 4);

        assertThat(SemanaIso.de(SemanaIso.fechaDeReferencia(lunesDeEnero))).isEqualTo("2026-W53");
    }
}
