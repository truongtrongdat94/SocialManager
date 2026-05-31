package com.socialmanager.dto.request;

import com.socialmanager.validation.FutureTimestamp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
    @NotBlank(message = "Message không được để trống")
    @Size(max = 63206, message = "Message không được vượt quá 63,206 ký tự")
    String message,
    
    @Pattern(regexp = "^https?://.*", message = "Link phải là URL hợp lệ bắt đầu bằng http:// hoặc https://")
    String link,
    
    @FutureTimestamp(min = 600, max = 6480000)
    Long scheduledPublishTime  // Unix timestamp (seconds)
) {}
