package com.socialmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSourceOption {

    private String id;

    private String contentPreview;

    private int mediaCount;

    private LocalDateTime createdAt;
}
