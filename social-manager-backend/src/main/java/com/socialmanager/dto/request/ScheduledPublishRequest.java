package com.socialmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduledPublishRequest {
    @NotBlank(message = "Caption không được để trống")
    private String caption;

    private List<String> mediaUrls;

    @NotNull(message = "Thời gian lên lịch không được để trống")
    private LocalDateTime scheduledTime;
}