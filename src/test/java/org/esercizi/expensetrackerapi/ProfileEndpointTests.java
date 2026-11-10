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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/me: il profilo dell'utente del token, solo username ed email. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileEndpointTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void returnsOnlyUsernameAndEmailOfTheTokenOwner() throws Exception {
        createVerifiedUser("meuser1");
        createVerifiedUser("meuser2");

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + login("meuser1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("meuser1"))
                .andExpect(jsonPath("$.email").value("meuser1@example.test"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.emailVerified").doesNotExist());

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + login("meuser2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("meuser2"))
                .andExpect(jsonPath("$.email").value("meuser2@example.test"));
    }

    @Test
    void rejectsRequestsWithoutAToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    private User createVerifiedUser(String username) {
        return users.saveAndFlush(User.builder().username(username).email(username + "@example.test")
                .password(encoder.encode("secret123")).role(Role.USER).emailVerified(true).build());
    }

    private String login(String username) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
