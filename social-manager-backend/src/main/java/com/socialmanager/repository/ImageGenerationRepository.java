package com.socialmanager.repository;

import com.socialmanager.model.ImageGeneration;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImageGenerationRepository extends JpaRepository<ImageGeneration, UUID> {

    Optional<ImageGeneration> findByIdAndUser_Id(UUID id, UUID userId);

    List<ImageGeneration> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<ImageGeneration> findByUserId(UUID userId);

    List<ImageGeneration> findByStatus(String status);

    List<ImageGeneration> findAllByOrderByCreatedAtDesc();
}
