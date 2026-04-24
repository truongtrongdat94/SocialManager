package com.socialmanager.dto;

import com.socialmanager.model.Platform;
import com.socialmanager.model.ScheduledPost;

import java.time.LocalDateTime;

public record PostHistoryItem(
        String id,
        String socialAccountId,
        String socialAccountName,
        Platform platform,
        String content,
        String mediaUrl,
        LocalDateTime scheduledTime,
        String status,
        String publishedPostId,
        String errorMessage,
        Integer retryCount,
        LocalDateTime lastAttemptAt,
        Boolean autoPilot,
        LocalDateTime createdAt
) {
    public static PostHistoryItem from(ScheduledPost post) {
        return new PostHistoryItem(
                post.getId() == null ? null : post.getId().toString(),
                post.getSocialAccount() == null || post.getSocialAccount().getId() == null ? null : post.getSocialAccount().getId().toString(),
                post.getSocialAccount() == null ? null : post.getSocialAccount().getAccountName(),
                post.getSocialAccount() == null ? null : post.getSocialAccount().getPlatform(),
                post.getCaption(),
                firstMediaUrl(post.getMediaUrls()),
                post.getScheduledTime(),
                post.getStatus(),
                post.getPublishedPostId(),
                post.getErrorMessage(),
                post.getRetryCount(),
                post.getLastAttemptAt(),
                post.getIsAutoPilot(),
                post.getCreatedAt()
        );
    }

    private static String firstMediaUrl(String[] mediaUrls) {
        if (mediaUrls == null || mediaUrls.length == 0) {
            return null;
        }

        return mediaUrls[0];
    }
}