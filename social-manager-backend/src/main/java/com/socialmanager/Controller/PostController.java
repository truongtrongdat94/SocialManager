package com.socialmanager.controller;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.external.*;
import com.socialmanager.dto.request.CreatePhotoRequest;
import com.socialmanager.dto.request.CreatePostRequest;
import com.socialmanager.dto.request.UpdatePostRequest;
import com.socialmanager.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * GET /api/posts/{pageId}
     * Lấy danh sách bài viết của Page
     */
    @GetMapping("/{pageId}")
    public ResponseEntity<ApiResponse<List<FacebookPost>>> getPosts(
        @PathVariable String pageId,
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<FacebookPost> posts = postService.getPagePosts(getCurrentUsername(), pageId, limit);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách bài viết thành công", posts));
    }

    /**
     * GET /api/posts/{pageId}/scheduled
     * Lấy danh sách bài đã lên lịch
     */
    @GetMapping("/{pageId}/scheduled")
    public ResponseEntity<ApiResponse<List<FacebookPost>>> getScheduledPosts(
        @PathVariable String pageId
    ) {
        List<FacebookPost> posts = postService.getScheduledPosts(getCurrentUsername(), pageId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách bài đã lên lịch thành công", posts));
    }

    /**
     * POST /api/posts/{pageId}
     * Đăng bài text/link lên Facebook Page
     * Hỗ trợ scheduling với scheduledPublishTime (Unix timestamp)
     */
    @PostMapping("/{pageId}")
    public ResponseEntity<ApiResponse<FacebookPostResponse>> createPost(
        @PathVariable String pageId,
        @Valid @RequestBody CreatePostRequest request
    ) {
        FacebookPostResponse response = postService.createPost(getCurrentUsername(), pageId, request);
        
        String message = request.scheduledPublishTime() != null 
            ? "Đã lên lịch đăng bài thành công" 
            : "Đã đăng bài thành công";
        
        return ResponseEntity.ok(new ApiResponse<>(true, message, response));
    }

    /**
     * POST /api/posts/{pageId}/photo
     * Đăng ảnh lên Facebook Page
     * Hỗ trợ scheduling với scheduledPublishTime (Unix timestamp)
     */
    @PostMapping("/{pageId}/photo")
    public ResponseEntity<ApiResponse<FacebookPhotoResponse>> createPhotoPost(
        @PathVariable String pageId,
        @Valid @RequestBody CreatePhotoRequest request
    ) {
        FacebookPhotoResponse response = postService.createPhotoPost(getCurrentUsername(), pageId, request);
        
        String message = request.scheduledPublishTime() != null 
            ? "Đã lên lịch đăng ảnh thành công" 
            : "Đã đăng ảnh thành công";
        
        return ResponseEntity.ok(new ApiResponse<>(true, message, response));
    }

    /**
     * PATCH /api/posts/{postId}
     * Cập nhật nội dung bài viết
     */
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<FacebookUpdateResponse>> updatePost(
        @PathVariable String postId,
        @Valid @RequestBody UpdatePostRequest request
    ) {
        FacebookUpdateResponse response = postService.updatePost(getCurrentUsername(), postId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã cập nhật bài viết thành công", response));
    }

    /**
     * DELETE /api/posts/{postId}?pageId=xxx
     * Xóa bài viết
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<FacebookDeleteResponse>> deletePost(
        @PathVariable String postId,
        @RequestParam String pageId
    ) {
        FacebookDeleteResponse response = postService.deletePost(getCurrentUsername(), postId, pageId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã xóa bài viết thành công", response));
    }
}
