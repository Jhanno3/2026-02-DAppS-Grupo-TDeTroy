package com.tdetroy.valuacion.repositories.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdetroy.valuacion.common.Monetario;
import com.tdetroy.valuacion.entity.auditoria.RegistroAuditoriaEntity;
import com.tdetroy.valuacion.entity.cartera.TenenciaTokenEntity;
import com.tdetroy.valuacion.entity.jugador.JugadorEntity;
import com.tdetroy.valuacion.entity.usuario.UsuarioEntity;
import com.tdetroy.valuacion.model.cartera.TenenciaToken;
import com.tdetroy.valuacion.model.jugador.Jugador;
import com.tdetroy.valuacion.model.usuario.Usuario;
import com.tdetroy.valuacion.repositories.cartera.TenenciaTokenRepository;
import com.tdetroy.valuacion.repositories.jugador.JugadorRepository;
import com.tdetroy.valuacion.repositories.support.PostgresIntegrationTest;
import com.tdetroy.valuacion.repositories.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuditoriaFinancieraTriggerIT extends PostgresIntegrationTest {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private JugadorRepository jugadorRepository;
    @Autowired private TenenciaTokenRepository tenenciaTokenRepository;
    @Autowired private RegistroAuditoriaRepository registroAuditoriaRepository;
    @Autowired private EntityManager entityManager;

    private List<RegistroAuditoriaEntity> buscarAuditorias(String entidad, UUID entidadId) {
        return entityManager
                .createQuery(
                        "SELECT r FROM RegistroAuditoriaEntity r"
                                + " WHERE r.entidadAfectada = :ent AND r.entidadId = :id"
                                + " ORDER BY r.fecha DESC",
                        RegistroAuditoriaEntity.class)
                .setParameter("ent", entidad)
                .setParameter("id", entidadId)
                .getResultList();
    }

    @Test
    void actualizarSaldoUsuario_disparaTriggerConAntesYDespues() {
        Usuario usuario =
                Usuario.registrar("trigger-" + UUID.randomUUID() + "@example.com", "hash");
        usuarioRepository.saveAndFlush(UsuarioEntity.desde(usuario));

        usuario.acreditarSaldo(new BigDecimal("250.00"));
        usuarioRepository.saveAndFlush(UsuarioEntity.desde(usuario));
        entityManager.clear();

        List<RegistroAuditoriaEntity> registros = buscarAuditorias("Usuario", usuario.getId());

        assertThat(registros)
                .anySatisfy(
                        reg -> {
                            assertThat(reg.getAccion()).isEqualTo("TRIGGER_SALDO_USUARIO");
                            assertThat(reg.getValoresAntes()).contains("0");
                            assertThat(reg.getValoresDespues()).contains("250");
                        });
    }

    @Test
    void insertarYActualizarTenencia_disparaTriggersDeTenencia() {
        Usuario usuario =
                Usuario.registrar("tenencia-" + UUID.randomUUID() + "@example.com", "hash");
        usuarioRepository.saveAndFlush(UsuarioEntity.desde(usuario));

        Jugador jugador =
                Jugador.darAlta(
                        "Jugador Trigger",
                        "Club FC",
                        "Delantero",
                        LocalDate.of(1995, 5, 10),
                        "Argentina",
                        "Primera");
        jugadorRepository.saveAndFlush(JugadorEntity.desde(jugador));

        TenenciaToken tenencia = TenenciaToken.abrir(usuario.getId(), jugador.getId());
        tenencia.acreditar(10, new BigDecimal("100.00"));
        tenenciaTokenRepository.saveAndFlush(TenenciaTokenEntity.desde(tenencia));

        tenencia.reservar(4);
        tenenciaTokenRepository.saveAndFlush(TenenciaTokenEntity.desde(tenencia));
        entityManager.clear();

        List<RegistroAuditoriaEntity> registros =
                buscarAuditorias("TenenciaToken", tenencia.getId());

        assertThat(registros).anyMatch(r -> r.getAccion().equals("TRIGGER_ALTA_TENENCIA"));
        assertThat(registros)
                .anySatisfy(
                        reg -> {
                            assertThat(reg.getAccion()).isEqualTo("TRIGGER_TENENCIA_TOKEN");
                            assertThat(reg.getValoresAntes()).contains("\"cantidadReservada\": 0");
                            assertThat(reg.getValoresDespues())
                                    .contains("\"cantidadReservada\": 4");
                        });
    }

    @Test
    void modificarOEliminarRegistroAuditoria_esBloqueadoPorTriggerInmutable() {
        Usuario usuario =
                Usuario.registrar("inmutable-" + UUID.randomUUID() + "@example.com", "hash");
        usuarioRepository.saveAndFlush(UsuarioEntity.desde(usuario));
        usuario.acreditarSaldo(Monetario.escalar(new BigDecimal("100.00")));
        usuarioRepository.saveAndFlush(UsuarioEntity.desde(usuario));
        entityManager.flush();

        List<RegistroAuditoriaEntity> registros = buscarAuditorias("Usuario", usuario.getId());
        assertThat(registros).isNotEmpty();
        UUID registroId = registros.getFirst().getId();

        assertThatThrownBy(
                        () -> {
                            entityManager
                                    .createNativeQuery(
                                            "DELETE FROM registros_auditoria WHERE id = :id")
                                    .setParameter("id", registroId)
                                    .executeUpdate();
                            entityManager.flush();
                        })
                .isInstanceOf(PersistenceException.class);
    }
}
