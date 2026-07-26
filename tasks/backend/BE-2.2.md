# BE-2.2 — `POST /auth/login` issuing a JWT

**Epic:** E-BE-2 — Auth & user management API
**Owning agent:** backend-agent
**Depends on:** BE-1.3, BE-2.1
**Size:** M

## Description
Implement login exactly per [`docs/api/auth.md`](../../docs/api/auth.md):
validate credentials against the stored bcrypt hash and return a signed
JWT.

## Scope
- `login` field accepts email **or** username, case-insensitive (reuse
  BE-2.1's repository lookups).
- Verify the password with the same `PasswordEncoder` bean; never load or
  return `password_hash` into a DTO.
- **Add a token-issuing method to BE-1.3's existing `JwtService`** —
  don't duplicate JWT signing logic elsewhere. Sign with `JWT_SECRET`
  (`app.jwt.secret`), `sub` = the user's id.
- Pick a reasonable expiry (e.g. 24h) and record it in the task's
  implementation notes — an implementation default, no ADR needed.

## Acceptance criteria
- [ ] Valid credentials return `200` with the documented body (same shape
      as register's 201), containing a usable access token.
- [ ] Wrong password and unknown email/username both return `401` with an
      identical body — nothing leaks which one was wrong.
- [ ] End-to-end test: a token issued by login is accepted by BE-1.3's
      existing `JwtService`/filter on a follow-up authenticated request.

## Notes for implementer
- **Implement BE-2.1 and BE-2.2 in one session (or a direct continuation
  of it).** They share the `User` entity, `UserRepository`, and
  `JwtService`. Do **not** split them across two parallel agent sessions
  — that is exactly how BE-1.2/BE-1.3 hit a transient file conflict. This
  is not optional parallelization.

**Status:** Backlog
