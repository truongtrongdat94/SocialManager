package com.socialmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CaptionRequest {
    
    @NotBlank(message = "Chủ đề (topic) không được để trống")
    private String topic;
    
    @NotBlank(message = "Nền tảng (platform) không được để trống (VD: Facebook, Instagram)")
    private String platform;
}