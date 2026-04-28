package com.socialmanager.service.post;

import com.socialmanager.dto.SocialPostPublishRequest;
import com.socialmanager.model.Platform;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("local")
public class LocalSocialPostPublisher implements SocialPostPublisher {

    @Override
    public boolean supports(Platform platform) {
        return platform != null;
    }

    @Override
    public String publish(SocialPostPublishRequest request) {
        return "local-" + request.platform().toLowerCase() + "-" + UUID.randomUUID();
    }
}