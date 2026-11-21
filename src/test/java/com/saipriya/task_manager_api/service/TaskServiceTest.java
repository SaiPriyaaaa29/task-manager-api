package com.saipriya.task_manager_api.service;

import com.saipriya.task_manager_api.exception.TaskNotFoundException;
import com.saipriya.task_manager_api.model.Task;
import com.saipriya.task_manager_api.model.TaskStatus;
import com.saipriya.task_manager_api.model.User;
import com.saipriya.task_manager_api.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @InjectMocks TaskService taskService;

    private User owner;
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setEmail("a@test.com");
    }

    @Test
    void getAllTasks_noStatus_returnsOwnersTasks() {
        Page<Task> page = new PageImpl<>(List.of(new Task("T", "D")));
        when(taskRepository.findByOwner(owner, pageable)).thenReturn(page);

        Page<Task> result = taskService.getAllTasks(null, pageable, owner);

        assertEquals(1, result.getTotalElements());
        verify(taskRepository, never()).findByOwnerAndStatus(any(), any(), any());
    }

    @Test
    void getAllTasks_withStatus_filtersByStatus() {
        Page<Task> page = new PageImpl<>(List.of(new Task("T", "D")));
        when(taskRepository.findByOwnerAndStatus(owner, TaskStatus.DONE, pageable)).thenReturn(page);

        Page<Task> result = taskService.getAllTasks(TaskStatus.DONE, pageable, owner);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getTaskById_found_returnsTask() {
        Task task = new Task("T", "D");
        when(taskRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(task));

        assertSame(task, taskService.getTaskById(1L, owner));
    }

    @Test
    void getTaskById_missingOrNotOwned_throwsNotFound() {
        when(taskRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(1L, owner));
    }

    @Test
    void createTask_setsOwnerAndDefaultStatus() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        Task request = new Task("Title", "Desc");
        request.setStatus(TaskStatus.DONE); // client-sent status must be ignored

        Task saved = taskService.createTask(request, owner);

        assertSame(owner, saved.getOwner());
        assertEquals(TaskStatus.TODO, saved.getStatus());
        assertEquals("Title", saved.getTitle());
    }

    @Test
    void updateTask_ownedTask_updatesFields() {
        Task existing = new Task("Old", "Old desc");
        when(taskRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        Task request = new Task("New", "New desc");
        request.setStatus(TaskStatus.IN_PROGRESS);

        Task updated = taskService.updateTask(1L, request, owner);

        assertEquals("New", updated.getTitle());
        assertEquals("New desc", updated.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, updated.getStatus());
    }

    @Test
    void updateTask_notOwned_throwsAndDoesNotSave() {
        when(taskRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class,
                () -> taskService.updateTask(1L, new Task("T", "D"), owner));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void deleteTask_ownedTask_deletes() {
        Task task = new Task("T", "D");
        when(taskRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.of(task));

        taskService.deleteTask(1L, owner);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_notOwned_throwsAndDoesNotDelete() {
        when(taskRepository.findByIdAndOwner(1L, owner)).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(1L, owner));
        verify(taskRepository, never()).delete(any());
    }
}