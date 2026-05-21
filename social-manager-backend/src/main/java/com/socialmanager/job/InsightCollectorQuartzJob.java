package com.socialmanager.job;

import com.socialmanager.model.PostInsight;
import com.socialmanager.model.ScheduledPost;
import com.socialmanager.repository.ScheduledPostRepository;
import com.socialmanager.repository.PostInsightRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

@Component
public class InsightCollectorQuartzJob {

    private final PostInsightRepository repo;
    private final ScheduledPostRepository scheduledPostRepository;
    private final Random random = new Random();

    public InsightCollectorQuartzJob(PostInsightRepository repo, ScheduledPostRepository scheduledPostRepository) {
        this.repo = repo;
        this.scheduledPostRepository = scheduledPostRepository;
    }

    @Scheduled(fixedRate = 15000) // mỗi 15 giây
    public void collectInsights() {
        ScheduledPost scheduledPost = scheduledPostRepository.findTopByStatusOrderByCreatedAtDesc("POSTED");
        if (scheduledPost == null) {
            return;
        }

        PostInsight insight = PostInsight.builder()
                .scheduledPost(scheduledPost)
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