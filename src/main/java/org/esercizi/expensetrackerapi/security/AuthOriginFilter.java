package org.esercizi.expensetrackerapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

/**
 * Origin validation protects the cookie-based auth endpoints, including login CSRF.
 * Le origini ammesse sono quelle elencate (il frontend e, se la Swagger UI è attiva, l'URL pubblico
 * del backend stesso, da cui la UI invia le richieste "Try it out"). Il confronto è esatto.
 */
final class AuthOriginFilter extends OncePerRequestFilter {
    private final Set<String> allowedOrigins;
    private final boolean required;

    AuthOriginFilter(Set<String> allowedOrigins, boolean required) {
        this.allowedOrigins = allowedOrigins;
        this.required = required;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (required && "POST".equals(request.getMethod())
                && request.getRequestURI().startsWith(request.getContextPath() + "/auth/")
                && !isAllowed(request.getHeader("Origin"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }

    /** Senza header Origin la richiesta non è ammessa. Set.copyOf(...).contains(null) lancerebbe NullPointerException. */
    private boolean isAllowed(String origin) {
        return origin != null && allowedOrigins.contains(origin);
    }
}
