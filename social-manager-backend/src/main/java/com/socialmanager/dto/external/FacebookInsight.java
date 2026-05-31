package com.socialmanager.dto.external;

import java.util.List;

public record FacebookInsight(
    String name,
    String period,
    List<InsightValue> values,
    String title,
    String description,
    String id
) {}
