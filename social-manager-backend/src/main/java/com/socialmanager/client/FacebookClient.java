package com.socialmanager.client;

import com.socialmanager.dto.external.*;
import com.socialmanager.dto.external.FacebookResponse.Page;
import com.socialmanager.dto.request.CreatePhotoRequest;
import com.socialmanager.dto.request.CreatePostRequest;
import com.socialmanager.exception.ExternalApiCallException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FacebookClient {
    private final RestTemplate restTemplate;

    @Value("${FACEBOOK_CLIENT_ID}")
    private String facebookClientId;

    @Value("${FACEBOOK_CLIENT_SECRET}")
    private String facebookClientSecret;

    @Value("${FACEBOOK_REDIRECT_URI}")
    private String facebookRedirectUri;

    public String getAuthUrl(String stateJwt) {
        return String.format(
            "https://www.facebook.com/v25.0/dialog/oauth" +
                "?client_id=%s" +
                "&redirect_uri=%s" +
                "&response_type=code" +
                "&scope=pages_manage_metadata,pages_manage_posts,pages_read_engagement,pages_show_list" +
                "&state=%s",
            facebookClientId,
            URLEncoder.encode(facebookRedirectUri, StandardCharsets.UTF_8),
            stateJwt
        );
    }

    private String getLongTokenUrl(TokenResponse shortRes) {
        if (shortRes == null || shortRes.accessToken() == null) {
            throw new ExternalApiCallException("Không thể lấy short token từ Facebook");
        }
        String shortToken = shortRes.accessToken();

        return "https://graph.facebook.com/v25.0/oauth/access_token" +
            "?grant_type=fb_exchange_token" +
            "&client_id=" + facebookClientId +
            "&client_secret=" + facebookClientSecret +
            "&fb_exchange_token=" + shortToken;
    }

    public TokenResponse exchangeCodeForFacebookLongToken(String code) {
        try {
            String shortTokenUrl = "https://graph.facebook.com/oauth/access_token" +
                "?client_id=" + facebookClientId +
                "&client_secret=" + facebookClientSecret +
                "&redirect_uri=" + facebookRedirectUri +
                "&code=" + code;

            TokenResponse shortRes = restTemplate.getForObject(shortTokenUrl, TokenResponse.class);
            String longTokenUrl = getLongTokenUrl(shortRes);

            TokenResponse longRes = restTemplate.getForObject(longTokenUrl, TokenResponse.class);
            if (longRes == null) {
                throw new ExternalApiCallException("Không thể lấy long token từ Facebook");
            }
            return longRes;
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi từ Facebook API: " + e.getResponseBodyAsString());
        }
    }

    public List<Page> fetchFacebookPages(String longToken) {
        try {
            String url = "https://graph.facebook.com/v25.0/me/accounts" +
                "?fields=id,name,access_token,picture" +
                "&access_token=" + longToken;
            FacebookResponse res = restTemplate.getForObject(url, FacebookResponse.class);
            return (res != null && res.data() != null) ? res.data() : List.of();
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi khi lấy danh sách Page: " + e.getResponseBodyAsString());
        }
    }

    // ==================== POSTING METHODS ====================

    /**
     * Đăng bài text/link lên Facebook Page
     * Hỗ trợ scheduling bằng cách set scheduledPublishTime (Unix timestamp)
     */
    public FacebookPostResponse createPost(String pageId, String pageToken, CreatePostRequest request) {
        try {
            String url = "https://graph.facebook.com/v25.0/" + pageId + "/feed";

            Map<String, Object> body = new HashMap<>();
            body.put("message", request.message());
            body.put("access_token", pageToken);

            if (request.link() != null && !request.link().isBlank()) {
                body.put("link", request.link());
            }

            if (request.scheduledPublishTime() != null) {
                body.put("published", false);
                body.put("scheduled_publish_time", request.scheduledPublishTime());
            }

            FacebookPostResponse response = restTemplate.postForObject(url, body, FacebookPostResponse.class);
            if (response == null) {
                throw new ExternalApiCallException("Facebook API trả về null khi đăng bài");
            }
            return response;
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi khi đăng bài lên Facebook: " + e.getResponseBodyAsString());
        }
    }

    /**
     * Đăng ảnh lên Facebook Page
     * Hỗ trợ scheduling bằng cách set scheduledPublishTime (Unix timestamp)
     */
    public FacebookPhotoResponse createPhotoPost(String pageId, String pageToken, CreatePhotoRequest request) {
        try {
            String url = "https://graph.facebook.com/v25.0/" + pageId + "/photos";

            Map<String, Object> body = new HashMap<>();
            body.put("url", request.photoUrl());
            body.put("access_token", pageToken);

            if (request.caption() != null && !request.caption().isBlank()) {
                body.put("caption", request.caption());
            }

            if (request.scheduledPublishTime() != null) {
                body.put("published", false);
                body.put("scheduled_publish_time", request.scheduledPublishTime());
            }

            FacebookPhotoResponse response = restTemplate.postForObject(url, body, FacebookPhotoResponse.class);
            if (response == null) {
                throw new ExternalApiCallException("Facebook API trả về null khi đăng ảnh");
            }
            return response;
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi khi đăng ảnh lên Facebook: " + e.getResponseBodyAsString());
        }
    }

    /**
     * Lấy danh sách bài viết của Page
     * Sử dụng endpoint 'published_posts' thay vì 'feed' để tránh yêu cầu quyền pages_read_engagement
     */
    public List<FacebookPost> getPagePosts(String pageId, String pageToken, int limit) {
        try {
            // Thử endpoint published_posts trước (không cần quyền đặc biệt)
            String url = String.format(
                "https://graph.facebook.com/v25.0/%s/published_posts?fields=id,message,created_time,full_picture,permalink_url&limit=%d&access_token=%s",
                pageId, limit, pageToken
            );

            System.out.println("DEBUG: Fetching posts from URL: " + url.replace(pageToken, "***TOKEN***"));

            ResponseEntity<FacebookFeedResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, null, FacebookFeedResponse.class
            );
            
            FacebookFeedResponse body = response.getBody();
            System.out.println("DEBUG: Response status: " + response.getStatusCode());
            System.out.println("DEBUG: Retrieved " + (body != null && body.data() != null ? body.data().size() : 0) + " posts");
            
            return (body != null && body.data() != null) ? body.data() : List.of();
        } catch (HttpClientErrorException e) {
            System.err.println("ERROR: Facebook API error - Status: " + e.getStatusCode());
            System.err.println("ERROR: Response body: " + e.getResponseBodyAsString());
            
            // Nếu vẫn lỗi, thông báo rõ ràng cho user
            if (e.getStatusCode().value() == 400 && e.getResponseBodyAsString().contains("pages_read_engagement")) {
                throw new ExternalApiCallException("Bạn cần kết nối lại Facebook Page để cấp đầy đủ quyền. Vui lòng vào trang Accounts và kết nối lại.");
            }
            
            throw new ExternalApiCallException("Lỗi khi lấy danh sách bài viết từ Facebook (Status " + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("ERROR: Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new ExternalApiCallException("Lỗi không xác định khi lấy danh sách bài viết: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách bài đã lên lịch (scheduled posts)
     */
    public List<FacebookPost> getScheduledPosts(String pageId, String pageToken) {
        try {
            String url = String.format(
                "https://graph.facebook.com/v25.0/%s/scheduled_posts?fields=id,message,created_time,full_picture,scheduled_publish_time&limit=50&access_token=%s",
                pageId, pageToken
            );

            System.out.println("DEBUG: Fetching scheduled posts from URL: " + url.replace(pageToken, "***TOKEN***"));

            ResponseEntity<FacebookFeedResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, null, FacebookFeedResponse.class
            );
            
            FacebookFeedResponse body = response.getBody();
            System.out.println("DEBUG: Scheduled posts response status: " + response.getStatusCode());
            
            return (body != null && body.data() != null) ? body.data() : List.of();
        } catch (HttpClientErrorException e) {
            System.err.println("ERROR: Facebook API error for scheduled posts - Status: " + e.getStatusCode());
            System.err.println("ERROR: Response body: " + e.getResponseBodyAsString());
            throw new ExternalApiCallException("Lỗi khi lấy danh sách bài đã lên lịch từ Facebook (Status " + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("ERROR: Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new ExternalApiCallException("Lỗi không xác định khi lấy danh sách bài đã lên lịch: " + e.getMessage());
        }
    }

    /**
     * Cập nhật nội dung bài viết
     */
    public FacebookUpdateResponse updatePost(String postId, String pageToken, String message) {
        try {
            String url = "https://graph.facebook.com/v25.0/" + postId;

            Map<String, Object> body = new HashMap<>();
            body.put("message", message);
            body.put("access_token", pageToken);

            FacebookUpdateResponse response = restTemplate.postForObject(url, body, FacebookUpdateResponse.class);
            if (response == null) {
                // Facebook trả về { success: true } khi update thành công
                return new FacebookUpdateResponse(true);
            }
            return response;
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi khi cập nhật bài viết: " + e.getResponseBodyAsString());
        }
    }

    /**
     * Xóa bài viết
     */
    public FacebookDeleteResponse deletePost(String postId, String pageToken) {
        try {
            String url = "https://graph.facebook.com/v25.0/" + postId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + pageToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            return new FacebookDeleteResponse(true);
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi khi xóa bài viết: " + e.getResponseBodyAsString());
        }
    }

    // ==================== INSIGHTS METHODS ====================

    /**
     * Lấy Page-level insights
     * @param pageId Facebook Page ID
     * @param pageToken Page access token
     * @param metric Comma-separated metrics (e.g., "page_impressions,page_engaged_users")
     * @param since Start date (YYYY-MM-DD) - optional
     * @param until End date (YYYY-MM-DD) - optional
     * @param period Aggregation period: "day", "week", "month" - default "day"
     */
    public List<FacebookInsight> getPageInsights(
        String pageId, 
        String pageToken, 
        String metric, 
        String since, 
        String until, 
        String period
    ) {
        try {
            StringBuilder urlBuilder = new StringBuilder("https://graph.facebook.com/v25.0/")
                .append(pageId)
                .append("/insights?metric=")
                .append(metric)
                .append("&period=")
                .append(period != null ? period : "day");

            if (since != null && !since.isBlank()) {
                urlBuilder.append("&since=").append(since);
            }
            if (until != null && !until.isBlank()) {
                urlBuilder.append("&until=").append(until);
            }

            String url = urlBuilder.toString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + pageToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<FacebookInsightsResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                FacebookInsightsResponse.class
            );

            FacebookInsightsResponse body = response.getBody();
            return (body != null && body.data() != null) ? body.data() : List.of();
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi khi lấy Page insights: " + e.getResponseBodyAsString());
        }
    }

    /**
     * Lấy Post-level insights
     * @param postId Facebook Post ID (format: {page_id}_{post_id})
     * @param pageToken Page access token
     * 
     * CHÚ Ý: Chỉ request 3 metrics KHÔNG deprecated (tested 22/05/2026):
     * - post_impressions_unique ✅
     * - post_clicks ✅
     * - post_reactions_by_type_total ✅
     * 
     * Các metrics ĐÃ DEPRECATED (không dùng):
     * - post_impressions ❌
     * - post_impressions_paid ❌
     * - post_impressions_organic ❌
     * - post_engaged_users ❌
     */
    public List<FacebookInsight> getPostInsights(String postId, String pageToken) {
        try {
            String url = "https://graph.facebook.com/v25.0/" + postId + "/insights" +
                "?metric=post_impressions_unique,post_clicks,post_reactions_by_type_total";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + pageToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<FacebookInsightsResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                FacebookInsightsResponse.class
            );

            FacebookInsightsResponse body = response.getBody();
            return (body != null && body.data() != null) ? body.data() : List.of();
        } catch (HttpClientErrorException e) {
            throw new ExternalApiCallException("Lỗi khi lấy Post insights: " + e.getResponseBodyAsString());
        }
    }
}

