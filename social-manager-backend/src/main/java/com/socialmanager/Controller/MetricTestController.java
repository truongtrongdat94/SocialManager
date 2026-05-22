package com.socialmanager.controller;

import com.socialmanager.model.Platform;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class MetricTestController {

    private final SocialAccountRepository socialAccountRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${AES_SECRET}")
    private String aesSecret;

    private static final List<String> POST_METRICS_TO_TEST = List.of(
        "post_impressions",
        "post_impressions_unique",
        "post_impressions_paid",
        "post_impressions_organic",
        "post_engaged_users",
        "post_clicks",
        "post_reactions_by_type_total"
    );

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * GET /api/test/post-metrics?postId=xxx&pageId=xxx
     * Test từng metric một để xem metric nào bị lỗi
     */
    @GetMapping("/post-metrics")
    public ResponseEntity<Map<String, Object>> testPostMetrics(
        @RequestParam String postId,
        @RequestParam String pageId
    ) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> successMetrics = new ArrayList<>();
        List<Map<String, String>> failedMetrics = new ArrayList<>();

        try {
            // Lấy token từ pageId
            List<SocialAccount> accounts = socialAccountRepository.findAll().stream()
                .filter(a -> a.getPlatform() == Platform.FACEBOOK && 
                           a.getExternalAccountId().equals(pageId))
                .toList();
            
            if (accounts.isEmpty()) {
                result.put("success", false);
                result.put("error", "Page not found in database");
                return ResponseEntity.ok(result);
            }

            SocialAccount account = accounts.get(0);
            String pageToken = EncryptionUtil.decrypt(account.getAccessToken(), aesSecret);

            // Test từng metric
            for (String metric : POST_METRICS_TO_TEST) {
                try {
                    String url = String.format(
                        "https://graph.facebook.com/v25.0/%s/insights?metric=%s",
                        postId, metric
                    );

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + pageToken);
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                    );

                    Map<String, String> success = new HashMap<>();
                    success.put("metric", metric);
                    success.put("status", "OK");
                    successMetrics.add(success);

                } catch (HttpClientErrorException e) {
                    Map<String, String> failed = new HashMap<>();
                    failed.put("metric", metric);
                    failed.put("status", "FAILED");
                    failed.put("error", e.getResponseBodyAsString());
                    failedMetrics.add(failed);
                }
            }

            result.put("success", true);
            result.put("postId", postId);
            result.put("pageId", pageId);
            result.put("successCount", successMetrics.size());
            result.put("failedCount", failedMetrics.size());
            result.put("successMetrics", successMetrics);
            result.put("failedMetrics", failedMetrics);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.ok(result);
    }
}
