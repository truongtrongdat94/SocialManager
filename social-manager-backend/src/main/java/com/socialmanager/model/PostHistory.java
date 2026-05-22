package com.socialmanager.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_history",
    indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "social_account_id"),
        @Index(columnList = "platform"),
        @Index(columnList = "published_at"),
        @Index(columnList = "created_by")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_account_id", nullable = true)
    private SocialAccount socialAccount;

    @Column(name = "social_account_id_snapshot", nullable = false)
    private UUID socialAccountIdSnapshot;

    @Column(name = "social_account_name_snapshot", nullable = false)
    private String socialAccountNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(name = "external_post_id")
    private String externalPostId; // Facebook post ID, Instagram post ID, etc.

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false)
    private PostType postType;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by", nullable = false)
    private PostCreatedBy createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
    }
}
