package com.trendly.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only controller used solely to exercise {@link GlobalExceptionHandler}
 * (see {@link GlobalExceptionHandlerTest}). There are no business endpoints
 * yet as of BE-1.2 -- this class must never be referenced from main code or
 * shipped beyond the test sources.
 */
@RestController
public class ThrowawayTestController {

    @PostMapping("/test/validate")
    public void validate(@Valid @RequestBody TestRequest request) {
        // never reached when validation fails
    }

    @PostMapping("/test/boom")
    public void boom() {
        throw new IllegalStateException("boom");
    }

    record TestRequest(@NotBlank String name) {
    }
}
