package com.trendly.common.exception;

/**
 * Single JSON error shape returned by every endpoint. See
 * {@code docs/api/errors.md} for the documented contract -- keep this
 * record and that doc in sync.
 */
public record ErrorResponse(String error, int status, String path) {
}
