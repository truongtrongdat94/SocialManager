package com.socialmanager.repository;

import com.socialmanager.model.Platform;
import com.socialmanager.model.PostCreatedBy;
import com.socialmanager.model.PostHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PostHistoryRepository extends JpaRepository<PostHistory, UUID> {

    // Find by user
    Page<PostHistory> findByUserIdOrderByPublishedAtDesc(UUID userId, Pageable pageable);

    // Find by user and platform
    Page<PostHistory> findByUserIdAndPlatformOrderByPublishedAtDesc(UUID userId, Platform platform, Pageable pageable);

    // Find by user and social account
    Page<PostHistory> findBySocialAccountIdOrderByPublishedAtDesc(UUID socialAccountId, Pageable pageable);

    // Find by user and created by
    Page<PostHistory> findByUserIdAndCreatedByOrderByPublishedAtDesc(UUID userId, PostCreatedBy createdBy, Pageable pageable);

    // Find by user and date range
    List<PostHistory> findByUserIdAndPublishedAtBetweenOrderByPublishedAtDesc(
        UUID userId, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );

    // Count posts by user
    long countByUserId(UUID userId);

    // Count posts by user and platform
    long countByUserIdAndPlatform(UUID userId, Platform platform);

    // Count posts by user and created by
    long countByUserIdAndCreatedBy(UUID userId, PostCreatedBy createdBy);

    void deleteBySocialAccount_Id(UUID socialAccountId);
}
