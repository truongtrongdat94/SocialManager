package com.socialmanager.dto;

import com.socialmanager.model.Platform;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostHistoryFilter(
        String status,
        Platform platform,
        UUID socialAccountId,
        LocalDateTime from,
        LocalDateTime to,
        String search
) {
}