package org.esercizi.expensetrackerapi;

import com.jayway.jsonpath.JsonPath;
import org.esercizi.expensetrackerapi.dto.CappedList;
import org.esercizi.expensetrackerapi.model.user.Role;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/expenses: tetto sulle righe, forma della risposta invariata, header solo se tronca. */
@SpringBootTest(properties = "app.cache.expenses-enabled=false")
@AutoConfigureMockMvc
@Transactional
class ExpenseListLimitTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void underTheCapTheResponseIsTheWholeArrayWithoutTheTruncationHeader() throws Exception {
        User owner = createVerifiedUser("listsmall1");
        insertExpenses(owner, 3);

        mvc.perform(get("/api/expenses").header("Authorization", "Bearer " + login("listsmall1")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(CappedList.TRUNCATED_HEADER))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void overTheCapOnlyTheNewestExpensesAreReturnedAndTheHeaderSaysSo() throws Exception {
        User owner = createVerifiedUser("listbig1");
        insertExpenses(owner, CappedList.MAX_SIZE + 1);

        String body = mvc.perform(get("/api/expenses").header("Authorization", "Bearer " + login("listbig1")))
                .andExpect(status().isOk())
                .andExpect(header().string(CappedList.TRUNCATED_HEADER, "true"))
                .andExpect(jsonPath("$.length()").value(CappedList.MAX_SIZE))
                .andReturn().getResponse().getContentAsString();

        // Le spese hanno date via via più vecchie con l'indice: la più vecchia (indice 0) è quella esclusa.
        List<String> titles = JsonPath.read(body, "$[*].title");
        assertThat(titles).doesNotContain("spesa-0").contains("spesa-1", "spesa-" + CappedList.MAX_SIZE);
    }

    private User createVerifiedUser(String username) {
        return users.saveAndFlush(User.builder().username(username).email(username + "@example.test")
                .password(encoder.encode("secret123")).role(Role.USER).emailVerified(true).build());
    }

    /** spesa-0 è la più vecchia, spesa-(n-1) la più recente. Titolo di max 15 caratteri. */
    private void insertExpenses(User owner, int count) {
        LocalDate newest = LocalDate.now().minusDays(1);
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(new Object[]{"1.00", "other", Date.valueOf(newest.minusDays(count - 1 - i)), "spesa-" + i, owner.getId()});
        }
        jdbc.batchUpdate("insert into expenses (amount, category, date, title, user_id) values (?, ?, ?, ?, ?)", rows);
    }

    private String login(String username) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
