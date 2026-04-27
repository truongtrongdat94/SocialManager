package com.socialmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiPostComposeRequest {

    @NotBlank
    private String socialAccountId;

    @NotNull
    private LocalDateTime scheduledTime;

    private String aiGenerationLogId;

    private String imageGenerationId;

    private String contentOverride;

    private String mediaUrl;

    private List<String> mediaUrls;
}
