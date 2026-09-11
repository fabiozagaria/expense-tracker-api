package org.esercizi.expensetrackerapi.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity
    ) {
        return httpSecurity
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(
                        csrf ->
                                csrf.disable()
                )
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(Customizer.withDefaults())
                )
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.POST, "/auth/login",
                                                "/auth/register").permitAll()
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
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

}
