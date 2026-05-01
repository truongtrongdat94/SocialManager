package com.socialmanager.scheduler;

import com.socialmanager.model.ImageGeneration;
import com.socialmanager.repository.ImageGenerationRepository;
import com.socialmanager.service.CloudinaryService;
import com.socialmanager.service.ImageGenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean({ImageGenService.class, CloudinaryService.class})
public class ImageCheckScheduler {

    private final ImageGenService imageGenService;
    private final ImageGenerationRepository repository;
    private final CloudinaryService cloudinaryService;

    // Cứ 15 giây quét DB một lần để tìm các bản ghi PENDING
    @Scheduled(fixedDelay = 15000) 
    public void checkPendingImages() {
        List<ImageGeneration> pendingList = repository.findByStatus("PENDING");
        
        if (pendingList.isEmpty()) {
            return;
        }

        log.info("🤖 Scheduler đang kiểm tra {} ảnh đang chờ...", pendingList.size());

        for (ImageGeneration img : pendingList) {
            try {
                // Gọi sang Leonardo hỏi link ảnh
                String result = imageGenService.getImageUrl(img.getLeonardoGenerationId());
                
                if (result != null && result.startsWith("http")) {
                    log.info("🎯 Ảnh đã xong, chuẩn bị đẩy lên Cloudinary...");
                    
                    //  Upload từ link Leonardo sang Cloudinary
                    String cloudinaryUrl = cloudinaryService.upload(result);
                    
                    //  Cập nhật thông tin vào DB
                    img.setCloudinaryUrl(cloudinaryUrl);
                    img.setStatus("COMPLETED");
                    repository.save(img);
                    
                    log.info("✅ Hoàn tất bài đăng ID: {}", img.getId());
                } else if ("ERROR".equals(result)) {
                    img.setStatus("FAILED");
                    repository.save(img);
                }
            } catch (Exception e) {
                log.error("❌ Lỗi khi xử lý ảnh ID {}: {}", img.getId(), e.getMessage());
            }
        }
    }
}