CREATE OR REPLACE FUNCTION fn_registros_auditoria_inmutable()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'registros_auditoria es append-only: no se permiten modificaciones ni eliminaciones';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_registros_auditoria_inmutable
BEFORE UPDATE OR DELETE ON registros_auditoria
FOR EACH ROW
EXECUTE FUNCTION fn_registros_auditoria_inmutable();

CREATE OR REPLACE FUNCTION fn_auditar_saldo_usuario()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.saldo_virtual IS DISTINCT FROM NEW.saldo_virtual THEN
        INSERT INTO registros_auditoria (
            id, actor_id, accion, entidad_afectada, entidad_id, valores_antes, valores_despues, fecha
        ) VALUES (
            gen_random_uuid(),
            NULL,
            'TRIGGER_SALDO_USUARIO',
            'Usuario',
            NEW.id,
            jsonb_build_object('saldoVirtual', OLD.saldo_virtual),
            jsonb_build_object('saldoVirtual', NEW.saldo_virtual),
            NOW()
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditar_saldo_usuario
AFTER UPDATE OF saldo_virtual ON usuarios
FOR EACH ROW
EXECUTE FUNCTION fn_auditar_saldo_usuario();

CREATE OR REPLACE FUNCTION fn_auditar_tenencia_token()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO registros_auditoria (
            id, actor_id, accion, entidad_afectada, entidad_id, valores_antes, valores_despues, fecha
        ) VALUES (
            gen_random_uuid(),
            NULL,
            'TRIGGER_ALTA_TENENCIA',
            'TenenciaToken',
            NEW.id,
            NULL,
            jsonb_build_object(
                'usuarioId', NEW.usuario_id,
                'jugadorId', NEW.jugador_id,
                'cantidad', NEW.cantidad,
                'cantidadReservada', NEW.cantidad_reservada
            ),
            NOW()
        );
    ELSIF OLD.cantidad IS DISTINCT FROM NEW.cantidad
       OR OLD.cantidad_reservada IS DISTINCT FROM NEW.cantidad_reservada THEN
        INSERT INTO registros_auditoria (
            id, actor_id, accion, entidad_afectada, entidad_id, valores_antes, valores_despues, fecha
        ) VALUES (
            gen_random_uuid(),
            NULL,
            'TRIGGER_TENENCIA_TOKEN',
            'TenenciaToken',
            NEW.id,
            jsonb_build_object(
                'usuarioId', OLD.usuario_id,
                'jugadorId', OLD.jugador_id,
                'cantidad', OLD.cantidad,
                'cantidadReservada', OLD.cantidad_reservada
            ),
            jsonb_build_object(
                'usuarioId', NEW.usuario_id,
                'jugadorId', NEW.jugador_id,
                'cantidad', NEW.cantidad,
                'cantidadReservada', NEW.cantidad_reservada
            ),
            NOW()
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditar_tenencia_token
AFTER INSERT OR UPDATE OF cantidad, cantidad_reservada ON tenencias_token
FOR EACH ROW
EXECUTE FUNCTION fn_auditar_tenencia_token();
