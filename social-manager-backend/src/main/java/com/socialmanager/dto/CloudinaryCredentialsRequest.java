package com.socialmanager.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CloudinaryCredentialsRequest {
    private String cloudName;
    private String apiKey;
    private String apiSecret;
}
