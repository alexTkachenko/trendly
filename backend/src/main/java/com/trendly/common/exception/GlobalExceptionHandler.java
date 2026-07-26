package com.trendly.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns exceptions raised anywhere in the controller layer into the one
 * consistent JSON error shape documented in {@code docs/api/errors.md}:
 * {@code {"error": "...", "status": ..., "path": "..."}}.
 *
 * Every later endpoint task inherits this for free -- do not add
 * per-controller exception handling, extend this class instead.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean Validation failures on {@code @Valid} request bodies -> 400.
     * The message is a compact summary of the field errors; no field-level
     * error array per BE-1.2's scope (keep the shape minimal).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message, HttpStatus.BAD_REQUEST.value(), request.getRequestURI()));
    }

    /**
     * Duplicate email/username on register (BE-2.1) -> 409. Message names
     * which field conflicted (set by the throwing service code), per
     * {@code docs/api/auth.md}.
     */
    @ExceptionHandler(DuplicateFieldException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateField(
            DuplicateFieldException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value(), request.getRequestURI()));
    }

    /**
     * Bad login credentials (BE-2.2) -> 401. Always the same fixed message
     * regardless of whether the identifier was unknown or the password was
     * wrong -- see {@link InvalidCredentialsException}.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED.value(), request.getRequestURI()));
    }

    /**
     * Catch-all fallback for anything not explicitly mapped -> 500. The
     * response body never leaks the exception class name or stack trace;
     * the real cause is only logged server-side.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnmapped(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception while processing {}", request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "Internal server error",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        request.getRequestURI()));
    }
}
