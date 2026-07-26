package com.trendly.security.support;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only controller used by {@code JwtAuthenticationFilterTest} to
 * exercise the filter chain end-to-end (BE-1.3). No production route maps
 * here -- login/register and any real protected endpoints are E-BE-2+.
 */
@RestController
public class ProtectedTestController {

    @GetMapping("/test/protected")
    public String protectedEndpoint() {
        return "ok";
    }
}
