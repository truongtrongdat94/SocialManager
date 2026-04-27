package com.socialmanager.service.post;

import com.socialmanager.dto.ScheduledPostRequest;
import com.socialmanager.dto.ScheduledPostResponse;
import com.socialmanager.exception.BusinessException;
import com.socialmanager.model.ScheduledPost;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.repository.ScheduledPostRepository;
import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.service.account.CurrentUserService;
import com.socialmanager.service.utils.TokenCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final ScheduledPostRepository postRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final TokenCryptoService tokenCryptoService;
    private final CurrentUserService currentUserService;

    @Value("${app.posting.daily-quota:5}")
    private int dailyQuota;

    // 1. Manual Create
    @Transactional
    public ScheduledPostResponse createScheduledPost(ScheduledPostRequest request) {
        UUID socialAccountId = parseUuid(request.getSocialAccountId(), "socialAccountId");
        SocialAccount account = socialAccountRepository.findByIdAndUserId(socialAccountId, currentUserService.getCurrentUser().getId())
                .orElseThrow(() -> new BusinessException("Social Account not found"));

        // Quota Check
        checkQuota(account.getId());

        // Decrypt token just to verify it's valid (optional, or do it in Job)
        tokenCryptoService.decrypt(account.getAccessToken());

        ScheduledPost post = new ScheduledPost();
        post.setUser(account.getUser());
        post.setSocialAccount(account);
        post.setCaption(request.getContent());
        post.setMediaUrls(toMediaUrls(request.getMediaUrl(), request.getMediaUrls()));
        post.setScheduledTime(request.getScheduledTime());
        post.setStatus(STATUS_PENDING);

        ScheduledPost saved = postRepository.save(post);
        return mapToResponse(saved);
    }

    // 2. Preview (Generates a preview object without saving)
    public ScheduledPostResponse preview(ScheduledPostRequest request) {
        UUID socialAccountId = parseUuid(request.getSocialAccountId(), "socialAccountId");
        SocialAccount account = socialAccountRepository.findByIdAndUserId(socialAccountId, currentUserService.getCurrentUser().getId())
                .orElseThrow(() -> new BusinessException("Social Account not found"));
        
        ScheduledPost mockPost = new ScheduledPost();
        mockPost.setUser(account.getUser());
        mockPost.setSocialAccount(account);
        mockPost.setCaption(request.getContent());
        mockPost.setMediaUrls(toMediaUrls(request.getMediaUrl(), request.getMediaUrls()));
        mockPost.setScheduledTime(request.getScheduledTime());
        mockPost.setStatus(STATUS_PENDING);
        return mapToResponse(mockPost);
    }

    // 3. Quota Logic
        private void checkQuota(UUID accountId) {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        
        long count = postRepository.countBySocialAccount_IdAndStatusAndScheduledTimeBetween(
                accountId, 
            STATUS_PENDING,
                startOfDay, 
                endOfDay
        );
        
        // Also count PROCESSING/PUBLISHED today if you want total limit, not just pending
        // long totalToday = count + postRepository.countPublishedToday(...);
        
        if (count >= dailyQuota) {
            throw new BusinessException("Daily post quota exceeded. Max: " + dailyQuota);
        }
    }

    private ScheduledPostResponse mapToResponse(ScheduledPost post) {
        ScheduledPostResponse res = new ScheduledPostResponse();
        res.setId(post.getId() != null ? post.getId().toString() : "PREVIEW");
        res.setContent(post.getCaption());
        res.setMediaUrl(firstMediaUrl(post.getMediaUrls()));
        res.setScheduledTime(post.getScheduledTime());
        res.setStatus(post.getStatus());
        res.setSocialAccountName(post.getSocialAccount().getAccountName());
        return res;
    }

    private String[] toMediaUrls(String mediaUrl, List<String> mediaUrls) {
        List<String> merged = new ArrayList<>();
        if (mediaUrl != null && !mediaUrl.isBlank()) {
            merged.add(mediaUrl.trim());
        }

        if (mediaUrls != null) {
            mediaUrls.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .forEach(merged::add);
        }

        if (merged.isEmpty()) {
            return null;
        }

        // Preserve input order while removing duplicates.
        List<String> deduplicated = new ArrayList<>(new LinkedHashSet<>(merged));
        return deduplicated.toArray(new String[0]);
    }

    private String firstMediaUrl(String[] mediaUrls) {
        if (mediaUrls == null || mediaUrls.length == 0) {
            return null;
        }
        return mediaUrls[0];
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getMonitorSummaryForCurrentUser() {
        UUID userId = currentUserService.getCurrentUser().getId();
        return Map.of(
                "pending", postRepository.countByUser_IdAndStatus(userId, "PENDING"),
                "processing", postRepository.countByUser_IdAndStatus(userId, "PROCESSING"),
                "posted", postRepository.countByUser_IdAndStatus(userId, "POSTED"),
                "failed", postRepository.countByUser_IdAndStatus(userId, "FAILED")
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentMonitorItemsForCurrentUser() {
        UUID userId = currentUserService.getCurrentUser().getId();
        return postRepository.findTop20ByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toMonitorItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduledPost> findHistoryForCurrentUser() {
        return postRepository.findByUser_IdOrderByCreatedAtDesc(currentUserService.getCurrentUser().getId());
    }

    @Transactional
    public ScheduledPostResponse cancelPost(UUID postId) {
        ScheduledPost post = findOwnedPost(postId);
        ensureMutable(post, "cancel");

        post.setStatus(STATUS_CANCELLED);
        post.setErrorMessage(null);
        post.setPublishedPostId(null);
        post.setLastAttemptAt(LocalDateTime.now());

        return mapToResponse(postRepository.save(post));
    }

    @Transactional
    public ScheduledPostResponse reschedulePost(UUID postId, LocalDateTime scheduledTime) {
        if (scheduledTime == null) {
            throw new BusinessException("scheduledTime is required");
        }

        ScheduledPost post = findOwnedPost(postId);
        ensureMutable(post, "reschedule");

        post.setScheduledTime(scheduledTime);
        post.setStatus(STATUS_PENDING);
        post.setErrorMessage(null);
        post.setPublishedPostId(null);
        post.setRetryCount(0);
        post.setLastAttemptAt(null);

        return mapToResponse(postRepository.save(post));
    }

    private ScheduledPost findOwnedPost(UUID postId) {
        return postRepository.findByIdAndUser_Id(postId, currentUserService.getCurrentUser().getId())
                .orElseThrow(() -> new BusinessException("Post not found"));
    }

    private void ensureMutable(ScheduledPost post, String action) {
        if (!STATUS_PENDING.equals(post.getStatus()) && !"FAILED".equals(post.getStatus())) {
            throw new BusinessException("Only pending or failed posts can be " + action + "d");
        }
    }

    private UUID parseUuid(String rawValue, String fieldName) {
        try {
            return UUID.fromString(rawValue);
        } catch (Exception ex) {
            throw new BusinessException(fieldName + " must be a valid UUID");
        }
    }

    private Map<String, Object> toMonitorItem(ScheduledPost post) {
        return Map.of(
                "id", post.getId() != null ? post.getId().toString() : "",
                "status", safe(post.getStatus()),
                "retryCount", post.getRetryCount() == null ? 0 : post.getRetryCount(),
                "scheduledTime", toIso(post.getScheduledTime()),
                "lastAttemptAt", toIso(post.getLastAttemptAt()),
                "publishedPostId", safe(post.getPublishedPostId()),
                "errorMessage", safe(post.getErrorMessage())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String toIso(LocalDateTime value) {
        return value == null ? "" : value.toString();
    }
}