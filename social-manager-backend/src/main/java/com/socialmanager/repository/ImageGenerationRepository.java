package com.socialmanager.repository;

import com.socialmanager.model.ImageGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImageGenerationRepository extends JpaRepository<ImageGeneration, UUID> {
    
    // Tìm các yêu cầu đang chờ xử lý
    List<ImageGeneration> findByStatus(String status);
    
    // Tìm các yêu cầu của một User cụ thể
    List<ImageGeneration> findByUserId(UUID userId);

    // Lấy danh sách mới nhất
    List<ImageGeneration> findAllByOrderByCreatedAtDesc();
}