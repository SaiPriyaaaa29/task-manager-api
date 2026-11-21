package com.saipriya.task_manager_api.controller;

import com.saipriya.task_manager_api.exception.TaskNotFoundException;
import com.saipriya.task_manager_api.model.Task;
import com.saipriya.task_manager_api.model.User;
import com.saipriya.task_manager_api.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean TaskService taskService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("a@test.com");
        testUser.setPassword("hashed");
    }

    @Test
    void getTask_withoutLogin_returns401() throws Exception {
        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTask_loggedIn_returns200() throws Exception {
        when(taskService.getTaskById(eq(1L), any(User.class))).thenReturn(new Task("Title", "Desc"));

        mockMvc.perform(get("/api/tasks/1").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void getTask_notFound_returns404() throws Exception {
        when(taskService.getTaskById(eq(99L), any(User.class))).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/api/tasks/99").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTask_valid_returns201() throws Exception {
        when(taskService.createTask(any(Task.class), any(User.class))).thenReturn(new Task("Title", "Desc"));

        mockMvc.perform(post("/api/tasks")
                        .with(user(testUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Title\",\"description\":\"Desc\",\"status\":\"TODO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void createTask_blankTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(user(testUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"Desc\",\"status\":\"TODO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTask_missingStatus_returns400() throws Exception {
        mockMvc.perform(put("/api/tasks/1")
                        .with(user(testUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Title\",\"description\":\"Desc\",\"status\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteTask_loggedIn_returns204() throws Exception {
        mockMvc.perform(delete("/api/tasks/1").with(user(testUser)).with(csrf()))
                .andExpect(status().isNoContent());
    }
}