package com.saipriya.task_manager_api.repository;

import com.saipriya.task_manager_api.model.Task;
import com.saipriya.task_manager_api.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);
}