package com.socialmanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auto_pilot_configs",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "social_account_id"})
    },
    indexes = {
        @Index(columnList = "social_account_id"),
        @Index(columnList = "next_run_at")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AutoPilotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_account_id", nullable = false)
    private SocialAccount socialAccount;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 20)
    @Column
    private String[] keywords;

    @Builder.Default
    @Column(name = "frequency_hours")
    private Integer frequencyHours = 6;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private AutoPilotStatus status = AutoPilotStatus.ACTIVE;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
