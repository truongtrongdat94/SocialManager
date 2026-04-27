package com.socialmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPostComposeResponse {

    private ScheduledPostResponse post;

    private String contentSource;

    private List<String> resolvedMediaUrls;

    private String aiGenerationLogId;

    private String imageGenerationId;
}
