package com.socialmanager.repository;

import com.socialmanager.model.ScheduledPost;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduledPostRepository extends JpaRepository<ScheduledPost, UUID> {
    List<ScheduledPost> findByStatus(String status);

    @EntityGraph(attributePaths = {"user", "socialAccount"})
    Optional<ScheduledPost> findDetailedById(UUID id);

    @EntityGraph(attributePaths = {"user", "socialAccount"})
    @Query("select sp from ScheduledPost sp where sp.user.id = :userId order by sp.createdAt desc")
    List<ScheduledPost> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"user", "socialAccount"})
    List<ScheduledPost> findByStatusAndScheduledTimeLessThanEqual(String status, LocalDateTime scheduledTime);

    ScheduledPost findTopByStatusOrderByCreatedAtDesc(String status);
}