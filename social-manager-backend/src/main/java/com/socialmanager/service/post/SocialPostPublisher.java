package com.socialmanager.service.post;

import com.socialmanager.dto.SocialPostPublishRequest;
import com.socialmanager.model.Platform;

public interface SocialPostPublisher {

    boolean supports(Platform platform);

    String publish(SocialPostPublishRequest request);
}