package org.esercizi.expensetrackerapi.services.mail;

import org.esercizi.expensetrackerapi.exceptions.EmailDeliveryException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SmtpVerificationEmailSenderTests {
    @Test
    void smtpFailureUsesPublicEmailErrorWithoutProviderDetails() {
        var mail = mock(JavaMailSender.class);
        doThrow(new MailSendException("private-provider-response"))
                .when(mail).send(any(SimpleMailMessage.class));
        var sender = new SmtpVerificationEmailSender(mail, "sender@example.test");
        assertThatThrownBy(() -> sender.send("recipient@example.test", "Subject", "Text", "<p>Text</p>"))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessage("Invio email non riuscito. Riprova più tardi.")
                .hasNoCause();
        verify(mail).send(any(SimpleMailMessage.class));
    }
}
