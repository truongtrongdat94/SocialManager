package com.socialmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank String username,
    @Size(min = 8) String password,
    @NotBlank String name
) {}
