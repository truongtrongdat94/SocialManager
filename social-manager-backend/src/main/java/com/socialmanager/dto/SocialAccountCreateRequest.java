package com.socialmanager.dto;

import com.socialmanager.model.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SocialAccountCreateRequest {
    @NotNull
    private Platform platform;

    @NotBlank
    private String accessToken;

    private String externalAccountId;
    private String accountAlias;
    private String accountName;
    private String profilePictureUrl;
    private String refreshToken;
    private LocalDateTime expiresAt;
    private String scopes;
    private Boolean autoPilot;
}