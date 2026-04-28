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
public class AiPostSourcesResponse {

    private List<AiSourceOption> aiGenerationLogs;

    private List<AiSourceOption> imageGenerations;
}
