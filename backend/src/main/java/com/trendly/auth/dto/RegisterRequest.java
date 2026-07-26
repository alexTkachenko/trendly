package com.trendly.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /auth/register}, per
 * {@code docs/api/auth.md}. Field names map to the documented
 * {@code snake_case} JSON via the app-wide Jackson naming strategy
 * ({@code spring.jackson.property-naming-strategy: SNAKE_CASE} in
 * {@code application.yml}), so {@code displayName} <-> {@code display_name}
 * with no per-field {@code @JsonProperty} needed.
 */
public record RegisterRequest(

        @NotBlank(message = "must not be blank")
        @Email(message = "must be a valid email address")
        String email,

        @NotBlank(message = "must not be blank")
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,30}$",
                message = "must be 3-30 characters, letters/digits/underscore only")
        String username,

        @NotBlank(message = "must not be blank")
        @Size(min = 8, message = "must be at least 8 characters")
        String password,

        String displayName
) {
}
