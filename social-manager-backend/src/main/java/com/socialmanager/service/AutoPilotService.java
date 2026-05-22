package com.socialmanager.service;

import com.socialmanager.dto.request.CreatePhotoRequest;
import com.socialmanager.dto.request.CreatePostRequest;
import com.socialmanager.exception.ResourceNotFoundException;
import com.socialmanager.model.AiGenerationLog;
import com.socialmanager.model.AutoPilotConfig;
import com.socialmanager.model.AutoPilotStatus;
import com.socialmanager.model.ImageGeneration;
import com.socialmanager.model.User;
import com.socialmanager.repository.AutoPilotConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AutoPilotService {

    private final AutoPilotConfigRepository repo;
    private final GeminiAIService geminiAIService;
    private final ImageGenService imageGenService;
    private final PostService postService;
    private final Random random = new Random();

    public List<AutoPilotConfig> getConfigsToRun() {
        return repo.findByStatusAndNextRunAtBefore(
                AutoPilotStatus.ACTIVE,
                LocalDateTime.now()
        );
    }

    public void updateNextRun(AutoPilotConfig config) {
        config.setLastRunAt(LocalDateTime.now());
        config.setNextRunAt(LocalDateTime.now().plusHours(config.getFrequencyHours()));
        repo.save(config);
    }

    /**
     * Execute auto pilot: generate caption, generate image, schedule post
     */
    public void executeAutoPilot(AutoPilotConfig config) {
        try {
            log.info("🤖 Executing Auto Pilot for config: {} (User: {})", 
                config.getId(), config.getUser().getUsername());

            // 1. Chọn random keyword
            String keyword = selectRandomKeyword(config.getKeywords());
            log.info("📝 Selected keyword: {}", keyword);

            // 2. Generate caption từ Gemini
            String platform = config.getSocialAccount().getPlatform().name();
            AiGenerationLog captionLog = geminiAIService.createCaption(keyword, platform, config.getUser());
            String caption = captionLog.getResultCaption();
            log.info("✍️ Generated caption: {}", caption.substring(0, Math.min(50, caption.length())) + "...");

            // 3. Generate image từ Leonardo.ai
            ImageGeneration imageGen = imageGenService.startImageGeneration(keyword, config.getUser());
            
            if (imageGen == null) {
                log.warn("⚠️ Image generation failed, posting text only");
                scheduleTextPost(config, caption);
                return;
            }

            // 4. Wait for image to be ready (polling with timeout)
            String imageUrl = waitForImageGeneration(imageGen.getLeonardoGenerationId(), 60); // 60 seconds timeout
            
            if (imageUrl == null || "ERROR".equals(imageUrl)) {
                log.warn("⚠️ Image not ready in time, posting text only");
                scheduleTextPost(config, caption);
                return;
            }

            // 5. Schedule post with image
            schedulePhotoPost(config, caption, imageUrl);
            
            log.info("✅ Auto Pilot executed successfully for config: {}", config.getId());

        } catch (Exception e) {
            log.error("❌ Error executing Auto Pilot for config {}: {}", config.getId(), e.getMessage(), e);
        }
    }

    /**
     * Select random keyword from array
     */
    private String selectRandomKeyword(String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return "technology"; // default fallback
        }
        return keywords[random.nextInt(keywords.length)];
    }

    /**
     * Wait for image generation to complete (polling)
     */
    private String waitForImageGeneration(String generationId, int timeoutSeconds) {
        int attempts = 0;
        int maxAttempts = timeoutSeconds / 5; // Check every 5 seconds

        while (attempts < maxAttempts) {
            try {
                String result = imageGenService.getImageUrl(generationId);
                
                if (result != null && !result.equals("PENDING") && !result.equals("ERROR")) {
                    return result; // Image URL ready
                }
                
                if ("ERROR".equals(result)) {
                    return null;
                }

                Thread.sleep(5000); // Wait 5 seconds before next check
                attempts++;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        
        return null; // Timeout
    }

    /**
     * Schedule text-only post
     */
    private void scheduleTextPost(AutoPilotConfig config, String caption) {
        try {
            String pageId = config.getSocialAccount().getExternalAccountId();
            String username = config.getUser().getUsername();
            
            // Schedule for 10 minutes from now
            long scheduledTime = System.currentTimeMillis() / 1000 + 600;
            
            CreatePostRequest request = new CreatePostRequest(
                caption,
                null, // no link
                scheduledTime
            );
            
            postService.createPost(username, pageId, request);
            log.info("📅 Scheduled text post for page: {}", pageId);
            
        } catch (Exception e) {
            log.error("❌ Error scheduling text post: {}", e.getMessage(), e);
        }
    }

    /**
     * Schedule photo post
     */
    private void schedulePhotoPost(AutoPilotConfig config, String caption, String imageUrl) {
        try {
            String pageId = config.getSocialAccount().getExternalAccountId();
            String username = config.getUser().getUsername();
            
            // Schedule for 10 minutes from now
            long scheduledTime = System.currentTimeMillis() / 1000 + 600;
            
            CreatePhotoRequest request = new CreatePhotoRequest(
                imageUrl,
                caption,
                scheduledTime
            );
            
            postService.createPhotoPost(username, pageId, request);
            log.info("📅 Scheduled photo post for page: {}", pageId);
            
        } catch (Exception e) {
            log.error("❌ Error scheduling photo post: {}", e.getMessage(), e);
        }
    }

    // CRUD operations for AutoPilotConfig

    public AutoPilotConfig createConfig(AutoPilotConfig config) {
        // Set initial next run time
        if (config.getNextRunAt() == null) {
            config.setNextRunAt(LocalDateTime.now().plusHours(config.getFrequencyHours()));
        }
        return repo.save(config);
    }

    public AutoPilotConfig updateConfig(UUID id, AutoPilotConfig updates) {
        AutoPilotConfig existing = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("AutoPilotConfig not found: " + id));
        
        if (updates.getKeywords() != null) {
            existing.setKeywords(updates.getKeywords());
        }
        if (updates.getFrequencyHours() != null) {
            existing.setFrequencyHours(updates.getFrequencyHours());
        }
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        if (updates.getPromptTemplate() != null) {
            existing.setPromptTemplate(updates.getPromptTemplate());
        }
        
        return repo.save(existing);
    }

    public void deleteConfig(UUID id) {
        repo.deleteById(id);
    }

    public List<AutoPilotConfig> getConfigsByUser(User user) {
        return repo.findByUserId(user.getId());
    }

    public AutoPilotConfig getConfigById(UUID id) {
        return repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("AutoPilotConfig not found: " + id));
    }
}