package com.socialmanager.service;

import com.socialmanager.model.Platform;

public interface SocialPostPublisher {

    boolean supports(Platform platform);

    String publish(SocialPostPublishRequest request);
}
