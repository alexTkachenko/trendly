package com.trendly.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "must contain only letters, numbers, and underscores")
        String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        String displayName
) {
}
