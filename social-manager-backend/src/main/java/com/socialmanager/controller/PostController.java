package com.socialmanager.controller;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.PagedHistoryResponse;
import com.socialmanager.dto.PostHistoryFilter;
import com.socialmanager.dto.PostHistoryItem;
import com.socialmanager.dto.ReschedulePostRequest;
import com.socialmanager.dto.ScheduledPostRequest;
import com.socialmanager.dto.ScheduledPostResponse;
import com.socialmanager.model.ScheduledPost;
import com.socialmanager.repository.ScheduledPostRepository;
import com.socialmanager.service.CurrentUserService;
import com.socialmanager.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ScheduledPostRepository scheduledPostRepository;
    private final CurrentUserService currentUserService;

    @PostMapping("/preview")
    public ResponseEntity<ScheduledPostResponse> previewPost(@Valid @RequestBody ScheduledPostRequest request) {
        return ResponseEntity.ok(postService.preview(request));
    }

    @PostMapping("/schedule")
    public ResponseEntity<ScheduledPostResponse> schedulePost(@Valid @RequestBody ScheduledPostRequest request) {
        ScheduledPostResponse response = postService.createScheduledPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/monitor/summary")
    public ResponseEntity<Map<String, Long>> monitorSummary() {
        UUID userId = currentUserService.getCurrentUser().getId();
        Map<String, Long> summary;
        try {
            summary = Map.of(
                    "pending", scheduledPostRepository.countByUser_IdAndStatus(userId, "PENDING"),
                    "processing", scheduledPostRepository.countByUser_IdAndStatus(userId, "PROCESSING"),
                    "posted", scheduledPostRepository.countByUser_IdAndStatus(userId, "POSTED"),
                    "failed", scheduledPostRepository.countByUser_IdAndStatus(userId, "FAILED")
            );
        } catch (Exception ex) {
            summary = Map.of(
                    "pending", 0L,
                    "processing", 0L,
                    "posted", 0L,
                    "failed", 0L
            );
        }
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/monitor/recent")
    public ResponseEntity<List<Map<String, Object>>> monitorRecent() {
        UUID userId = currentUserService.getCurrentUser().getId();
        List<Map<String, Object>> items;
        try {
            items = scheduledPostRepository.findTop20ByUser_IdOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(this::toMonitorItem)
                    .toList();
        } catch (Exception ex) {
            items = List.of();
        }
        return ResponseEntity.ok(items);
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedHistoryResponse<PostHistoryItem>>> history(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) UUID socialAccountId,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        PostHistoryFilter filter = new PostHistoryFilter(
                normalize(status),
                parsePlatform(platform),
                socialAccountId,
                from,
                to,
                normalize(search)
        );

        List<PostHistoryItem> filtered = postService.findHistoryForCurrentUser().stream()
                .map(PostHistoryItem::from)
                .filter(item -> matchesFilter(item, filter))
                .toList();

        int fromIndex = Math.min(safePage * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());
        List<PostHistoryItem> history = filtered.subList(fromIndex, toIndex);
        boolean hasNext = toIndex < filtered.size();

        return ResponseEntity.ok(ApiResponse.ok(new PagedHistoryResponse<>(history, filtered.size(), safePage, safeSize, hasNext)));
    }

    @PatchMapping("/{postId}/cancel")
    public ResponseEntity<ApiResponse<ScheduledPostResponse>> cancelPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(ApiResponse.ok(postService.cancelPost(postId)));
    }

    @PatchMapping("/{postId}/reschedule")
    public ResponseEntity<ApiResponse<ScheduledPostResponse>> reschedulePost(
            @PathVariable UUID postId,
            @Valid @RequestBody ReschedulePostRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(postService.reschedulePost(postId, request.scheduledTime())));
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

    private boolean matchesFilter(PostHistoryItem item, PostHistoryFilter filter) {
        if (filter.status() != null && !filter.status().equalsIgnoreCase(item.status())) {
            return false;
        }

        if (filter.platform() != null && item.platform() != filter.platform()) {
            return false;
        }

        if (filter.socialAccountId() != null && (item.socialAccountId() == null || !filter.socialAccountId().toString().equals(item.socialAccountId()))) {
            return false;
        }

        if (filter.from() != null && (item.scheduledTime() == null || item.scheduledTime().isBefore(filter.from()))) {
            return false;
        }

        if (filter.to() != null && (item.scheduledTime() == null || item.scheduledTime().isAfter(filter.to()))) {
            return false;
        }

        if (filter.search() != null && !filter.search().isBlank()) {
            String haystack = String.join(" ",
                    safe(item.content()),
                    safe(item.socialAccountName()),
                    safe(item.status()),
                    safe(item.errorMessage()),
                    safe(item.publishedPostId()));
            if (!haystack.toLowerCase().contains(filter.search().toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private com.socialmanager.model.Platform parsePlatform(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return com.socialmanager.model.Platform.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            return null;
        }
    }

    // Optional: Get all pending posts for user
    // @GetMapping("/pending")
    // ...
}