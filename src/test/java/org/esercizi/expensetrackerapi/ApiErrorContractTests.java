package org.esercizi.expensetrackerapi;

import com.jayway.jsonpath.JsonPath;
import org.esercizi.expensetrackerapi.model.user.Role;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.esercizi.expensetrackerapi.services.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ogni errore 4xx previsto esce nel formato APIError, senza dettagli interni. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiErrorContractTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @MockitoBean JavaMailSender mailSender;
    @MockitoBean DashboardService dashboardService;

    @Test
    void beanValidationFailuresUseApiErrorFormat() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json")
                        .content("{\"username\":\"abc\",\"email\":\"not-an-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorName").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details").value(containsString("username:")))
                .andExpect(jsonPath("$.details").value(containsString("email:")))
                .andExpect(jsonPath("$.details").value(containsString("password:")));
    }

    @Test
    void malformedJsonDoesNotLeakParserDetails() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json").content("{\"username\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorName").value("MALFORMED_REQUEST"))
                .andExpect(content().string(not(containsString("Jackson"))))
                .andExpect(content().string(not(containsString("JsonParse"))));
    }

    @Test
    void searchErrorsKeepTheirNameAndMessage() throws Exception {
        String token = login(createVerifiedUser("contract1"));

        mvc.perform(get("/api/expenses/search").param("size", "1000").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorName").value("ILLEGAL_ARGUMENT"))
                .andExpect(jsonPath("$.details").value("Page must be nonnegative; size must be 1–100"));
    }

    @Test
    void wrongParameterTypeUsesApiErrorFormat() throws Exception {
        String token = login(createVerifiedUser("contract2"));

        mvc.perform(get("/api/expenses/not-a-number").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorName").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.details").value("Parametro non valido: id"));
    }

    @Test
    void unexpectedIllegalArgumentIsNotTurnedIntoA400WithItsInternalMessage() throws Exception {
        String token = login(createVerifiedUser("contract3"));
        when(dashboardService.getSummary("contract3"))
                .thenThrow(new IllegalArgumentException("internal-detail-that-must-not-leak"));

        // Non c'è più un handler che la converta in APIError: propaga come errore del server.
        assertThatThrownBy(() -> mvc.perform(get("/api/dashboard/summary").header("Authorization", "Bearer " + token)))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    private String createVerifiedUser(String username) {
        users.saveAndFlush(User.builder().username(username).email(username + "@example.test")
                .password(encoder.encode("secret123")).role(Role.USER).emailVerified(true).build());
        return username;
    }

    private String login(String username) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");
        assertThat(token).isNotBlank();
        return token;
    }
}
