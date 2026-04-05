package com.socialmanager.controller;

import com.socialmanager.mapper.ChartDataMapper;
import com.socialmanager.model.PostInsight;
import com.socialmanager.repository.PostInsightRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final PostInsightRepository repo;
    private final ChartDataMapper mapper;

    public AnalyticsController(PostInsightRepository repo, ChartDataMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @GetMapping
    public Map<String, List<Integer>> getAnalytics() {
        List<PostInsight> data = repo.findAll();
        return mapper.toChartData(data);
    }
}