package com.socialmanager.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduledPublishRequest {
    private String caption;
    private List<String> mediaUrls;
    // ISO-8601 local datetime, e.g. 2026-05-20T18:30:00
    private LocalDateTime scheduledTime;
}
