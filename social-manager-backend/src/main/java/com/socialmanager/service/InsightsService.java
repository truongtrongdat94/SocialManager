package com.socialmanager.service;

import com.socialmanager.client.FacebookClient;
import com.socialmanager.dto.external.FacebookInsight;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InsightsService {

    private final FacebookClient facebookClient;
    private final FacebookTokenService tokenService;

    /**
     * Lấy Page-level insights
     * Cache 10 phút vì insights không thay đổi thường xuyên
     */
    @Cacheable(value = "pageInsights", key = "#pageId + '_' + #metric + '_' + #since + '_' + #until + '_' + #period")
    public List<FacebookInsight> getPageInsights(
        String username,
        String pageId,
        String metric,
        String since,
        String until,
        String period
    ) {
        String token = tokenService.getPageToken(username, pageId);
        return facebookClient.getPageInsights(pageId, token, metric, since, until, period);
    }

    /**
     * Lấy Post-level insights
     * Cache 10 phút
     */
    @Cacheable(value = "postInsights", key = "#postId")
    public List<FacebookInsight> getPostInsights(String username, String postId, String pageId) {
        String token = tokenService.getPageToken(username, pageId);
        return facebookClient.getPostInsights(postId, token);
    }
}
