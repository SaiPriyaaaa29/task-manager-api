package com.saipriya.task_manager_api.repository;

import com.saipriya.task_manager_api.model.Task;
import com.saipriya.task_manager_api.model.TaskStatus;
import com.saipriya.task_manager_api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByOwner(User owner, Pageable pageable);
    Page<Task> findByOwnerAndStatus(User owner, TaskStatus status, Pageable pageable);
    Optional<Task> findByIdAndOwner(Long id, User owner);
}