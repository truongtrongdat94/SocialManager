package com.socialmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduledPostRequest {
    @NotBlank
    private String socialAccountId;
    
    @NotBlank
    private String content;
    
    private String mediaUrl;

    private List<String> mediaUrls;
    
    @NotNull
    private LocalDateTime scheduledTime;
}