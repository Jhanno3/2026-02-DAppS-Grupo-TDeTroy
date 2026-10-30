-- El peso de una metrica siempre es > 0, asi que el signo de su aporte al puntaje no puede venir
-- del peso: sin esta columna, mas tarjetas puntuaba hacia arriba (bug conocido, plan.md S8.1).
ALTER TABLE pesos_metrica
ADD COLUMN sentido VARCHAR(10) NOT NULL DEFAULT 'POSITIVO';

-- Las tarjetas son un castigo (sentido NEGATIVO) y suben de peso: antes estaban subestimadas
-- frente a goals/manOfTheMatch/assists.
UPDATE pesos_metrica SET sentido = 'NEGATIVO', peso = 3.0000 WHERE clave = 'redCard';
UPDATE pesos_metrica SET sentido = 'NEGATIVO', peso = 1.0000 WHERE clave = 'yellowCard';
