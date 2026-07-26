# BE-1.3 — Spring Security JWT skeleton

**Epic:** E-BE-1 — Backend project setup & security skeleton
**Owning agent:** backend-agent
**Depends on:** BE-1.1
**Size:** M

## Description
Wire the stateless JWT filter chain: validate a bearer token if one is
present and populate the security context; reject unauthenticated access
to protected routes.

## Scope
- Stateless session policy, CSRF off (BE-1.1 already set this — extend,
  don't rewrite).
- A `OncePerRequestFilter` that parses `Authorization: Bearer <token>`,
  verifies the signature with `JWT_SECRET` (already bound to
  `app.jwt.secret` in BE-1.1) and expiry, and sets the `Authentication`.
  Invalid/expired token → no authentication set (let the chain 401), not
  a 500.
- Keep `/actuator/health` permitted; everything else authenticated.
- **Out of scope:** login/register, token *issuance*, refresh tokens,
  user lookup from DB — that's E-BE-2. Token subject handling can be a
  simple username claim for now.

## Acceptance criteria
- [x] An unauthenticated request to a protected route returns 401 (add a
      trivial protected endpoint or a security-slice test if none exists).
- [x] `GET /actuator/health` still returns 200 unauthenticated.
- [x] A request with a token signed by the wrong secret, or expired, is
      rejected as unauthenticated — i.e. `JWT_SECRET` is genuinely used to
      verify signatures, not just read.

## Implementation notes

Added `JwtService` (verifies signature/expiry with `app.jwt.secret`,
returns the `sub` claim, never throws) and `JwtAuthenticationFilter`
(`OncePerRequestFilter`, sets an unauthenticated-authority
`Authentication` on a valid token, otherwise leaves the request
untouched so the chain 401s on protected routes). The filter is wired
as a `@Bean` inside `SecurityConfig` (not `@Component`-scanned) so it
and its `JwtService` dependency don't leak into unrelated
`@WebMvcTest` slices via classpath scanning — this was needed to avoid
breaking BE-1.2's `GlobalExceptionHandlerTest`, which loads a slice
without `JwtService` in scope. JJWT (`io.jsonwebtoken`, 0.12.6) added
as the JWT library. Covered by `JwtServiceTest` (unit) and
`JwtAuthenticationFilterTest` (`@WebMvcTest` slice against a
test-only `ProtectedTestController`, no production endpoint added).
Manually verified via `docker compose up`: `/actuator/health` → 200
unauthenticated, an arbitrary other route → 401 unauthenticated.

**Status:** Done
