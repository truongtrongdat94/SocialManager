package com.socialmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
    @NotBlank(message = "Message không được để trống")
    @Size(max = 63206, message = "Message không được vượt quá 63,206 ký tự")
    String message,
    
    @NotBlank(message = "Page ID không được để trống")
    String pageId
) {}
