package com.example.taskmanagement.repository;

import com.example.taskmanagement.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndTenantId(Long id, Long tenantId);

    List<Task> findAllByTenantId(Long tenantId);

    List<Task> findByAssignedToIdAndTenantId(Long userId, Long tenantId);
}