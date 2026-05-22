package com.socialmanager.dto.response;

import com.socialmanager.model.Platform;
import com.socialmanager.model.PostCreatedBy;
import com.socialmanager.model.PostHistory;
import com.socialmanager.model.PostType;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostHistoryResponse(
    UUID id,
    UUID socialAccountId,
    String socialAccountName,
    Platform platform,
    String externalPostId,
    String content,
    String mediaUrl,
    PostType postType,
    LocalDateTime publishedAt,
    PostCreatedBy createdBy,
    LocalDateTime createdAt
) {
    public static PostHistoryResponse from(PostHistory history) {
        UUID socialAccountId = history.getSocialAccount() != null
            ? history.getSocialAccount().getId()
            : history.getSocialAccountIdSnapshot();
        String socialAccountName = history.getSocialAccount() != null
            ? history.getSocialAccount().getAccountName()
            : history.getSocialAccountNameSnapshot();

        return new PostHistoryResponse(
            history.getId(),
            socialAccountId,
            socialAccountName,
            history.getPlatform(),
            history.getExternalPostId(),
            history.getContent(),
            history.getMediaUrl(),
            history.getPostType(),
            history.getPublishedAt(),
            history.getCreatedBy(),
            history.getCreatedAt()
        );
    }
}
