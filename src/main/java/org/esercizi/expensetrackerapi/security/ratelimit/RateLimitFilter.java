package org.esercizi.expensetrackerapi.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.esercizi.expensetrackerapi.exceptions.RateLimitExceededException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Optional;

/**
 * Limita per IP le POST agli endpoint pubblici di /auth. Il rifiuto passa da GlobalExceptionHandler
 * (stesso formato APIError degli altri errori), perché un filtro non arriva ai @ExceptionHandler da solo.
 * <p>
 * L'IP è request.getRemoteAddr(): dietro un reverse proxy è quello del proxy, finché il server non
 * è configurato per fidarsi di X-Forwarded-For (server.forward-headers-strategy). Leggere l'header
 * a mano sarebbe peggio: chiunque potrebbe falsificarlo per aggirare il limite.
 */
@Slf4j
public final class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimiter limiter;
    private final HandlerExceptionResolver exceptionResolver;

    public RateLimitFilter(RateLimiter limiter, HandlerExceptionResolver exceptionResolver) {
        this.limiter = limiter;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Optional<RateLimitPolicy> policy = "POST".equals(request.getMethod())
                ? RateLimitPolicy.forPath(pathWithoutContext(request))
                : Optional.empty();
        if (policy.isPresent()) {
            RateLimiter.Decision decision = limiter.tryAcquire(policy.get(), request.getRemoteAddr());
            if (!decision.allowed()) {
                log.debug("Rate limit superato: categoria={} ip={}", policy.get(), request.getRemoteAddr());
                exceptionResolver.resolveException(request, response, null,
                        new RateLimitExceededException(decision.retryAfterSeconds()));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static String pathWithoutContext(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context.isEmpty() ? uri : uri.substring(context.length());
    }
}
