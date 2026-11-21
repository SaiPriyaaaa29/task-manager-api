package com.saipriya.task_manager_api.service;

import com.saipriya.task_manager_api.exception.TaskNotFoundException;
import com.saipriya.task_manager_api.model.Task;
import com.saipriya.task_manager_api.model.TaskStatus;
import com.saipriya.task_manager_api.model.User;
import com.saipriya.task_manager_api.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Page<Task> getAllTasks(TaskStatus status, Pageable pageable, User owner) {
        if (status == null) {
            return taskRepository.findByOwner(owner, pageable);
        }
        return taskRepository.findByOwnerAndStatus(owner, status, pageable);
    }

    public Task getTaskById(Long id, User owner) {
        return taskRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task createTask(Task request, User owner) {
        Task task = new Task(request.getTitle(), request.getDescription());
        task.setOwner(owner);
        return taskRepository.save(task);
    }

    public Task updateTask(Long id, Task request, User owner) {
        Task task = getTaskById(id, owner);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        return taskRepository.save(task);
    }

    public void deleteTask(Long id, User owner) {
        Task task = getTaskById(id, owner);
        taskRepository.delete(task);
    }
}