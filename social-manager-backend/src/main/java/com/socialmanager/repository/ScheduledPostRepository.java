package com.socialmanager.repository;

import com.socialmanager.model.ScheduledPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledPostRepository extends JpaRepository<ScheduledPost, UUID> {
    List<ScheduledPost> findByStatus(String status);
}
