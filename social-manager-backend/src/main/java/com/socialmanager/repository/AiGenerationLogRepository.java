package com.socialmanager.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialmanager.model.AiGenerationLog;

@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {
    List<AiGenerationLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}