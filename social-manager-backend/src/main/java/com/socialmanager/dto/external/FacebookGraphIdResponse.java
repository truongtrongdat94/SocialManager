package com.socialmanager.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FacebookGraphIdResponse(
    @JsonProperty("id") String id
) {
}
