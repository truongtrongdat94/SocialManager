package com.socialmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class FacebookPublishRequest {
    @NotBlank(message = "Caption không được để trống")
    private String caption;

    private List<String> mediaUrls;
}
