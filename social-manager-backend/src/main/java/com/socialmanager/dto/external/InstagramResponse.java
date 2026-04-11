package com.socialmanager.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InstagramResponse(
    @JsonProperty("id")
    String id,

    @JsonProperty("username")
    String username,

    @JsonProperty("name")
    String name,

    @JsonProperty("profile_picture_url")
    String pictureUrl
) {
}
