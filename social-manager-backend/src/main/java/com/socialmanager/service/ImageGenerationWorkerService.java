package com.socialmanager.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialmanager.model.ImageGeneration;
import com.socialmanager.repository.ImageGenerationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationWorkerService {

    private final ImageGenerationRepository imageGenerationRepository;
    private final ImageGenService imageGenService;
    private final CloudinaryService cloudinaryService;

    @Scheduled(fixedDelayString = "${app.ai-image.worker-delay-ms:5000}")
    @Transactional
    public void processPendingImages() {
        List<ImageGeneration> pendingItems = imageGenerationRepository.findByStatus("PENDING");
        if (pendingItems.isEmpty()) {
            return;
        }

        log.info("🛠️ AI Image Worker: found {} PENDING item(s)", pendingItems.size());

        for (ImageGeneration item : pendingItems) {
            try {
                String generationId = item.getLeonardoGenerationId();
                if (generationId == null || generationId.isBlank()) {
                    item.setStatus("FAILED");
                    imageGenerationRepository.save(item);
                    log.warn("❌ Item {} missing generationId -> FAILED", item.getId());
                    continue;
                }

                String result = imageGenService.getImageUrl(generationId);

                if (result == null || "PENDING".equals(result)) {
                    continue;
                }

                if ("ERROR".equals(result)) {
                    item.setStatus("FAILED");
                    imageGenerationRepository.save(item);
                    log.warn("❌ Leonardo returned ERROR for item {} -> FAILED", item.getId());
                    continue;
                }

                String cloudinaryUrl = cloudinaryService.upload(result);
                if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
                    item.setStatus("FAILED");
                    imageGenerationRepository.save(item);
                    log.warn("❌ Cloudinary upload failed for item {} -> FAILED", item.getId());
                    continue;
                }

                item.setCloudinaryUrl(cloudinaryUrl);
                item.setCloudinaryUrls(new String[]{cloudinaryUrl});
                item.setStatus("COMPLETE");
                item.setCompletedAt(LocalDateTime.now());
                imageGenerationRepository.save(item);

                log.info("✅ Item {} completed with Cloudinary URL", item.getId());
            } catch (Exception e) {
                log.error("❌ Worker failed for image item {}: {}", item.getId(), e.getMessage(), e);
            }
        }
    }
}
