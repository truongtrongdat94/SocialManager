package com.socialmanager.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InsightValue(
    Object value,
    
    @JsonProperty("end_time")
    String endTime
) {}
