package com.saipriya.task_manager_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// test-only secret (valid Base64, 37 bytes); never use this in production
@SpringBootTest(properties = "jwt.secret=dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLTEyMzQ1Ng==")
@AutoConfigureMockMvc
class AuthAndIsolationIntegrationTest {

    @Autowired MockMvc mockMvc;

    private static final String PASSWORD = "password123";

    private String uniqueEmail() {
        return UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return body.split("\"token\":\"")[1].split("\"")[0];
    }

    private long createTask(String token, String title) throws Exception {
        String body = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"description\":\"D\",\"status\":\"TODO\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(body);
        assertTrue(m.find(), "response should contain task id: " + body);
        return Long.parseLong(m.group(1));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        register(email);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        register(email);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tasks_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void tasks_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void user_canCreateAndReadOwnTask() throws Exception {
        String email = uniqueEmail();
        register(email);
        String token = login(email);
        long id = createTask(token, "My task");

        mockMvc.perform(get("/api/tasks/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My task"));
    }

    @Test
    void user_cannotReadUpdateOrDeleteAnotherUsersTask() throws Exception {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        register(emailA);
        register(emailB);
        String tokenA = login(emailA);
        String tokenB = login(emailB);
        long taskId = createTask(tokenA, "A's private task");

        mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked\",\"description\":\"D\",\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/tasks/" + taskId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // A's task is untouched
        mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("A's private task"));
    }

    @Test
    void listTasks_returnsOnlyCallersTasks() throws Exception {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        register(emailA);
        register(emailB);
        String tokenA = login(emailA);
        String tokenB = login(emailB);
        createTask(tokenA, "A1");
        createTask(tokenA, "A2");
        createTask(tokenB, "B1");

        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}