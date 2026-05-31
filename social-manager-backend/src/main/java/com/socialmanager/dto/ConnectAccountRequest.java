package com.socialmanager.dto;

import com.socialmanager.model.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConnectAccountRequest(
        @NotBlank String code,
        @NotNull Platform platform
) {}