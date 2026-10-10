-- PesoMetrica (T4.2, plan.md S2.9, S6.2): pesos del motor de cotizacion, en tabla de
-- configuracion ajustable en runtime -- nunca hardcodeados en el algoritmo. "clave" es la clave
-- primaria natural y debe matchear una key dentro de rendimientos_partido.metricas; una metrica
-- nueva que WhoScored empiece a proveer solo participa del calculo una vez que alguien le crea
-- una fila activa aca.
CREATE TABLE pesos_metrica (
    clave   VARCHAR(100) PRIMARY KEY,
    peso    NUMERIC(10,4) NOT NULL,
    activo  BOOLEAN NOT NULL DEFAULT true
);
