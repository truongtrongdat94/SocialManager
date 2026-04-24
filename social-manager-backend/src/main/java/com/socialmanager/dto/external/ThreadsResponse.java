package com.socialmanager.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ThreadsResponse(
    @JsonProperty("id")
    String id,

    @JsonProperty("username")
    String username,

    @JsonProperty("name")
    String name,

    @JsonProperty("threads_profile_picture_url")
    String pictureUrl
) {
}
