package com.socialmanager.dto;

import com.socialmanager.model.ScheduledPost;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record ScheduledPostHistoryDto(
    UUID id,
    String caption,
    List<String> mediaUrls,
    LocalDateTime scheduledTime,
    String status,
    String publishedPostId,
    String publishedPostUrl,
    String errorMessage,
    LocalDateTime createdAt,
    String accountName,
    String accountPlatform
) {
    public static ScheduledPostHistoryDto fromEntity(ScheduledPost scheduledPost) {
        String publishedUrl = null;
        try {
            if (scheduledPost.getSocialAccount() != null && scheduledPost.getSocialAccount().getPlatform() != null && scheduledPost.getPublishedPostId() != null) {
                switch (scheduledPost.getSocialAccount().getPlatform()) {
                    case FACEBOOK -> {
                        String id = scheduledPost.getPublishedPostId();
                        if (id.contains("_")) {
                            String[] parts = id.split("_");
                            publishedUrl = "https://www.facebook.com/" + parts[0] + "/posts/" + parts[1];
                        } else {
                            publishedUrl = "https://www.facebook.com/" + id;
                        }
                    }
                    default -> publishedUrl = null;
                }
            }
        } catch (Exception ignored) {}

        return ScheduledPostHistoryDto.builder()
            .id(scheduledPost.getId())
            .caption(scheduledPost.getCaption())
            .mediaUrls(scheduledPost.getMediaUrls() != null ? List.of(scheduledPost.getMediaUrls()) : List.of())
            .scheduledTime(scheduledPost.getScheduledTime())
            .status(scheduledPost.getStatus())
            .publishedPostId(scheduledPost.getPublishedPostId())
            .publishedPostUrl(publishedUrl)
            .errorMessage(scheduledPost.getErrorMessage())
            .createdAt(scheduledPost.getCreatedAt())
            .accountName(scheduledPost.getSocialAccount() != null ? scheduledPost.getSocialAccount().getAccountName() : null)
            .accountPlatform(scheduledPost.getSocialAccount() != null && scheduledPost.getSocialAccount().getPlatform() != null
                ? scheduledPost.getSocialAccount().getPlatform().name()
                : null)
            .build();
    }
}