package com.tdetroy.valuacion.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.repositories.JugadorRepository;
import com.tdetroy.valuacion.repositories.PesoMetricaRepository;
import com.tdetroy.valuacion.repositories.UsuarioRepository;
import com.tdetroy.valuacion.services.JugadorService;
import com.tdetroy.valuacion.services.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cubre que {@link DevDataSeeder} siembra exactamente una vez (idempotencia: si el catálogo de
 * jugadores ya tiene filas, no vuelve a insertar nada) — la carga real contra una base real queda
 * fuera de este test unitario (constitution.md §4: nunca depender de un estado de base compartido);
 * eso se verifica manualmente corriendo la app con el profile {@code dev}. Los pesos de las
 * métricas de WhoScored (plan.md §6.2/§8.1) se completan con un chequeo de idempotencia propio,
 * independiente del gate de "catálogo vacío" de usuarios/jugadores.
 */
@ExtendWith(MockitoExtension.class)
class DevDataSeederTest {

    @Mock private JugadorRepository jugadorRepository;
    @Mock private JugadorService jugadorService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private PesoMetricaRepository pesoMetricaRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private DevDataSeeder seeder() {
        return new DevDataSeeder(
                jugadorRepository,
                jugadorService,
                usuarioRepository,
                usuarioService,
                pesoMetricaRepository,
                passwordEncoder);
    }

    @Test
    void catalogoVacio_siembraUsuariosYJugadores() throws Exception {
        when(jugadorRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("hash");

        seeder().run();

        verify(usuarioService).registrar(any(), any());
        verify(usuarioRepository).save(any());
        verify(jugadorService, times(3)).darAlta(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void catalogoConDatos_noVuelveASembrar() throws Exception {
        when(jugadorRepository.count()).thenReturn(1L);

        seeder().run();

        verify(usuarioService, never()).registrar(any(), any());
        verify(usuarioRepository, never()).save(any());
        verify(jugadorService, never()).darAlta(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void pesosMetricaSinSembrar_siembraLosSietePesosDeWhoScoredLiga() throws Exception {
        when(jugadorRepository.count()).thenReturn(1L);
        when(pesoMetricaRepository.existsById(any())).thenReturn(false);

        seeder().run();

        verify(pesoMetricaRepository, times(7)).save(any());
    }

    @Test
    void pesosMetricaYaSembrados_noLosVuelveAGuardar() throws Exception {
        when(jugadorRepository.count()).thenReturn(1L);
        when(pesoMetricaRepository.existsById(any())).thenReturn(true);

        seeder().run();

        verify(pesoMetricaRepository, never()).save(any());
    }
}
