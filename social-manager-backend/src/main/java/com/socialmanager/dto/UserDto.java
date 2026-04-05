package com.socialmanager.dto;

import java.util.UUID;

public record UserDto(UUID id, String email, String username, String name) {}
