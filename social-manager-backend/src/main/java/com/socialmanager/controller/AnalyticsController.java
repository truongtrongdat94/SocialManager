package com.socialmanager.controller;

import com.socialmanager.dto.ApiResponse;
import com.socialmanager.dto.external.FacebookInsight;
import com.socialmanager.service.InsightsService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Validated
public class AnalyticsController {

    private final InsightsService insightsService;

    // Whitelist metrics được phép (chỉ những metrics KHÔNG deprecated)
    private static final Set<String> ALLOWED_PAGE_METRICS = Set.of(
        // Biểu đồ 1: Reach & Impressions (bỏ page_impressions)
        "page_impressions_unique",
        "page_posts_impressions_organic",
        "page_posts_impressions_paid",
        
        // Biểu đồ 2: BỎ (quá nhiều deprecated)
        "page_daily_follows_unique",  // chỉ còn 1 metric
        
        // Biểu đồ 3: Engagement (bỏ page_consumptions_unique)
        "page_post_engagements",
        
        // Biểu đồ 4: Reactions (đầy đủ)
        "page_actions_post_reactions_like_total",
        "page_actions_post_reactions_love_total",
        "page_actions_post_reactions_wow_total",
        "page_actions_post_reactions_haha_total",
        "page_actions_post_reactions_sorry_total",
        "page_actions_post_reactions_anger_total"
    );

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * GET /api/analytics/page/{pageId}
     * Lấy Page-level insights
     * 
     * Query params:
     * - metric (required): Comma-separated metrics
     *   Examples: "page_impressions,page_engaged_users,page_fans"
     * - since (optional): Start date YYYY-MM-DD
     * - until (optional): End date YYYY-MM-DD
     * - period (optional): "day", "week", "month" (default: "day")
     */
    @GetMapping("/page/{pageId}")
    public ResponseEntity<ApiResponse<List<FacebookInsight>>> getPageInsights(
        @PathVariable String pageId,
        
        @RequestParam 
        @Pattern(regexp = "^[a-z0-9_,]+$", message = "Metric phải là lowercase, số, underscore và comma")
        String metric,
        
        @RequestParam(required = false)
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Since phải format YYYY-MM-DD")
        String since,
        
        @RequestParam(required = false)
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Until phải format YYYY-MM-DD")
        String until,
        
        @RequestParam(defaultValue = "day")
        @Pattern(regexp = "^(day|week|month)$", message = "Period phải là day, week hoặc month")
        String period
    ) {
        // Validate metrics whitelist
        List<String> requestedMetrics = Arrays.asList(metric.split(","));
        List<String> invalidMetrics = requestedMetrics.stream()
            .filter(m -> !ALLOWED_PAGE_METRICS.contains(m.trim()))
            .collect(Collectors.toList());
        
        if (!invalidMetrics.isEmpty()) {
            throw new IllegalArgumentException("Invalid metrics: " + String.join(", ", invalidMetrics));
        }
        
        // Validate date range
        if (since != null && until != null) {
            LocalDate sinceDate = LocalDate.parse(since);
            LocalDate untilDate = LocalDate.parse(until);
            
            if (sinceDate.isAfter(untilDate)) {
                throw new IllegalArgumentException("Since date phải trước until date");
            }
            
            // Facebook limit: max 93 days
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(sinceDate, untilDate);
            if (daysBetween > 93) {
                throw new IllegalArgumentException("Khoảng thời gian tối đa là 93 ngày");
            }
        }
        
        List<FacebookInsight> insights = insightsService.getPageInsights(
            getCurrentUsername(), 
            pageId, 
            metric, 
            since, 
            until, 
            period
        );
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy Page insights thành công", insights));
    }

    /**
     * GET /api/analytics/post/{postId}?pageId=xxx
     * Lấy Post-level insights
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<List<FacebookInsight>>> getPostInsights(
        @PathVariable String postId,
        @RequestParam String pageId
    ) {
        String username = getCurrentUsername();
        System.out.println("DEBUG: Getting post insights for postId=" + postId + ", pageId=" + pageId + ", user=" + username);
        
        List<FacebookInsight> insights = insightsService.getPostInsights(
            username, 
            postId, 
            pageId
        );
        
        System.out.println("DEBUG: Retrieved " + (insights != null ? insights.size() : 0) + " insights for post " + postId);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy Post insights thành công", insights));
    }
}
