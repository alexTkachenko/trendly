package com.trendly.common.exception;

/**
 * Thrown by login when the {@code login} identifier doesn't resolve to a
 * user, or the password doesn't match. Mapped to {@code 401 Unauthorized}
 * by {@link GlobalExceptionHandler} with a fixed, generic message -- per
 * {@code docs/api/auth.md}, "unknown user" and "wrong password" must be
 * indistinguishable in the response, so this exception intentionally never
 * carries a caller-supplied or field-specific message.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
