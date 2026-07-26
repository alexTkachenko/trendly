package com.trendly.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /auth/login}, per {@code docs/api/auth.md}.
 * {@code login} accepts either the account's email or its username,
 * case-insensitive -- resolved by {@code UserRepository
 * #findByEmailOrUsernameIgnoreCase}.
 */
public record LoginRequest(

        @NotBlank(message = "must not be blank")
        String login,

        @NotBlank(message = "must not be blank")
        String password
) {
}
