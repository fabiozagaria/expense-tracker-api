package org.esercizi.expensetrackerapi;

import org.esercizi.expensetrackerapi.repository.EmailVerificationTokenRepository;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmailRegistrationFailureTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired EmailVerificationTokenRepository tokens;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void rejectedEmailRollsBackUserAndTokenAndAllowsRetryWithoutExposingProviderDetails() throws Exception {
        String username = "emailfail" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String payload = """
                {"username":"%s","email":"%s@example.test","password":"secret123"}
                """.formatted(username, username);
        long tokenCount = tokens.count();
        doThrow(new MailSendException("private-provider-key and private-email-body"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        mvc.perform(post("/auth/register").contentType("application/json").content(payload))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorName").value("EMAIL_DELIVERY_UNAVAILABLE"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-provider"))));
        assertThat(users.existsByUsername(username)).isFalse();
        assertThat(tokens.count()).isEqualTo(tokenCount);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        try {
            mvc.perform(post("/auth/register").contentType("application/json").content(payload))
                    .andExpect(status().isCreated());
            assertThat(users.findByUsername(username).orElseThrow().isEmailVerified()).isFalse();
            verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        } finally {
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                var token = tokens.findByUserUsername(username);
                if (token != null) tokens.delete(token);
                users.findByUsername(username).ifPresent(users::delete);
            });
        }
    }
}
