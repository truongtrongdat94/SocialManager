package com.socialmanager.dto.request;

import com.socialmanager.model.AutoPilotStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAutoPilotRequest(
    @NotNull(message = "Social account ID is required")
    UUID socialAccountId,
    
    @NotEmpty(message = "At least one keyword is required")
    String[] keywords,
    
    @NotNull(message = "Frequency hours is required")
    @Min(value = 1, message = "Frequency must be at least 1 hour")
    Integer frequencyHours,
    
    AutoPilotStatus status,
    
    String promptTemplate
) {
    public CreateAutoPilotRequest {
        // Default status to ACTIVE if not provided
        if (status == null) {
            status = AutoPilotStatus.ACTIVE;
        }
    }
}
