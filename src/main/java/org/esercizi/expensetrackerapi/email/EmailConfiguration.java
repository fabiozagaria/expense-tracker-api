package org.esercizi.expensetrackerapi.email;

import org.esercizi.expensetrackerapi.exceptions.EmailDeliveryException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfiguration {
    @Bean
    VerificationEmailSender verificationEmailSender(EmailProperties properties,
            ObjectProvider<JavaMailSender> smtpSender,
            @Value("${app.registration-enabled:true}") boolean registrationEnabled) {
        return switch (properties.getProvider()) {
            case SMTP -> new SmtpVerificationEmailSender(smtpSender.getObject(), properties.getFrom());
            case BREVO -> {
                if (properties.getBrevoApiKey().isBlank() || properties.getFrom().isBlank()) {
                    throw new IllegalStateException("Brevo requires BREVO_API_KEY and a verified EMAIL_FROM");
                }
                var httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                        .followRedirects(HttpClient.Redirect.NEVER).build();
                var factory = new JdkClientHttpRequestFactory(httpClient);
                factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
                yield new BrevoVerificationEmailSender(RestClient.builder().requestFactory(factory).build(), properties);
            }
            case DISABLED -> {
                if (registrationEnabled) {
                    throw new IllegalStateException("Registration requires an enabled email provider");
                }
                yield (recipient, subject, text, html) -> { throw new EmailDeliveryException(); };
            }
        };
    }
}
