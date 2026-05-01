package com.socialmanager.job;

import com.socialmanager.model.ScheduledPost;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.repository.ScheduledPostRepository;
import com.socialmanager.service.post.PlatformApiService;
import com.socialmanager.service.utils.MediaPreparationService;
import com.socialmanager.util.EncryptionUtil;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Component
@DisallowConcurrentExecution
public class AutoPostQuartzJob implements Job {

    private static final Logger log = Logger.getLogger(AutoPostQuartzJob.class.getName());

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_PUBLISHED = "POSTED";
    private static final String STATUS_FAILED = "FAILED";

    @Autowired
    private ScheduledPostRepository postRepository;

    @Autowired
    private PlatformApiService platformApiService;

    @Autowired
    private MediaPreparationService mediaPreparationService;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Value("${app.posting.batch-size:50}")
    private int batchSize;

    @Value("${app.posting.max-job-attempts:3}")
    private int maxJobAttempts;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("AutoPostQuartzJob started...");
        List<UUID> candidatePostIds = postRepository.findReadyToPublishIds(
                STATUS_PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, Math.max(1, batchSize))
        );

        int processedCount = 0;
        for (UUID postId : candidatePostIds) {
            int claimed = postRepository.claimPostForProcessing(postId, STATUS_PENDING, STATUS_PROCESSING);
            if (claimed == 0) {
                continue;
            }

            postRepository.findByIdWithSocialAccount(postId)
                    .ifPresent(this::processClaimedPost);
            processedCount++;
        }

        log.info("AutoPostQuartzJob finished. Processed " + processedCount + " posts.");
    }

    private void processClaimedPost(ScheduledPost post) {
        post.setLastAttemptAt(LocalDateTime.now());
        int nextRetryCount = (post.getRetryCount() == null ? 0 : post.getRetryCount()) + 1;
        post.setRetryCount(nextRetryCount);

        try {
            SocialAccount account = post.getSocialAccount();
            
            // Decrypt Token
            String decryptedToken = encryptionUtil.decrypt(account.getAccessToken());
                String platformMediaUrl = mediaPreparationService.preparePrimaryMediaUrl(account.getPlatform(), post.getMediaUrls());
            
            // Call Platform API (Facebook Graph API or TikTok Business API)
            String platformPostId = platformApiService.publishPost(
                    account.getPlatform().name(),
                    account.getExternalAccountId(),
                    decryptedToken, 
                    post.getCaption(),
                    platformMediaUrl,
                    post.getId().toString()
            );

            post.setStatus(STATUS_PUBLISHED);
            post.setPublishedPostId(platformPostId);
                // attempt to build a public URL for the published post if possible
                post.setPublishedPostUrl(buildPublishedUrl(account.getPlatform().name(), account.getExternalAccountId(), platformPostId));
                post.setErrorMessage(null);
            
        } catch (Exception e) {
            log.severe("Failed to publish scheduled post " + post.getId() + ": " + e.getMessage());
            if (nextRetryCount < Math.max(1, maxJobAttempts)) {
                post.setStatus(STATUS_PENDING);
            } else {
                post.setStatus(STATUS_FAILED);
            }
            post.setErrorMessage(e.getMessage());
        }
        
        postRepository.save(post);
    }

    private String buildPublishedUrl(String platform, String externalAccountId, String platformPostId) {
        if (platformPostId == null || platformPostId.isBlank()) return null;
        try {
            switch (platform.toUpperCase()) {
                case "FACEBOOK":
                    // If id looks like pageId_postId
                    if (platformPostId.contains("_")) {
                        String[] parts = platformPostId.split("_");
                        if (parts.length >= 2) {
                            return "https://www.facebook.com/" + parts[0] + "/posts/" + parts[1];
                        }
                    }
                    return "https://www.facebook.com/" + platformPostId;
                case "INSTAGRAM":
                case "THREADS":
                    // Cannot reliably map numeric ids to shortcodes; return null
                    return null;
                case "TIKTOK":
                    // If externalAccountId looks like username, construct a likely URL
                    if (externalAccountId != null && !externalAccountId.isBlank()) {
                        return "https://www.tiktok.com/" + externalAccountId + "/video/" + platformPostId;
                    }
                    return null;
                default:
                    return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }

}