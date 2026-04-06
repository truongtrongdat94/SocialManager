package com.socialmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank String username,
    @NotBlank @jakarta.validation.constraints.Email String email,
    @Size(min = 8) String password,
    @NotBlank String name
) {}
