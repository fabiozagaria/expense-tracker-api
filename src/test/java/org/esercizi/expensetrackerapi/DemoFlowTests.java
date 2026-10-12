package org.esercizi.expensetrackerapi;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.demo-password=test-only-demo-password-1234",
        "spring.datasource.hikari.data-source-properties.sslMode=DISABLED"
})
@ActiveProfiles("demo")
@AutoConfigureMockMvc
@Transactional
class DemoFlowTests {
    @Autowired MockMvc mvc;
    private static final String ORIGIN = "http://localhost:4200";

    @Test
    void demoLoginRefreshLogoutAndDataWorkWithoutEmail() throws Exception {
        var login = mvc.perform(post("/auth/demo").header("Origin", ORIGIN))
                .andExpect(status().isOk()).andReturn();
        String access = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        Cookie cookie = login.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(login.getResponse().getHeader("Set-Cookie")).contains("SameSite=Strict", "Path=/auth");
        mvc.perform(get("/api/dashboard/summary").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
        mvc.perform(get("/api/expenses").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").exists());
        mvc.perform(get("/api/incomes").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").exists());
        // Removing Data REST must make repository endpoints unavailable even with a token.
        mvc.perform(get("/users").header("Authorization", "Bearer " + access))
                .andExpect(status().isNotFound());
        var refreshed = mvc.perform(post("/auth/refresh").header("Origin", ORIGIN).cookie(cookie))
                .andExpect(status().isOk()).andReturn();
        mvc.perform(post("/auth/refresh").header("Origin", ORIGIN).cookie(cookie))
                .andExpect(status().isUnauthorized());
        Cookie renewed = refreshed.getResponse().getCookie("refresh_token");
        mvc.perform(post("/auth/logout").header("Origin", ORIGIN).cookie(renewed))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
        mvc.perform(post("/auth/refresh").header("Origin", ORIGIN).cookie(renewed))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void demoRejectsRegistrationAndUntrustedAuthRequests() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
        mvc.perform(post("/auth/demo")).andExpect(status().isForbidden());
        mvc.perform(post("/auth/demo").header("Origin", "https://other.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/auth/register").header("Origin", ORIGIN).contentType("application/json")
                        .content("{\"username\":\"visitor123\",\"email\":\"visitor@example.test\",\"password\":\"secret123\"}"))
                .andExpect(status().isServiceUnavailable());
    }
}
