# BE-1.2 — Global exception handling

**Epic:** E-BE-1 — Backend project setup & security skeleton
**Owning agent:** backend-agent
**Depends on:** BE-1.1
**Size:** S

## Description
Add a `@ControllerAdvice` that turns exceptions into one consistent JSON
error shape, so every later endpoint task inherits it for free.

## Scope
- Shape: `{"error": "...", "status": 400, "path": "/some/path"}`. Keep it
  at that — no error codes catalogue, no field-level error arrays unless
  trivially free from `MethodArgumentNotValidException`.
- Map at least Bean Validation failures → 400 and a catch-all
  `Exception` → 500.
- Document the shape in `docs/api/errors.md` (create it; 5-10 lines: the
  JSON shape, plus a one-line note that all endpoints use it).

## Acceptance criteria
- [x] A request that fails validation returns 400 with the documented
      shape (demonstrated by a test or a documented manual call).
- [x] An unmapped runtime exception returns 500 in the same shape, with no
      stack trace or exception class name in the body.
- [x] `docs/api/errors.md` exists and matches what the code actually
      returns.

## Notes for implementer
- There are no business endpoints yet — a throwaway test-only controller
  (or a `@WebMvcTest` slice) is fine for demonstrating both cases.

## Implementation note
Added `com.trendly.common.exception.GlobalExceptionHandler`
(`@RestControllerAdvice`) mapping `MethodArgumentNotValidException` -> 400
and a catch-all `Exception` -> 500, both returning
`com.trendly.common.exception.ErrorResponse` (`{error, status, path}`).
The 500 branch always returns the fixed message `"Internal server error"`
(no exception class/stack trace in the body); the real exception is only
logged server-side. Verified with a `@WebMvcTest` slice
(`GlobalExceptionHandlerTest`) against a throwaway test-only controller
(`ThrowawayTestController`, test sources only). Documented the shape in
`docs/api/errors.md`. `./mvnw test` and `./mvnw package` both pass
(12 tests, BUILD SUCCESS).

**Status:** Done
