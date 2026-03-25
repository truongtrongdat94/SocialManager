package com.socialmanager.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "social_accounts",
    indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "expires_at"),
        @Index(columnList = "user_id, platform")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(name = "external_account_id")
    private String externalAccountId;

    @Column(name = "account_alias")
    private String accountAlias;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "profile_picture_url", columnDefinition = "TEXT")
    private String profilePictureUrl;

    // AES-encrypted
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(columnDefinition = "TEXT")
    private String scopes;

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
