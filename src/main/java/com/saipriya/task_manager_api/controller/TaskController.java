package com.saipriya.task_manager_api.controller;

import jakarta.validation.Valid;
import com.saipriya.task_manager_api.model.Task;
import com.saipriya.task_manager_api.model.TaskStatus;
import com.saipriya.task_manager_api.model.User;
import com.saipriya.task_manager_api.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Page<Task> getAllTasks(@RequestParam(required = false) TaskStatus status,
                                  @PageableDefault(size = 10, sort = "id") Pageable pageable,
                                  @AuthenticationPrincipal User user) {
        return taskService.getAllTasks(status, pageable, user);
    }

    @GetMapping("/{id}")
    public Task getTask(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return taskService.getTaskById(id, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTask(@Valid @RequestBody Task task, @AuthenticationPrincipal User user) {
        return taskService.createTask(task, user);
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id, @Valid @RequestBody Task task,
                           @AuthenticationPrincipal User user) {
        return taskService.updateTask(id, task, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id, @AuthenticationPrincipal User user) {
        taskService.deleteTask(id, user);
    }
}