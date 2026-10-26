package org.esercizi.expensetrackerapi.services.mail;

import org.esercizi.expensetrackerapi.exceptions.EmailDeliveryException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class VerificationMailConfigurationTests {
    final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(VerificationMailConfiguration.class);

    @Test
    void disabledDemoDoesNotRequireOrFallBackToSmtp() {
        context.withPropertyValues("app.mail.provider=disabled", "app.registration-enabled=false")
                .run(c -> {
                    assertThat(c).hasNotFailed().hasSingleBean(VerificationEmailSender.class);
                    assertThat(c).doesNotHaveBean(JavaMailSender.class);
                    assertThatThrownBy(() -> c.getBean(VerificationEmailSender.class).send("x", "x", "x", "x"))
                            .isInstanceOf(EmailDeliveryException.class);
                });
    }

    @Test
    void disabledProviderCannotAdvertiseWorkingRegistration() {
        context.withPropertyValues("app.mail.provider=disabled", "app.registration-enabled=true")
                .run(c -> assertThat(c).hasFailed());
    }

    @Test
    void brevoNeedsKeyAndVerifiedSenderConfiguration() {
        context.withPropertyValues("app.mail.provider=brevo", "app.mail.from=sender@example.test")
                .run(c -> assertThat(c).hasFailed());
        context.withPropertyValues("app.mail.provider=brevo", "app.mail.brevo-api-key=synthetic-test-key")
                .run(c -> assertThat(c).hasFailed());
    }

    @Test
    void brevoDoesNotRequireSmtp() {
        context.withPropertyValues("app.mail.provider=brevo", "app.mail.from=sender@example.test",
                        "app.mail.brevo-api-key=synthetic-test-key")
                .run(c -> assertThat(c).hasNotFailed().hasSingleBean(BrevoVerificationEmailSender.class)
                        .doesNotHaveBean(JavaMailSender.class));
    }

    @Test
    void unknownProviderAndInvalidTimeoutFailAtStartup() {
        context.withPropertyValues("app.mail.provider=typo")
                .run(c -> assertThat(c).hasFailed());
        context.withPropertyValues("app.mail.provider=brevo", "app.mail.from=sender@example.test",
                        "app.mail.brevo-api-key=synthetic-test-key", "app.mail.read-timeout-ms=0")
                .run(c -> assertThat(c).hasFailed());
    }

    @Test
    void localSmtpKeepsMailpitSupport() {
        context.withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
                .run(c -> assertThat(c).hasNotFailed().hasSingleBean(SmtpVerificationEmailSender.class));
    }
}
