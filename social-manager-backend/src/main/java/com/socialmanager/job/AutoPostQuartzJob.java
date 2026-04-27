package com.socialmanager.job;

import com.socialmanager.model.ScheduledPost;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.repository.ScheduledPostRepository;
import com.socialmanager.service.post.PlatformApiService; // Assume this handles FB/TikTok posting
import com.socialmanager.service.utils.MediaPreparationService;
import com.socialmanager.service.utils.TokenCryptoService;
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
    private TokenCryptoService tokenCryptoService;

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
            
            // Decrypt Token (Critical for your part)
            String decryptedToken = tokenCryptoService.decrypt(account.getAccessToken());
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

}