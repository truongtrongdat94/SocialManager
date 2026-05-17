package com.socialmanager.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReschedulePostRequest(
        @NotNull LocalDateTime scheduledTime
) {
}