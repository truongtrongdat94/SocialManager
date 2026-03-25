package com.socialmanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "account_daily_insights",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"social_account_id", "date"})
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountDailyInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_account_id", nullable = false)
    private SocialAccount socialAccount;

    private LocalDate date;
    private Integer followers;

    @Column(name = "profile_views")
    private Integer profileViews;

    private Integer impressions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;
}
