package com.socialmanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "image_generations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImageGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(name = "leonardo_generation_id")
    private String leonardoGenerationId;

    // PENDING, COMPLETE, FAILED
    @Builder.Default
    private String status = "PENDING";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 10)
    @Column(name = "cloudinary_urls")
    private String[] cloudinaryUrls;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 10)
    @Column(name = "cloudinary_public_ids")
    private String[] cloudinaryPublicIds;

    @Column(name = "model_id")
    private String modelId;

    private Integer width;
    private Integer height;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
