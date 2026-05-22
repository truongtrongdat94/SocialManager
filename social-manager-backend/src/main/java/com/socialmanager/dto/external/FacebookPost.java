package com.socialmanager.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FacebookPost(
    String id,
    String message,
    
    @JsonProperty("created_time")
    String createdTime,
    
    @JsonProperty("scheduled_publish_time")
    Long scheduledPublishTime,
    
    @JsonProperty("full_picture")
    String fullPicture,
    
    @JsonProperty("permalink_url")
    String permalinkUrl
) {}
