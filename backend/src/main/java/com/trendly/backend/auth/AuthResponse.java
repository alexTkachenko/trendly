package com.trendly.backend.auth;

import com.trendly.backend.user.MeResponse;

public record AuthResponse(
        String token,
        MeResponse user
) {
}
