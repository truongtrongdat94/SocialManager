package com.socialmanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_insights",
    indexes = { @Index(columnList = "date") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_post_id", nullable = false)
    private ScheduledPost scheduledPost;

    private String platform;
    private LocalDate date;
    private Integer impressions;
    private Integer reach;
    private Integer likes;
    private Integer comments;
    private Integer shares;
    private Integer saves;

    @Column(name = "engagement_rate", precision = 10, scale = 4)
    private BigDecimal engagementRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
