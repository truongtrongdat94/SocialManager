package com.socialmanager.repository;

import com.socialmanager.model.AiGenerationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {

    Optional<AiGenerationLog> findByIdAndUser_Id(UUID id, UUID userId);

    List<AiGenerationLog> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<AiGenerationLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
