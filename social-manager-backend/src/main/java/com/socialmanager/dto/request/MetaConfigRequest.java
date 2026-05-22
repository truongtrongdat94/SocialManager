package com.socialmanager.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetaConfigRequest {
    private String appId;
    private String appSecret;
    private String redirectUri;
}
