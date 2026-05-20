package com.socialmanager.service;

import com.socialmanager.model.ScheduledPost;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.model.User;
import com.socialmanager.repository.ScheduledPostRepository;
import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduledPostService {
    private final ScheduledPostRepository scheduledPostRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final SocialAccountService socialAccountService;
    private final TaskScheduler taskScheduler;

    @Transactional
    public UUID scheduleFacebookPost(UUID accountId, String username, String caption, List<String> mediaUrls, java.time.LocalDateTime scheduledTime) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        SocialAccount account = socialAccountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Social account not found"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        ScheduledPost post = ScheduledPost.builder()
            .user(user)
            .socialAccount(account)
            .caption(caption)
            .mediaUrls(mediaUrls == null ? null : mediaUrls.toArray(new String[0]))
            .scheduledTime(scheduledTime)
            .status("PENDING")
            .build();

        scheduledPostRepository.save(post);

        scheduleExecution(post);

        return post.getId();
    }

    private void scheduleExecution(ScheduledPost post) {
        Date runAt = Date.from(post.getScheduledTime().atZone(ZoneId.systemDefault()).toInstant());
        taskScheduler.schedule(() -> executePost(post.getId()), runAt);
    }

    @Transactional
    protected void executePost(UUID scheduledPostId) {
        ScheduledPost post = scheduledPostRepository.findById(scheduledPostId).orElse(null);
        if (post == null) return;
        try {
            String publishedId = socialAccountService.publishFacebookPost(post.getSocialAccount().getId(), post.getUser().getUsername(), post.getCaption(), post.getMediaUrls() == null ? null : java.util.Arrays.asList(post.getMediaUrls()));
            post.setStatus("POSTED");
            post.setPublishedPostId(publishedId);
            scheduledPostRepository.save(post);
        } catch (Exception e) {
            post.setStatus("FAILED");
            post.setErrorMessage(e.getMessage());
            scheduledPostRepository.save(post);
        }
    }

    @PostConstruct
    public void schedulePendingPosts() {
        List<ScheduledPost> pending = scheduledPostRepository.findByStatus("PENDING");
        for (ScheduledPost post : pending) {
            // If scheduled time is in the past, execute immediately
            if (post.getScheduledTime().isBefore(java.time.LocalDateTime.now())) {
                taskScheduler.schedule(() -> executePost(post.getId()), new Date());
            } else {
                scheduleExecution(post);
            }
        }
    }
}
