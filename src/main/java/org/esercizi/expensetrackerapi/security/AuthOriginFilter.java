package org.esercizi.expensetrackerapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Origin validation protects the cookie-based auth endpoints, including login CSRF. */
final class AuthOriginFilter extends OncePerRequestFilter {
    private final String allowedOrigin;
    private final boolean required;

    AuthOriginFilter(String allowedOrigin, boolean required) {
        this.allowedOrigin = allowedOrigin;
        this.required = required;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (required && "POST".equals(request.getMethod())
                && request.getRequestURI().startsWith(request.getContextPath() + "/auth/")
                && !allowedOrigin.equals(request.getHeader("Origin"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
