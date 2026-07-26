# API error shape

Every endpoint returns errors in this shape:

```json
{
  "error": "human-readable message",
  "status": 400,
  "path": "/some/path"
}
```

- `error` -- short, human-readable message. For Bean Validation failures it
  summarizes the failing field(s); for unmapped exceptions it is always
  the fixed string `"Internal server error"` (no exception class name or
  stack trace is ever included).
- `status` -- the HTTP status code, duplicated in the body for convenience.
- `path` -- the request URI that produced the error.

All endpoints use this shape via a single global `@ControllerAdvice`
(`com.trendly.common.exception.GlobalExceptionHandler`); no endpoint should
implement its own error response format.
