package org.esercizi.expensetrackerapi.services.mail;

import org.esercizi.expensetrackerapi.exceptions.EmailDeliveryException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VerificationMailProperties.class)
public class VerificationMailConfiguration {
    @Bean
    VerificationEmailSender verificationEmailSender(VerificationMailProperties properties,
            ObjectProvider<JavaMailSender> smtpSender,
            @Value("${app.registration-enabled:true}") boolean registrationEnabled) {
        return switch (properties.getProvider()) {
            case SMTP -> new SmtpVerificationEmailSender(smtpSender.getObject(), properties.getFrom());
            case BREVO -> new BrevoVerificationEmailSender(properties);
            case DISABLED -> {
                if (registrationEnabled) {
                    throw new IllegalStateException("Registration requires an enabled email provider");
                }
                yield (recipient, subject, text, html) -> { throw new EmailDeliveryException(); };
            }
        };
    }
}
