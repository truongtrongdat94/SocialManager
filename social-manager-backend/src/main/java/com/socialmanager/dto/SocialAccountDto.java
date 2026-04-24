package com.socialmanager.dto;

import com.socialmanager.model.Platform;
import java.util.UUID;

public record SocialAccountDto(
        UUID id,
        Platform platform,
        String accountName,
        String accountAlias,
        String profilePictureUrl,
        Boolean isAutoPilot
) {}