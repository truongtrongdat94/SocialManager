package com.socialmanager.controller;

import com.socialmanager.model.Platform;
import com.socialmanager.model.SocialAccount;
import com.socialmanager.repository.SocialAccountRepository;
import com.socialmanager.service.FacebookService;
import com.socialmanager.service.InsightCollectorService;
import com.socialmanager.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
public class InsightController {

    private final InsightCollectorService insightCollectorService;
    private final FacebookService facebookService;
    private final SocialAccountRepository socialAccountRepository;

    @Value("${AES_SECRET}")
    private String aesSecret;

    public InsightController(InsightCollectorService insightCollectorService,
                             FacebookService facebookService,
                             SocialAccountRepository socialAccountRepository) {
        this.insightCollectorService = insightCollectorService;
        this.facebookService = facebookService;
        this.socialAccountRepository = socialAccountRepository;
    }

    /**
     * API: collect all insights manually
     */
    @PostMapping("/collect")
    public String collectInsights() {
        insightCollectorService.collectAllAccountsInsights();
        return "Collected insights successfully!";
    }

    /**
     * API: lấy 1 post theo postId
     */
    @GetMapping("/post/{postId}")
    public Map<String, Object> getPost(@PathVariable String postId) {

        try {
            List<SocialAccount> accounts =
                    socialAccountRepository.findByPlatform(Platform.FACEBOOK);

            if (accounts.isEmpty()) {
                throw new RuntimeException("No Facebook account found");
            }

            SocialAccount account = accounts.get(0);

            String accessToken =
                    EncryptionUtil.decrypt(account.getAccessToken(), aesSecret);

            return facebookService.getPostById(postId, accessToken);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error getting post");
        }
    }
}