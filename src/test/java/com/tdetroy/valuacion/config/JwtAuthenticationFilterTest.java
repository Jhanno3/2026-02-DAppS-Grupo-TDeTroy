package com.tdetroy.valuacion.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tdetroy.valuacion.model.RolUsuario;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Cubre {@link JwtAuthenticationFilter}: un token válido deja el {@link UsuarioPrincipal} en el
 * {@code SecurityContext}; la ausencia de header, un header sin el prefijo {@code Bearer}, o un
 * token que {@link JwtService} considera inválido, dejan la request sin autenticar — nunca lanzan,
 * la cadena de filtros sigue igual en los cuatro casos (es {@code SecurityConfig} quien decide si
 * el endpoint exige autenticación).
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void conTokenValido_dejaElPrincipalEnElSecurityContext() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        when(jwtService.validarYExtraerClaims("token-valido"))
                .thenReturn(
                        Optional.of(
                                new JwtService.ClaimsToken(
                                        usuarioId, "persona@example.com", RolUsuario.ADMIN)));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(UsuarioPrincipal.class);
        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        assertThat(principal.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(principal.getUsername()).isEqualTo("persona@example.com");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void sinHeaderAuthorization_noAutenticaYContinuaLaCadena() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void conHeaderSinPrefijoBearer_noAutentica() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "token-suelto-sin-prefijo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void conTokenInvalidoSegunJwtService_noAutenticaPeroContinuaLaCadena() throws Exception {
        when(jwtService.validarYExtraerClaims("token-invalido")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-invalido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void siempreDelegaEnLaCadenaDeFiltros() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var chainSpy = new MockFilterChain();

        filter.doFilter(request, response, chainSpy);

        assertThat(chainSpy.getRequest()).isEqualTo(request);
        verify(jwtService, never()).validarYExtraerClaims(any());
    }
}
