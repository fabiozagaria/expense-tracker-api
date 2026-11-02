package org.esercizi.expensetrackerapi;

import com.jayway.jsonpath.JsonPath;
import org.esercizi.expensetrackerapi.model.user.Role;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Controller + service + repository + sicurezza su database reale, rollback a fine test. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpenseCrudIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void expenseCrudValidationOwnershipAndDashboard() throws Exception {
        String owner = login(createVerifiedUser("crudowner1"));
        String other = login(createVerifiedUser("crudowner2"));
        String yesterday = LocalDate.now().minusDays(1).toString();
        String payload = """
                {"title":"Spesa","amount":40.00,"description":"nota","category":"groceries","date":"%s"}
                """.formatted(yesterday);

        mvc.perform(post("/api/expenses").contentType("application/json").content(payload))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/expenses").header("Authorization", bearer(owner))
                        .contentType("application/json")
                        .content("{\"title\":\"ab\",\"amount\":-1,\"category\":\"other\",\"date\":\"" + yesterday + "\"}"))
                .andExpect(status().isBadRequest());

        Integer id = JsonPath.read(mvc.perform(post("/api/expenses").header("Authorization", bearer(owner))
                        .contentType("application/json").content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Spesa"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/expenses/{id}", id).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/expenses/{id}", id).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/api/expenses/{id}", id).header("Authorization", bearer(owner))
                        .contentType("application/json").content("{\"title\":\"Nuovo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nuovo"))
                .andExpect(jsonPath("$.amount").value(40.0));
        mvc.perform(put("/api/expenses/{id}", id).header("Authorization", bearer(owner))
                        .contentType("application/json").content(payload.replace("40.00", "55.50")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(55.5));

        mvc.perform(get("/api/dashboard/summary").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(55.5))
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.balance").value(-55.5));
        mvc.perform(get("/api/dashboard/summary").header("Authorization", bearer(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(0));

        mvc.perform(delete("/api/expenses/{id}", id).header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/expenses/{id}", id).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void loginRejectsWrongPasswordAndUnverifiedAccount() throws Exception {
        createVerifiedUser("loginuser1");
        mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"username\":\"loginuser1\",\"password\":\"wrongpass1\"}"))
                .andExpect(status().isUnauthorized());

        User unverified = users.findByUsername("loginuser1").orElseThrow();
        unverified.setEmailVerified(false);
        users.saveAndFlush(unverified);
        mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"username\":\"loginuser1\",\"password\":\"secret123\"}"))
                .andExpect(status().isForbidden());
    }

    private String createVerifiedUser(String username) {
        users.saveAndFlush(User.builder().username(username).email(username + "@example.test")
                .password(encoder.encode("secret123")).role(Role.USER).emailVerified(true).build());
        return username;
    }

    private String login(String username) throws Exception {
        return JsonPath.read(mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
