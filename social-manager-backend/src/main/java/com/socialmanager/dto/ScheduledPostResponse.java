package com.socialmanager.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ScheduledPostResponse {
    private String id;
    private String content;
    private String mediaUrl;
    private LocalDateTime scheduledTime;
    private String status;
    private String socialAccountName;
    private String publishedPostId;
    private String publishedPostUrl;
}