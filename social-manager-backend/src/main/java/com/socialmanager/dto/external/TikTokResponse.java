package com.socialmanager.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TikTokResponse(
    TikTokData data
) {
    public record TikTokData(
        TikTok user
    ) {
    }

    public record TikTok(
        @JsonProperty("open_id")
        String id,

        @JsonProperty("display_name")
        String name,

        @JsonProperty("avatar_url")
        String pictureUrl
    ) {
    }
}
