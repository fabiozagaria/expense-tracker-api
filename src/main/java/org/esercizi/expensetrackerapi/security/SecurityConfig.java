package org.esercizi.expensetrackerapi.security;

import com.nimbusds.jose.jwk.JWK;
import jakarta.servlet.DispatcherType;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.esercizi.expensetrackerapi.dto.CappedList;
import org.esercizi.expensetrackerapi.security.ratelimit.RateLimitFilter;
import org.esercizi.expensetrackerapi.security.ratelimit.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final int MIN_JWT_SECRET_BYTES = 32;

    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${app.frontend-url}")
    private String frontendUrl;
    @Value("${app.require-auth-origin:false}")
    private boolean requireAuthOrigin;
    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;
    @Value("${app.api-public-url:}")
    private String apiPublicUrl;
    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerUiEnabled;

    /**
     * Origini da cui sono ammesse le POST /auth/*: il frontend e, se la Swagger UI è attiva e l'URL
     * pubblico del backend è configurato, il backend stesso (da lì parte il "Try it out" della UI).
     */
    private Set<String> allowedAuthOrigins() {
        Set<String> origins = new HashSet<>();
        origins.add(frontendUrl);
        if (swaggerUiEnabled && !apiPublicUrl.isBlank()) {
            origins.add(apiPublicUrl.replaceAll("/+$", ""));
        }
        return Set.copyOf(origins);
    }

    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            RateLimiter rateLimiter,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) {
        httpSecurity.addFilterBefore(new AuthOriginFilter(allowedAuthOrigins(), requireAuthOrigin),
                UsernamePasswordAuthenticationFilter.class);
        if (rateLimitEnabled) {
            httpSecurity.addFilterBefore(new RateLimitFilter(rateLimiter, exceptionResolver),
                    UsernamePasswordAuthenticationFilter.class);
        }
        return httpSecurity
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(
                        AbstractHttpConfigurer::disable
                )
                .cors(Customizer.withDefaults())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(Customizer.withDefaults())
                )
                .authorizeHttpRequests(
                        auth ->
                                // Gli errori inviati con sendError (es. il 403 di AuthOriginFilter) passano da un dispatch
                                // interno verso /error: se richiedesse autenticazione, lo stato diventerebbe 401 e
                                // nasconderebbe la causa. Vale solo per il dispatch ERROR, non per richieste dirette a /error.
                                auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                                        .requestMatchers(HttpMethod.POST,
                                                "/auth/login",
                                "/auth/demo",
                                                "/auth/register",
                                                "/auth/refresh",
                                                "/auth/logout",
                                                "/auth/verify-email",
                                                "/auth/resend-verification",
                                                "/auth/forgot-password",
                                                "/auth/reset-password"
                                        ).permitAll()
                                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                                        // Documentazione pubblica del contratto REST (si spegne con SWAGGER_ENABLED=false).
                                        .requestMatchers(HttpMethod.GET,
                                                "/v3/api-docs", "/v3/api-docs/**",
                                                "/swagger-ui.html", "/swagger-ui/**"
                                        ).permitAll()
                                .anyRequest().authenticated()
                )
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Anche l'origine del backend (se la Swagger UI è attiva): senza, il CorsFilter rifiuterebbe con 403 le
        // richieste della UI quando host o schema ricostruiti dietro il proxy non coincidono con l'Origin.
        configuration.setAllowedOrigins(List.copyOf(allowedAuthOrigins()));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Senza questo il browser non lascia leggere l'header da JavaScript (CORS).
        configuration.setExposedHeaders(List.of(CappedList.TRUNCATED_HEADER));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtDecoder nimbusJwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(getSecretKey()).build();
    }

    @Bean
    public JwtEncoder nimbusJwtEncoder() {
        JWK jwk = new OctetSequenceKey.Builder(getSecretKey().getEncoded()).build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    private SecretKey getSecretKey() {
        return new SecretKeySpec(
                requireStrongSecret(jwtSecret),
                "HmacSHA256"
        );
    }

    static byte[] requireStrongSecret(String secret) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException(
                    "SECRET_KEY deve contenere almeno " + MIN_JWT_SECRET_BYTES
                            + " byte (HS256, RFC 7518 sez. 3.2); lunghezza attuale: " + bytes.length);
        }
        return bytes;
    }

}

