package com.tdetroy.valuacion.controllers;

import com.tdetroy.valuacion.config.JwtService;
import com.tdetroy.valuacion.config.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.request.LoginRequest;
import com.tdetroy.valuacion.dto.request.RegistrarUsuarioRequest;
import com.tdetroy.valuacion.dto.response.LoginResponse;
import com.tdetroy.valuacion.dto.response.UsuarioResponse;
import com.tdetroy.valuacion.model.Usuario;
import com.tdetroy.valuacion.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro e inicio de sesión (UC-01/UC-02, plan.md §3). No hay un {@code AuthService} en services/
 * (ver la tabla de Services de plan.md §1): orquestar credenciales/JWT es infraestructura de
 * seguridad, no una regla de negocio sobre una entidad — este Controller coordina directamente
 * {@link UsuarioService} (alta de la cuenta) y los beans de seguridad de {@code config/} ({@link
 * PasswordEncoder}, {@link AuthenticationManager}, {@link JwtService}), sin reglas de negocio
 * propias (constitution.md §2).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(
            UsuarioService usuarioService,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * UC-01. La contraseña se hashea acá (BCrypt) antes de llegar a {@code UsuarioService}, que
     * nunca ve texto plano (ver javadoc de {@code UsuarioService.registrar}).
     */
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(
            @Valid @RequestBody RegistrarUsuarioRequest request) {
        Usuario usuario =
                usuarioService.registrar(
                        request.email(), passwordEncoder.encode(request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.desde(usuario));
    }

    /**
     * UC-02. Credenciales inválidas lanzan {@code AuthenticationException}, traducida a 401
     * genérico por {@code GlobalExceptionHandler.manejarCredencialesInvalidas} — nunca hay un
     * {@code try/catch} propio acá que pudiera terminar revelando cuál dato falló.
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.email(), request.password()));
        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        String token =
                jwtService.generarToken(
                        principal.getUsuarioId(), principal.getUsername(), principal.getRol());
        return new LoginResponse(token);
    }
}
