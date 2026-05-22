package com.socialmanager.dto.request;

import com.socialmanager.validation.FutureTimestamp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePhotoRequest(
    @NotBlank(message = "Photo URL không được để trống")
    @Pattern(regexp = "^https?://.*", message = "Photo URL phải là URL hợp lệ bắt đầu bằng http:// hoặc https://")
    String photoUrl,
    
    @Size(max = 63206, message = "Caption không được vượt quá 63,206 ký tự")
    String caption,
    
    @FutureTimestamp(min = 600, max = 6480000)
    Long scheduledPublishTime  // Unix timestamp (seconds)
) {}
