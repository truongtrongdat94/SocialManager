package com.socialmanager.dto.request;

import com.socialmanager.model.AutoPilotStatus;
import jakarta.validation.constraints.Min;

public record UpdateAutoPilotRequest(
    String[] keywords,
    
    @Min(value = 1, message = "Frequency must be at least 1 hour")
    Integer frequencyHours,
    
    AutoPilotStatus status,
    
    String promptTemplate
) {}
