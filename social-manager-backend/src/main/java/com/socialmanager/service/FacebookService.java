package com.socialmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class FacebookService {

    private static final Logger log = LoggerFactory.getLogger(FacebookService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL = "https://graph.facebook.com/v19.0/";

    /**
     * Lấy tất cả postId của page
     */
    public List<String> getAllPostIds(String pageId, String accessToken) {

        List<String> postIds = new ArrayList<>();

        try {
            String url = BASE_URL + pageId + "/posts?limit=10&access_token=" + accessToken;

            Map response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("data")) {
                log.warn("No posts found");
                return postIds;
            }

            List<Map<String, Object>> data =
                    (List<Map<String, Object>>) response.get("data");

            for (Map<String, Object> post : data) {
                String id = (String) post.get("id");
                postIds.add(id);
            }

            log.info("Fetched {} posts from page {}", postIds.size(), pageId);

        } catch (Exception e) {
            log.error("Error fetching posts", e);
        }

        return postIds;
    }

    /**
     * Lấy insight của post (fallback + insights)
     */
    public Map<String, Integer> getPostInsights(String postId, String accessToken) {

        Map<String, Integer> result = new HashMap<>();

        try {
            // ========================
            // 1. LẤY BASIC (LUÔN CÓ)
            // ========================
            String basicUrl = BASE_URL + postId +
                    "?fields=reactions.summary(true),comments.summary(true),shares" +
                    "&access_token=" + accessToken;

            Map basicRes = restTemplate.getForObject(basicUrl, Map.class);

            if (basicRes != null) {

                // likes = reactions
                Map reactions = (Map) basicRes.get("reactions");
                if (reactions != null && reactions.containsKey("summary")) {
                    Map summary = (Map) reactions.get("summary");
                    result.put("likes", ((Number) summary.getOrDefault("total_count", 0)).intValue());
                }

                // comments
                Map comments = (Map) basicRes.get("comments");
                if (comments != null && comments.containsKey("summary")) {
                    Map summary = (Map) comments.get("summary");
                    result.put("comments", ((Number) summary.getOrDefault("total_count", 0)).intValue());
                }

                // shares
                Map shares = (Map) basicRes.get("shares");
                if (shares != null) {
                    result.put("shares", ((Number) shares.getOrDefault("count", 0)).intValue());
                }
            }

            // ========================
            // 2. LẤY INSIGHTS (CÓ THÌ LẤY)
            // ========================
            String insightUrl = BASE_URL + postId +
                    "/insights?metric=post_impressions,post_reach,post_engaged_users" +
                    "&access_token=" + accessToken;

            Map insightRes = restTemplate.getForObject(insightUrl, Map.class);

            if (insightRes != null && insightRes.containsKey("data")) {

                List<Map<String, Object>> data =
                        (List<Map<String, Object>>) insightRes.get("data");

                for (Map<String, Object> metric : data) {

                    String name = (String) metric.get("name");

                    List<Map<String, Object>> values =
                            (List<Map<String, Object>>) metric.get("values");

                    if (values == null || values.isEmpty()) continue;

                    Object rawValue = values.get(0).get("value");

                    int value = 0;
                    if (rawValue instanceof Number) {
                        value = ((Number) rawValue).intValue();
                    }

                    result.put(name, value);
                }
            } else {
                log.warn("No insights metrics for post {}", postId);
            }

            log.info("Final insight data for {}: {}", postId, result);

        } catch (Exception e) {
            log.error("Error fetching insights for postId={}", postId, e);
        }

        return result;
    }

    public Map<String, Object> getPostById(String postId, String accessToken) {

        try {
            String url = BASE_URL + postId +
                    "?fields=id,message,created_time" +
                    "&access_token=" + accessToken;
    
            Map response = restTemplate.getForObject(url, Map.class);
    
            if (response == null) {
                log.warn("Post not found: {}", postId);
                return Collections.emptyMap();
            }
    
            log.info("Fetched post {}", postId);
            return response;
    
        } catch (Exception e) {
            log.error("Error fetching post {}", postId, e);
            return Collections.emptyMap();
        }
    }

    public Map<String, Integer> getDetailedReactions(String postId, String accessToken) {

        Map<String, Integer> result = new HashMap<>();
    
        try {
            String url = BASE_URL + postId +
                    "?fields=" +
                    "reactions.type(LIKE).summary(total_count).limit(0).as(like)," +
                    "reactions.type(LOVE).summary(total_count).limit(0).as(love)," +
                    "reactions.type(HAHA).summary(total_count).limit(0).as(haha)," +
                    "reactions.type(WOW).summary(total_count).limit(0).as(wow)," +
                    "reactions.type(SAD).summary(total_count).limit(0).as(sad)," +
                    "reactions.type(ANGRY).summary(total_count).limit(0).as(angry)," +
                    "reactions.type(CARE).summary(total_count).limit(0).as(care)" +
                    "&access_token=" + accessToken;
    
            Map response = restTemplate.getForObject(url, Map.class);
    
            String[] types = {"like", "love", "haha", "wow", "sad", "angry", "care"};
    
            for (String type : types) {
                Map data = (Map) response.get(type);
                if (data != null && data.containsKey("summary")) {
                    Map summary = (Map) data.get("summary");
                    result.put(type, ((Number) summary.getOrDefault("total_count", 0)).intValue());
                } else {
                    result.put(type, 0);
                }
            }
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return result;
    }
}