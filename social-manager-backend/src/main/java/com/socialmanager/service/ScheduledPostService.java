package com.socialmanager.service;

import com.socialmanager.model.ScheduledPost;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.model.User;
import com.socialmanager.repository.ScheduledPostRepository;
import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduledPostService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_POSTED = "POSTED";
    private static final String STATUS_FAILED = "FAILED";

    private final ScheduledPostRepository scheduledPostRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final SocialAccountService socialAccountService;
    private final TaskScheduler taskScheduler;

    @PostConstruct
    public void schedulePendingPosts() {
        scheduledPostRepository.findByStatus(STATUS_PENDING).forEach(this::scheduleTask);
    }

    @Scheduled(fixedDelay = 30000)
    public void processDueScheduledPosts() {
        LocalDateTime now = LocalDateTime.now();
        scheduledPostRepository.findByStatusAndScheduledTimeLessThanEqual(STATUS_PENDING, now)
            .forEach(this::executePost);
    }

    public ScheduledPost scheduleFacebookPost(UUID accountId, String username, String caption, List<String> mediaUrls, LocalDateTime scheduledTime) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        SocialAccount account = socialAccountRepository.findByIdAndUserId(accountId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản Facebook"));

        ScheduledPost scheduledPost = ScheduledPost.builder()
            .user(user)
            .socialAccount(account)
            .caption(caption)
            .mediaUrls(mediaUrls != null ? mediaUrls.toArray(new String[0]) : new String[0])
            .scheduledTime(scheduledTime)
            .status(STATUS_PENDING)
            .retryCount(0)
            .isAutoPilot(Boolean.FALSE)
            .build();

        ScheduledPost saved = scheduledPostRepository.save(scheduledPost);
        scheduleTask(saved);
        return saved;
    }

    private void scheduleTask(ScheduledPost scheduledPost) {
        taskScheduler.schedule(() -> executePost(scheduledPost.getId()), scheduledPost.getScheduledTime().atZone(ZoneId.systemDefault()).toInstant());
    }

    public synchronized void executePost(UUID scheduledPostId) {
        ScheduledPost scheduledPost = scheduledPostRepository.findDetailedById(scheduledPostId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài đăng đã lên lịch"));

        executePost(scheduledPost);
    }

    public synchronized void executePost(ScheduledPost scheduledPost) {

        if (!STATUS_PENDING.equals(scheduledPost.getStatus())) {
            return;
        }

        scheduledPost.setStatus(STATUS_PROCESSING);
        scheduledPost.setProcessingStartedAt(LocalDateTime.now());
        scheduledPostRepository.save(scheduledPost);

        try {
            String publishedId = socialAccountService.publishFacebookPost(
                scheduledPost.getSocialAccount().getId(),
                scheduledPost.getUser().getUsername(),
                scheduledPost.getCaption(),
                scheduledPost.getMediaUrls() != null ? Arrays.asList(scheduledPost.getMediaUrls()) : List.of()
            );
            scheduledPost.setStatus(STATUS_POSTED);
            scheduledPost.setPublishedPostId(publishedId);
            scheduledPost.setErrorMessage(null);
        } catch (Exception ex) {
            scheduledPost.setStatus(STATUS_FAILED);
            scheduledPost.setErrorMessage(ex.getMessage());
            scheduledPost.setRetryCount((scheduledPost.getRetryCount() == null ? 0 : scheduledPost.getRetryCount()) + 1);
        }

        scheduledPostRepository.save(scheduledPost);
    }
}