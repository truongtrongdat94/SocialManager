package com.socialmanager.controller;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public-config")
public class PublicConfigController {

    @Value("${FACEBOOK_CLIENT_ID:}")
    private String facebookClientId;

    @GetMapping("/oauth")
    public Map<String, String> oauth() {
        return Collections.singletonMap("facebookClientId", facebookClientId == null ? "" : facebookClientId);
    }
}
