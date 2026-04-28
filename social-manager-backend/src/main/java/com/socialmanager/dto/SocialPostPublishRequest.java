package com.socialmanager.dto;

public record SocialPostPublishRequest(
        String platform,
        String accountId,
        String token,
        String content,
        String mediaUrl,
        String idempotencyKey
) {
}
