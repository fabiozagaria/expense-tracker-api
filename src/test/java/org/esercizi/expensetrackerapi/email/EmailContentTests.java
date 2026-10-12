package org.esercizi.expensetrackerapi.email;

import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.EmailVerificationTokenRepository;
import org.esercizi.expensetrackerapi.services.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmailContentTests {
    @Test
    void htmlEscapesUsernameAndUsesConfiguredFrontendAndExpiry() {
        var sender = mock(VerificationEmailSender.class);
        var service = new EmailService(mock(EmailVerificationTokenRepository.class), sender);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://gestionale-spese.vercel.app/");
        var user = new User();
        user.setUsername("<script>alert(1)</script>");
        user.setEmail("recipient@example.test");
        service.sendVerificationEmail(user, "safe-token");
        var text = ArgumentCaptor.forClass(String.class);
        var html = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq("recipient@example.test"), eq("Conferma il tuo account"), text.capture(), html.capture());
        assertThat(text.getValue()).contains("10 minuti")
                .endsWith("https://gestionale-spese.vercel.app/verify-email?token=safe-token\n");
        assertThat(html.getValue()).doesNotContain("<script>")
                .contains("&lt;script&gt;", "https://gestionale-spese.vercel.app/verify-email?token=safe-token");
    }
}
