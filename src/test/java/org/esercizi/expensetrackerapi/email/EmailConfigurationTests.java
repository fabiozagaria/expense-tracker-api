package org.esercizi.expensetrackerapi.email;

import org.esercizi.expensetrackerapi.exceptions.EmailDeliveryException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EmailConfigurationTests {
    final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(EmailConfiguration.class);

    @Test
    void disabledDemoDoesNotRequireOrFallBackToSmtp() {
        context.withPropertyValues("app.email.provider=disabled", "app.registration-enabled=false")
                .run(c -> {
                    assertThat(c).hasNotFailed().hasSingleBean(VerificationEmailSender.class);
                    assertThat(c).doesNotHaveBean(JavaMailSender.class);
                    assertThatThrownBy(() -> c.getBean(VerificationEmailSender.class).send("x", "x", "x", "x"))
                            .isInstanceOf(EmailDeliveryException.class);
                });
    }

    @Test
    void disabledProviderCannotAdvertiseWorkingRegistration() {
        context.withPropertyValues("app.email.provider=disabled", "app.registration-enabled=true")
                .run(c -> assertThat(c).hasFailed());
    }

    @Test
    void brevoNeedsKeyAndVerifiedSenderConfiguration() {
        context.withPropertyValues("app.email.provider=brevo", "app.email.from=sender@example.test")
                .run(c -> assertThat(c).hasFailed());
        context.withPropertyValues("app.email.provider=brevo", "app.email.brevo-api-key=synthetic-test-key")
                .run(c -> assertThat(c).hasFailed());
    }

    @Test
    void brevoDoesNotRequireSmtp() {
        context.withPropertyValues("app.email.provider=brevo", "app.email.from=sender@example.test",
                        "app.email.brevo-api-key=synthetic-test-key")
                .run(c -> assertThat(c).hasNotFailed().hasSingleBean(BrevoVerificationEmailSender.class)
                        .doesNotHaveBean(JavaMailSender.class));
    }

    @Test
    void unknownProviderAndInvalidTimeoutFailAtStartup() {
        context.withPropertyValues("app.email.provider=typo")
                .run(c -> assertThat(c).hasFailed());
        context.withPropertyValues("app.email.provider=brevo", "app.email.from=sender@example.test",
                        "app.email.brevo-api-key=synthetic-test-key", "app.email.read-timeout-ms=0")
                .run(c -> assertThat(c).hasFailed());
    }

    @Test
    void localSmtpKeepsMailpitSupport() {
        context.withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
                .run(c -> assertThat(c).hasNotFailed().hasSingleBean(SmtpVerificationEmailSender.class));
    }
}
