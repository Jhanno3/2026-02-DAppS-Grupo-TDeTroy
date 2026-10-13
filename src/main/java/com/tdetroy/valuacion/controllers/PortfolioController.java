package com.tdetroy.valuacion.controllers;

import com.tdetroy.valuacion.config.UsuarioPrincipal;
import com.tdetroy.valuacion.dto.response.PortfolioResponse;
import com.tdetroy.valuacion.services.PortfolioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portfolio del usuario autenticado (plan.md §3, UC-11): tenencias + valor + saldo, a la cotización
 * vigente. Siempre el propio usuario — {@code usuarioId} sale del JWT vía {@link UsuarioPrincipal},
 * nunca de un path variable, así que nadie puede consultar el portfolio de otro usuario.
 */
@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/me")
    public PortfolioResponse obtenerMiPortfolio(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return PortfolioResponse.desde(portfolioService.obtenerDe(principal.getUsuarioId()));
    }
}
