package com.socialmanager.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FacebookPhotoResponse(
    String id,
    
    @JsonProperty("post_id")
    String postId
) {}
