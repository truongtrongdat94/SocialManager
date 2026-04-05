package com.socialmanager.job;

import com.socialmanager.model.PostInsight;
import com.socialmanager.repository.PostInsightRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

@Component
public class InsightCollectorQuartzJob {

    private final PostInsightRepository repo;
    private final Random random = new Random();

    public InsightCollectorQuartzJob(PostInsightRepository repo) {
        this.repo = repo;
    }

    @Scheduled(fixedRate = 15000) // mỗi 15 giây
    public void collectInsights() {

        PostInsight insight = PostInsight.builder()
                .scheduledPost(null)
                .platform("facebook")
                .date(LocalDate.now())
                .likes(random.nextInt(100))
                .comments(random.nextInt(50))
                .shares(random.nextInt(30))
                .impressions(random.nextInt(1000))
                .reach(random.nextInt(800))
                .engagementRate(BigDecimal.valueOf(random.nextDouble()))
                .build();

        repo.save(insight);

        System.out.println("Saved insight!");
    }
}