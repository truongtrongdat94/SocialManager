package com.socialmanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduledPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_account_id", nullable = false)
    private SocialAccount socialAccount;

    @Column(columnDefinition = "TEXT")
    private String caption;

    // TEXT[] in PostgreSQL
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 10)
    @Column(name = "media_urls")
    private String[] mediaUrls;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    // PENDING, POSTED, FAILED
    @Builder.Default
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "published_post_id")
    private String publishedPostId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Builder.Default
    @Column(name = "is_auto_pilot")
    private Boolean isAutoPilot = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
