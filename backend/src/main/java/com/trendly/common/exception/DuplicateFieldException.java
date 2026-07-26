package com.trendly.common.exception;

/**
 * Thrown when a write would violate a case-insensitive uniqueness rule
 * (currently {@code users.email} / {@code users.username}, DB-2.1's
 * {@code uq_users_email_lower}/{@code uq_users_username_lower} indexes).
 * Mapped to {@code 409 Conflict} by {@link GlobalExceptionHandler}. The
 * message names which field conflicted, per {@code docs/api/auth.md}
 * ("The `error` message says which one").
 */
public class DuplicateFieldException extends RuntimeException {

    public DuplicateFieldException(String message) {
        super(message);
    }
}
