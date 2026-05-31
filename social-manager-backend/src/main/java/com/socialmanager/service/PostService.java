package com.socialmanager.service;

import com.socialmanager.client.FacebookClient;
import com.socialmanager.dto.external.*;
import com.socialmanager.dto.request.CreatePhotoRequest;
import com.socialmanager.dto.request.CreatePostRequest;
import com.socialmanager.dto.request.UpdatePostRequest;
import com.socialmanager.model.PostCreatedBy;
import com.socialmanager.model.PostHistory;
import com.socialmanager.model.PostType;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.model.User;
import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final FacebookClient facebookClient;
    private final FacebookTokenService tokenService;
    private final PostHistoryService postHistoryService;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    /**
     * Đăng bài text/link lên Facebook Page
     */
    @CacheEvict(value = "pagePosts", key = "#pageId")
    public FacebookPostResponse createPost(String username, String pageId, CreatePostRequest request) {
        String token = tokenService.getPageToken(username, pageId);
        FacebookPostResponse response = facebookClient.createPost(pageId, token, request);
        
        // Save to post history (scheduled posts are also saved immediately)
        savePostToHistory(username, pageId, response.id(), request.message(), null, PostType.TEXT, PostCreatedBy.MANUAL);
        
        return response;
    }

    /**
     * Đăng ảnh lên Facebook Page
     */
    @CacheEvict(value = "pagePosts", key = "#pageId")
    public FacebookPhotoResponse createPhotoPost(String username, String pageId, CreatePhotoRequest request) {
        String token = tokenService.getPageToken(username, pageId);
        FacebookPhotoResponse response = facebookClient.createPhotoPost(pageId, token, request);
        
        // Save to post history (scheduled posts are also saved immediately)
        savePostToHistory(username, pageId, response.postId(), request.caption(), request.photoUrl(), PostType.PHOTO, PostCreatedBy.MANUAL);
        
        return response;
    }

    /**
     * Lấy danh sách bài viết của Page
     * Cache 5 phút
     */
    @Cacheable(value = "pagePosts", key = "#pageId")
    public List<FacebookPost> getPagePosts(String username, String pageId, int limit) {
        try {
            System.out.println("DEBUG: Getting page posts for user=" + username + ", pageId=" + pageId + ", limit=" + limit);
            String token = tokenService.getPageToken(username, pageId);
            System.out.println("DEBUG: Token retrieved successfully (length=" + (token != null ? token.length() : 0) + ")");
            
            List<FacebookPost> posts = facebookClient.getPagePosts(pageId, token, limit);
            System.out.println("DEBUG: Retrieved " + (posts != null ? posts.size() : 0) + " posts");
            
            return posts;
        } catch (Exception e) {
            System.err.println("ERROR in getPagePosts: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Lấy danh sách bài đã lên lịch (scheduled posts)
     */
    public List<FacebookPost> getScheduledPosts(String username, String pageId) {
        String token = tokenService.getPageToken(username, pageId);
        return facebookClient.getScheduledPosts(pageId, token);
    }

    /**
     * Cập nhật bài viết
     */
    @CacheEvict(value = "pagePosts", allEntries = true)
    public FacebookUpdateResponse updatePost(String username, String postId, UpdatePostRequest request) {
        String token = tokenService.getPageToken(username, request.pageId());
        return facebookClient.updatePost(postId, token, request.message());
    }

    /**
     * Xóa bài viết
     */
    @CacheEvict(value = "pagePosts", key = "#pageId")
    public FacebookDeleteResponse deletePost(String username, String postId, String pageId) {
        String token = tokenService.getPageToken(username, pageId);
        return facebookClient.deletePost(postId, token);
    }

    /**
     * Helper method to save post to history
     */
    private void savePostToHistory(
        String username, 
        String pageId, 
        String externalPostId, 
        String content, 
        String mediaUrl, 
        PostType postType,
        PostCreatedBy createdBy
    ) {
        try {
            // username parameter might be email or username, try both
            User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            SocialAccount socialAccount = socialAccountRepository
                .findByUserIdAndExternalAccountId(user.getId(), pageId)
                .orElseThrow(() -> new RuntimeException("Social account not found"));
            
            postHistoryService.savePostHistory(
                user, 
                socialAccount, 
                externalPostId, 
                content, 
                mediaUrl, 
                postType, 
                createdBy
            );
        } catch (Exception e) {
            // Log error but don't fail the post operation
            System.err.println("Failed to save post history: " + e.getMessage());
        }
    }

    /**
     * Create post from Auto Pilot (internal use)
     */
    public FacebookPostResponse createPostFromAutoPilot(
        User user, 
        String pageId, 
        CreatePostRequest request
    ) {
        String token = tokenService.getPageToken(user.getUsername(), pageId);
        FacebookPostResponse response = facebookClient.createPost(pageId, token, request);
        
        // Save to post history with AUTO_PILOT flag
        savePostToHistory(user.getUsername(), pageId, response.id(), request.message(), null, PostType.TEXT, PostCreatedBy.AUTO_PILOT);
        
        return response;
    }

    /**
     * Create photo post from Auto Pilot (internal use)
     */
    public FacebookPhotoResponse createPhotoPostFromAutoPilot(
        User user, 
        String pageId, 
        CreatePhotoRequest request
    ) {
        String token = tokenService.getPageToken(user.getUsername(), pageId);
        FacebookPhotoResponse response = facebookClient.createPhotoPost(pageId, token, request);
        
        // Save to post history with AUTO_PILOT flag
        savePostToHistory(user.getUsername(), pageId, response.postId(), request.caption(), request.photoUrl(), PostType.PHOTO, PostCreatedBy.AUTO_PILOT);
        
        return response;
    }
}
