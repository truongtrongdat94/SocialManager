package com.socialmanager.mapper;

import com.socialmanager.model.PostInsight;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ChartDataMapper {

    public Map<String, List<Integer>> toChartData(List<PostInsight> insights) {

        List<Integer> likes = new ArrayList<>();
        List<Integer> impressions = new ArrayList<>();
        List<Integer> reach = new ArrayList<>();

        for (PostInsight i : insights) {
            likes.add(i.getLikes() != null ? i.getLikes() : 0);
            impressions.add(i.getImpressions() != null ? i.getImpressions() : 0);
            reach.add(i.getReach() != null ? i.getReach() : 0);
        }

        Map<String, List<Integer>> result = new HashMap<>();
        result.put("likes", likes);
        result.put("impressions", impressions);
        result.put("reach", reach);

        return result;
    }
}