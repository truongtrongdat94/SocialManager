package com.socialmanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_generation_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiGenerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "result_caption", columnDefinition = "TEXT")
    private String resultCaption;

    @Column(name = "ai_model")
    @Builder.Default
    private String aiModel = "gemini-3-flash-preview";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 10)
    @Column(name = "image_urls")
    private String[] imageUrls;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
