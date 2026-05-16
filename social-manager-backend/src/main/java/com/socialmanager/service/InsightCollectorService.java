package com.socialmanager.service;

import com.socialmanager.model.*;
import com.socialmanager.repository.*;
import com.socialmanager.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class InsightCollectorService {

    private final PostInsightRepository repo;
    private final FacebookService facebookService;
    private final SocialAccountRepository socialAccountRepository;

    @Value("${AES_SECRET}")
    private String aesSecret;

    public InsightCollectorService(
            PostInsightRepository repo,
            FacebookService facebookService,
            SocialAccountRepository socialAccountRepository
    ) {
        this.repo = repo;
        this.facebookService = facebookService;
        this.socialAccountRepository = socialAccountRepository;
    }

    /**
     * Collect insights for ALL Facebook pages
     */
    public void collectAllAccountsInsights() {

        System.out.println("Start collecting Facebook insights (MANUAL)...");

        List<SocialAccount> accounts =
                socialAccountRepository.findByPlatform(Platform.FACEBOOK);

        for (SocialAccount account : accounts) {
            collectInsightsForAccount(account);
        }
    }

    /**
     * Collect insight for ONE account
     */
    public void collectInsightsForAccount(SocialAccount account) {

        try {
            String pageId = account.getExternalAccountId();
            String accessToken = EncryptionUtil.decrypt(account.getAccessToken(), aesSecret);

            List<String> postIds =
                    facebookService.getAllPostIds(pageId, accessToken);

            for (String postId : postIds) {

                LocalDate today = LocalDate.now();

                if (repo.findByPostIdAndDate(postId, today).isPresent()) {
                    continue;
                }

                Map<String, Integer> reactions =
                        facebookService.getDetailedReactions(postId, accessToken);

                Map<String, Integer> fbData =
                        facebookService.getPostInsights(postId, accessToken);

                if (fbData.isEmpty()) {
                    System.out.println("Skip post: " + postId);
                    continue;
                }

                // ===== OLD METRICS (optional) =====
                int likes = fbData.getOrDefault("likes", 0);
                int comments = fbData.getOrDefault("comments", 0);
                int shares = fbData.getOrDefault("shares", 0);

                // ===== REAL INSIGHTS =====
                int reach = fbData.getOrDefault("post_reach", 0);
                int impressions = fbData.getOrDefault("post_impressions", 0);
                int engagedUsers = fbData.getOrDefault("post_engaged_users", 0);

                PostInsight insight = PostInsight.builder()
                        .postId(postId)
                        .platform("facebook")
                        .date(today)
                        .likes(likes)
                        .comments(comments)
                        .shares(shares)
                        .likeCount(reactions.getOrDefault("like", 0))
                        .loveCount(reactions.getOrDefault("love", 0))
                        .hahaCount(reactions.getOrDefault("haha", 0))
                        .wowCount(reactions.getOrDefault("wow", 0))
                        .sadCount(reactions.getOrDefault("sad", 0))
                        .angryCount(reactions.getOrDefault("angry", 0))
                        .careCount(reactions.getOrDefault("care", 0))
                        .reach(reach)
                        .impressions(impressions)
                        .engagedUsers(engagedUsers)
                        .engagementRate(BigDecimal.valueOf(engagedUsers))
                        .build();

                repo.save(insight);

                System.out.println("Saved insight: " + postId);
            }

        } catch (Exception e) {
            System.out.println("Error account: " + e.getMessage());
        }
    }
}
