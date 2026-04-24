package com.socialmanager.dto;

import com.socialmanager.model.Platform;
import com.socialmanager.model.SocialAccount;

import java.time.LocalDateTime;

public record SocialAccountResponse(
        String id,
        Platform platform,
        String externalAccountId,
        String accountAlias,
        String accountName,
        String profilePictureUrl,
        boolean autoPilot,
        LocalDateTime expiresAt,
        String scopes,
        LocalDateTime createdAt,
        boolean hasAccessToken,
        boolean hasRefreshToken
) {

    public static SocialAccountResponse from(SocialAccount account) {
        return new SocialAccountResponse(
                account.getId() == null ? null : account.getId().toString(),
                account.getPlatform(),
                account.getExternalAccountId(),
                account.getAccountAlias(),
                account.getAccountName(),
                account.getProfilePictureUrl(),
                Boolean.TRUE.equals(account.getIsAutoPilot()),
                account.getExpiresAt(),
                account.getScopes(),
                account.getCreatedAt(),
                account.getAccessToken() != null && !account.getAccessToken().isBlank(),
                account.getRefreshToken() != null && !account.getRefreshToken().isBlank()
        );
    }
}