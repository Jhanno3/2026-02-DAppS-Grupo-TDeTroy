-- Usuario (T1.1, plan.md §2.1): cuenta con saldo virtual interno. email es el identificador
-- unico de registro/login (UC-01/UC-02). saldo_virtual nunca se actualiza por SQL directo fuera
-- de la app -- siempre via Usuario.debitarSaldo/acreditarSaldo (constitution.md S2).
CREATE TABLE usuarios (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    rol             VARCHAR(10) NOT NULL,
    saldo_virtual   NUMERIC(19, 2) NOT NULL,
    fecha_creacion  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);
