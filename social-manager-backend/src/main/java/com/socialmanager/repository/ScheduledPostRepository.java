package com.socialmanager.repository;

import com.socialmanager.model.ScheduledPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduledPostRepository extends JpaRepository<ScheduledPost, UUID> {

    @Query("select sp.id from ScheduledPost sp where sp.status = :status and sp.scheduledTime <= :time order by sp.scheduledTime asc")
    List<UUID> findReadyToPublishIds(@Param("status") String status, @Param("time") LocalDateTime time, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("update ScheduledPost sp set sp.status = :nextStatus where sp.id = :postId and sp.status = :currentStatus")
    int claimPostForProcessing(@Param("postId") UUID postId, @Param("currentStatus") String currentStatus, @Param("nextStatus") String nextStatus);

    @Query("select sp from ScheduledPost sp join fetch sp.socialAccount sa where sp.id = :postId")
    Optional<ScheduledPost> findByIdWithSocialAccount(@Param("postId") UUID postId);

    Optional<ScheduledPost> findByIdAndUser_Id(UUID id, UUID userId);

    @Query("select sp from ScheduledPost sp join fetch sp.socialAccount sa where sp.user.id = :userId order by sp.createdAt desc")
    List<ScheduledPost> findByUser_IdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    long countByUser_IdAndStatus(UUID userId, String status);

    List<ScheduledPost> findTop20ByUser_IdOrderByCreatedAtDesc(UUID userId);
    
    // For quota checking (manual create)
    long countBySocialAccount_IdAndStatusAndScheduledTimeBetween(
        UUID accountId,
        String status,
        LocalDateTime start, 
        LocalDateTime end
    );

    long countByStatus(String status);

    List<ScheduledPost> findTop20ByOrderByCreatedAtDesc();
}