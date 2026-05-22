package com.socialmanager.dto.response;

import com.socialmanager.model.AutoPilotConfig;
import com.socialmanager.model.AutoPilotStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AutoPilotConfigResponse(
    UUID id,
    UUID socialAccountId,
    String socialAccountName,
    String platform,
    String[] keywords,
    Integer frequencyHours,
    AutoPilotStatus status,
    LocalDateTime lastRunAt,
    LocalDateTime nextRunAt,
    String promptTemplate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AutoPilotConfigResponse from(AutoPilotConfig config) {
        return new AutoPilotConfigResponse(
            config.getId(),
            config.getSocialAccount().getId(),
            config.getSocialAccount().getAccountName(),
            config.getSocialAccount().getPlatform().name(),
            config.getKeywords(),
            config.getFrequencyHours(),
            config.getStatus(),
            config.getLastRunAt(),
            config.getNextRunAt(),
            config.getPromptTemplate(),
            config.getCreatedAt(),
            config.getUpdatedAt()
        );
    }
}
