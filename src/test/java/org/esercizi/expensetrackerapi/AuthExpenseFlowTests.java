package org.esercizi.expensetrackerapi;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.esercizi.expensetrackerapi.model.user.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthExpenseFlowTests {
    @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mailSender;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void incomeCrudSearchAndOwnership() throws Exception {
        registerAndVerify("incomeowner1", "income1@example.test", 1);
        registerAndVerify("incomeowner2", "income2@example.test", 2);
        String first = JsonPath.read(mvc.perform(post("/auth/login").contentType("application/json")
                .content("{\"username\":\"incomeowner1\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.accessToken");
        String second = JsonPath.read(mvc.perform(post("/auth/login").contentType("application/json")
                .content("{\"username\":\"incomeowner2\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.accessToken");
        String payload = "{\"title\":\"Stipendio\",\"amount\":25.50,\"description\":\"Nota\",\"category\":\"salary\",\"date\":\"2026-01-01\"}";
        Integer id = JsonPath.read(mvc.perform(post("/api/incomes").header("Authorization", "Bearer " + first)
                .contentType("application/json").content(payload)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/api/incomes/{id}", id).header("Authorization", "Bearer " + second)).andExpect(status().isNotFound());
        mvc.perform(put("/api/incomes/{id}", id).header("Authorization", "Bearer " + second).contentType("application/json").content(payload)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/incomes/{id}", id).header("Authorization", "Bearer " + second).contentType("application/json").content("{\"title\":\"Alterato\"}")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/incomes/{id}", id).header("Authorization", "Bearer " + second)).andExpect(status().isNotFound());
        for (String query : new String[]{"Stipendio", "Nota", "25,50", "2026-01-01", "01/01/2026"}) {
            mvc.perform(get("/api/incomes/search").param("month", "2026-01").param("query", query)
                    .header("Authorization", "Bearer " + first)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        }
        mvc.perform(get("/api/incomes/search").header("Authorization", "Bearer " + second))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/incomes/search").param("month", "2026-02").header("Authorization", "Bearer " + first))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/expenses/search").param("size", "101").header("Authorization", "Bearer " + first)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/incomes/{id}", id).header("Authorization", "Bearer " + first).contentType("application/json").content(payload.replace("25.50", "30.00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(30));
        mvc.perform(patch("/api/incomes/{id}", id).header("Authorization", "Bearer " + first).contentType("application/json").content("{\"description\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value(""));
        mvc.perform(delete("/api/incomes/{id}", id).header("Authorization", "Bearer " + first)).andExpect(status().isOk());
        mvc.perform(get("/api/incomes/{id}", id).header("Authorization", "Bearer " + first)).andExpect(status().isNotFound());
    }

    @Test
    void corsAllowsCredentialedFrontendAndExpensesRequireBearer() throws Exception {
        mvc.perform(options("/auth/refresh")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(get("/api/expenses")).andExpect(status().isUnauthorized());
    }

    @Test
    void registerVerifyLoginRefreshAndOwnExpenses() throws Exception {
        registerAndVerify("verticaluser1", "vertical1@example.test", 1);

        MvcResult login = mvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"verticaluser1","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String access = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        Cookie refreshCookie = login.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();

        MvcResult created = mvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + access)
                        .contentType("application/json")
                        .content("""
                                {"title":"Test expense","amount":12.50,"category":"other","date":"%s"}
                                """.formatted(LocalDate.now().minusDays(1))))
                .andExpect(status().isCreated()).andReturn();
        Integer expenseId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/expenses").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(expenseId));

        MvcResult refreshed = mvc.perform(post("/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk()).andReturn();
        String renewedAccess = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.accessToken");
        assertThat(renewedAccess).isNotBlank();
        assertThat(refreshed.getResponse().getCookie("refresh_token")).isNotNull();
        mvc.perform(post("/auth/refresh").cookie(refreshCookie)).andExpect(status().isUnauthorized());

        registerAndVerify("verticaluser2", "vertical2@example.test", 2);
        MvcResult secondLogin = mvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"verticaluser2","password":"secret123"}
                                """))
                .andExpect(status().isOk()).andReturn();
        String secondAccess = JsonPath.read(secondLogin.getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(get("/api/expenses").header("Authorization", "Bearer " + secondAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/expenses/{id}", expenseId)
                        .header("Authorization", "Bearer " + secondAccess))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrationPersistsHashedPasswordAndRejectsDuplicateIdentityBeforeSendingMail() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json").content("""
                {"username":"registration1","email":"registration1@example.test","password":"secret123"}
                """))
                .andExpect(status().isCreated());
        var user = users.findByUsername("registration1").orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", user.getPassword())).isTrue();
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.isEmailVerified()).isFalse();

        mvc.perform(post("/auth/register").contentType("application/json").content("""
                {"username":"registration1","email":"different@example.test","password":"secret123"}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorName").value("USERNAME_ALREADY_EXISTS"));
        mvc.perform(post("/auth/register").contentType("application/json").content("""
                {"username":"registration2","email":"registration1@example.test","password":"secret123"}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorName").value("EMAIL_ALREADY_EXISTS"));
        assertThat(users.findByUsername("registration2")).isEmpty();
        verify(mailSender, times(1)).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        mvc.perform(post("/auth/login").contentType("application/json").content("""
                {"username":"registration1","password":"secret123"}
                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationRejectsBlankEmailWithoutSavingUserOrSendingMail() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json").content("""
                {"username":"blankemail1","email":"","password":"secret123"}
                """))
                .andExpect(status().isBadRequest());
        assertThat(users.findByUsername("blankemail1")).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(mailSender);
    }

    private void registerAndVerify(String username, String email, int emailNumber) throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{" + "\"username\":\"" + username + "\",\"email\":\"" + email
                                + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<SimpleMailMessage> messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(emailNumber)).send(messages.capture());
        String body = messages.getAllValues().get(emailNumber - 1).getText();
        assertThat(body).isNotNull();
        String token = body.substring(body.indexOf("?token=") + 7).trim();
        mvc.perform(post("/auth/verify-email").param("token", token))
                .andExpect(status().isNoContent());
        mvc.perform(post("/auth/verify-email").param("token", token))
                .andExpect(status().isNoContent());
    }
}

