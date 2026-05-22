package com.socialmanager.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cloudinary_credentials", indexes = { @Index(columnList = "user_id") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CloudinaryCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "cloud_name")
    private String cloudName;

    @Column(name = "api_key", columnDefinition = "TEXT")
    private String apiKey; // encrypted

    @Column(name = "api_secret", columnDefinition = "TEXT")
    private String apiSecret; // encrypted

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
