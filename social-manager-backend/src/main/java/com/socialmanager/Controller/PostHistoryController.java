package com.socialmanager.controller;

import com.socialmanager.dto.response.PostHistoryResponse;
import com.socialmanager.dto.response.PostHistoryStatsResponse;
import com.socialmanager.model.Platform;
import com.socialmanager.model.User;
import com.socialmanager.service.PostHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/post-history")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Post History", description = "Post history management")
public class PostHistoryController {

    private final PostHistoryService postHistoryService;

    @GetMapping
    @Operation(summary = "Get post history with pagination")
    public ResponseEntity<Page<PostHistoryResponse>> getPostHistory(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Getting post history for user: {}, page: {}, size: {}", 
            currentUser.getUsername(), page, size);
        
        Page<PostHistoryResponse> history = postHistoryService.getPostHistory(
            currentUser.getUsername(), 
            page, 
            size
        );
        
        return ResponseEntity.ok(history);
    }

    @GetMapping("/platform/{platform}")
    @Operation(summary = "Get post history by platform")
    public ResponseEntity<Page<PostHistoryResponse>> getPostHistoryByPlatform(
            @PathVariable Platform platform,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Getting post history for user: {}, platform: {}", 
            currentUser.getUsername(), platform);
        
        Page<PostHistoryResponse> history = postHistoryService.getPostHistoryByPlatform(
            currentUser.getUsername(), 
            platform, 
            page, 
            size
        );
        
        return ResponseEntity.ok(history);
    }

    @GetMapping("/account/{socialAccountId}")
    @Operation(summary = "Get post history by social account")
    public ResponseEntity<Page<PostHistoryResponse>> getPostHistoryBySocialAccount(
            @PathVariable UUID socialAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Getting post history for social account: {}", socialAccountId);
        
        Page<PostHistoryResponse> history = postHistoryService.getPostHistoryBySocialAccount(
            socialAccountId, 
            page, 
            size
        );
        
        return ResponseEntity.ok(history);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get post history statistics")
    public ResponseEntity<PostHistoryStatsResponse> getPostHistoryStats(
            @AuthenticationPrincipal User currentUser
    ) {
        log.info("Getting post history stats for user: {}", currentUser.getUsername());
        
        PostHistoryStatsResponse stats = postHistoryService.getPostHistoryStats(
            currentUser.getUsername()
        );
        
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/{historyId}")
    @Operation(summary = "Delete post history")
    public ResponseEntity<Void> deletePostHistory(
            @PathVariable UUID historyId,
            @AuthenticationPrincipal User currentUser
    ) {
        log.info("Deleting post history: {} by user: {}", historyId, currentUser.getUsername());
        
        postHistoryService.deletePostHistory(historyId, currentUser.getUsername());
        
        return ResponseEntity.noContent().build();
    }
}
