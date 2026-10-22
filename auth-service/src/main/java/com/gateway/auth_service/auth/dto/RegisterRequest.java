package com.gateway.auth_service.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(
                min = 3,
                max = 50,
                message = "Username must contain 3 to 50 characters"
        )
        @Pattern(
                regexp = "[a-z][a-z0-9._-]*",
                message = "Username must start with a lowercase letter "
                          + "and contain only lowercase letters, digits, dots, "
                          + "underscores or hypens"
        )
        String username,

        @NotBlank(message = "password is required")
        @Size(
                min = 12,
                max = 72,
                message = "Password must contain 12 to 72 characters"
        )
        String password


) {
}
