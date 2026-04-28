package com.socialmanager.controller;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.AiPostComposeRequest;
import com.socialmanager.dto.AiPostComposeResponse;
import com.socialmanager.dto.AiPostSourcesResponse;
import com.socialmanager.dto.PagedHistoryResponse;
import com.socialmanager.dto.PostHistoryFilter;
import com.socialmanager.dto.PostHistoryItem;
import com.socialmanager.dto.ReschedulePostRequest;
import com.socialmanager.dto.ScheduledPostRequest;
import com.socialmanager.dto.ScheduledPostResponse;
import com.socialmanager.service.post.AiPostComposeService;
import com.socialmanager.service.post.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final AiPostComposeService aiPostComposeService;

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<ScheduledPostResponse>> previewPost(@Valid @RequestBody ScheduledPostRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(postService.preview(request)));
    }

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<ScheduledPostResponse>> schedulePost(@Valid @RequestBody ScheduledPostRequest request) {
        ScheduledPostResponse response = postService.createScheduledPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/ai/preview")
    public ResponseEntity<ApiResponse<AiPostComposeResponse>> previewFromAi(@Valid @RequestBody AiPostComposeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(aiPostComposeService.preview(request)));
    }

    @PostMapping("/ai/schedule")
    public ResponseEntity<ApiResponse<AiPostComposeResponse>> scheduleFromAi(@Valid @RequestBody AiPostComposeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(aiPostComposeService.schedule(request)));
    }

    @GetMapping("/ai/sources")
    public ResponseEntity<ApiResponse<AiPostSourcesResponse>> listAiSources(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(aiPostComposeService.listSources(limit)));
    }

    @GetMapping("/monitor/summary")
    public ResponseEntity<Map<String, Long>> monitorSummary() {
        return ResponseEntity.ok(postService.getMonitorSummaryForCurrentUser());
    }

    @GetMapping("/monitor/recent")
    public ResponseEntity<List<Map<String, Object>>> monitorRecent() {
        return ResponseEntity.ok(postService.getRecentMonitorItemsForCurrentUser());
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

    private String safe(String value) {
        return value == null ? "" : value;
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