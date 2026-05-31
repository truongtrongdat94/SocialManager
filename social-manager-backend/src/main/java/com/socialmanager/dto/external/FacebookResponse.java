package com.socialmanager.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FacebookResponse(
    List<Page> data
) {
    public record Page(
        @JsonProperty("id")
        String id,

        @JsonProperty("name")
        String name,

        @JsonProperty("access_token")
        String pageToken,

        @JsonProperty("picture")
        Picture picture
    ) {
        public record Picture(PictureData data) {
        }

        public record PictureData(String url) {
        }

        public String pictureUrl() {
            return (picture != null && picture.data() != null) ? picture.data().url() : null;
        }
    }
}