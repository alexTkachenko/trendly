# BE-2.1 — `POST /auth/register`

**Epic:** E-BE-2 — Auth & user management API
**Owning agent:** backend-agent
**Depends on:** BE-1.1, BE-1.2, BE-1.3, DB-2.1
**Size:** M

## Description
Implement account registration exactly per
[`docs/api/auth.md`](../../docs/api/auth.md) — read it and match the
request/response shapes literally.

## Scope
- `User` JPA entity + `UserRepository` over the existing `users` table
  (DB-2.1; `ddl-auto: validate`, no schema changes from the app).
  Lookups by email/username must filter on `lower(...)` per
  `docs/db/conventions.md`.
- Request DTO with Bean Validation (email format, non-blank
  username/password, lengths per the contract); response DTO that
  **never** exposes `password_hash`.
- Hash the password with Spring Security's `PasswordEncoder`
  (`BCryptPasswordEncoder` bean).
- Uniqueness is enforced by the DB indexes from DB-2.1 — catch the
  resulting conflict and surface it as a clean `409` via BE-1.2's
  `@ControllerAdvice` pattern, not a raw 500.
- The `access_token` in the 201 body comes from BE-2.2's token issuance —
  see Notes.

## Acceptance criteria
- [ ] Successful registration returns `201` with the documented body
      shape, and no password hash appears anywhere in the response.
- [ ] Duplicate email or username (case-insensitive) returns `409` in
      BE-1.2's error shape — never a 500.
- [ ] Test asserts against the DB directly that the stored
      `password_hash` is a bcrypt hash, not the plaintext password.

## Notes for implementer
- **Implement BE-2.1 and BE-2.2 in one session (or a direct continuation
  of it).** They share the `User` entity, `UserRepository`, and
  `JwtService`. Do **not** split them across two parallel agent sessions
  — that is exactly how BE-1.2/BE-1.3 hit a transient file conflict. This
  is not optional parallelization.

**Status:** Backlog
