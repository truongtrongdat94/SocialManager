package com.socialmanager.service;

import com.socialmanager.dto.response.PostHistoryResponse;
import com.socialmanager.dto.response.PostHistoryStatsResponse;
import com.socialmanager.exception.ResourceNotFoundException;
import com.socialmanager.exception.UnauthorizedException;
import com.socialmanager.model.*;
import com.socialmanager.repository.PostHistoryRepository;
import com.socialmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PostHistoryService {

    private final PostHistoryRepository postHistoryRepository;
    private final UserRepository userRepository;

    /**
     * Save post history after successful posting
     */
    public PostHistory savePostHistory(
        User user,
        SocialAccount socialAccount,
        String externalPostId,
        String content,
        String mediaUrl,
        PostType postType,
        PostCreatedBy createdBy
    ) {
        PostHistory history = PostHistory.builder()
            .user(user)
            .socialAccount(socialAccount)
            .socialAccountIdSnapshot(socialAccount.getId())
            .socialAccountNameSnapshot(socialAccount.getAccountName())
            .platform(socialAccount.getPlatform())
            .externalPostId(externalPostId)
            .content(content)
            .mediaUrl(mediaUrl)
            .postType(postType)
            .createdBy(createdBy)
            .publishedAt(LocalDateTime.now())
            .build();

        PostHistory saved = postHistoryRepository.save(history);
        log.info("Saved post history: {} for user: {}", saved.getId(), user.getUsername());
        return saved;
    }

    /**
     * Get post history for user with pagination
     */
    public Page<PostHistoryResponse> getPostHistory(String usernameOrEmail, int page, int size) {
        User user = userRepository.findByUsername(usernameOrEmail)
            .or(() -> userRepository.findByEmail(usernameOrEmail))
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + usernameOrEmail));

        Pageable pageable = PageRequest.of(page, size);
        Page<PostHistory> historyPage = postHistoryRepository.findByUserIdOrderByPublishedAtDesc(
            user.getId(), 
            pageable
        );

        return historyPage.map(PostHistoryResponse::from);
    }

    /**
     * Get post history by platform
     */
    public Page<PostHistoryResponse> getPostHistoryByPlatform(
        String usernameOrEmail, 
        Platform platform, 
        int page, 
        int size
    ) {
        User user = userRepository.findByUsername(usernameOrEmail)
            .or(() -> userRepository.findByEmail(usernameOrEmail))
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + usernameOrEmail));

        Pageable pageable = PageRequest.of(page, size);
        Page<PostHistory> historyPage = postHistoryRepository.findByUserIdAndPlatformOrderByPublishedAtDesc(
            user.getId(), 
            platform, 
            pageable
        );

        return historyPage.map(PostHistoryResponse::from);
    }

    /**
     * Get post history by social account
     */
    public Page<PostHistoryResponse> getPostHistoryBySocialAccount(
        UUID socialAccountId, 
        int page, 
        int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostHistory> historyPage = postHistoryRepository.findBySocialAccountIdOrderByPublishedAtDesc(
            socialAccountId, 
            pageable
        );

        return historyPage.map(PostHistoryResponse::from);
    }

    /**
     * Get post history statistics
     */
    public PostHistoryStatsResponse getPostHistoryStats(String usernameOrEmail) {
        User user = userRepository.findByUsername(usernameOrEmail)
            .or(() -> userRepository.findByEmail(usernameOrEmail))
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + usernameOrEmail));

        UUID userId = user.getId();

        long totalPosts = postHistoryRepository.countByUserId(userId);
        long manualPosts = postHistoryRepository.countByUserIdAndCreatedBy(userId, PostCreatedBy.MANUAL);
        long autoPilotPosts = postHistoryRepository.countByUserIdAndCreatedBy(userId, PostCreatedBy.AUTO_PILOT);
        long facebookPosts = postHistoryRepository.countByUserIdAndPlatform(userId, Platform.FACEBOOK);
        long instagramPosts = postHistoryRepository.countByUserIdAndPlatform(userId, Platform.INSTAGRAM);
        long threadsPosts = postHistoryRepository.countByUserIdAndPlatform(userId, Platform.THREADS);
        long tiktokPosts = postHistoryRepository.countByUserIdAndPlatform(userId, Platform.TIKTOK);

        return new PostHistoryStatsResponse(
            totalPosts,
            manualPosts,
            autoPilotPosts,
            facebookPosts,
            instagramPosts,
            threadsPosts,
            tiktokPosts
        );
    }

    /**
     * Delete post history
     */
    public void deletePostHistory(UUID historyId, String usernameOrEmail) {
        PostHistory history = postHistoryRepository.findById(historyId)
            .orElseThrow(() -> new ResourceNotFoundException("Post history not found: " + historyId));

        User user = userRepository.findByUsername(usernameOrEmail)
            .or(() -> userRepository.findByEmail(usernameOrEmail))
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + usernameOrEmail));

        // Security check: ensure history belongs to user
        if (!history.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Unauthorized to delete this post history");
        }

        postHistoryRepository.delete(history);
        log.info("Deleted post history: {} by user: {}", historyId, usernameOrEmail);
    }
}
