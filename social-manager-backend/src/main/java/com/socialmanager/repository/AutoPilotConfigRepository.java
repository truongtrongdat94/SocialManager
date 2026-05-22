package com.socialmanager.repository;

import com.socialmanager.model.AutoPilotConfig;
import com.socialmanager.model.AutoPilotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AutoPilotConfigRepository extends JpaRepository<AutoPilotConfig, UUID> {

    // lấy config cần chạy
    List<AutoPilotConfig> findByStatusAndNextRunAtBefore(
            AutoPilotStatus status,
            LocalDateTime time
    );

    // lấy config theo user ID
    List<AutoPilotConfig> findByUserId(UUID userId);

    void deleteBySocialAccount_Id(UUID socialAccountId);
}