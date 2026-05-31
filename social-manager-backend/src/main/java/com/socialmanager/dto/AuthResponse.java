package com.socialmanager.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken
) {}
