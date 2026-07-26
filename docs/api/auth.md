# API contract — Auth

Endpoints implemented by E-BE-2 (BE-2.1, BE-2.2). Error bodies follow the
shape in [`errors.md`](errors.md).

> **Open decision:** refresh tokens are still unresolved
> (`PROJECT_PLAN.md` §6). This contract assumes a **JWT access token
> only**, which matches BE-1.3's scope. If refresh tokens are chosen
> later, a `refresh_token` field is added to the login/register responses
> — nothing here has to change to ship MVP.

## POST /auth/register

Creates an account and logs the user in.

**Request**

| Field | Type | Required | Notes |
|---|---|---|---|
| `email` | string | yes | valid email; unique, case-insensitive |
| `username` | string | yes | 3–30 chars, `[a-zA-Z0-9_]`; unique, case-insensitive |
| `password` | string | yes | min 8 chars |
| `display_name` | string | no | free text |

**Responses**

- `201 Created`

```json
{
  "access_token": "<jwt>",
  "token_type": "Bearer",
  "user": {
    "id": 1,
    "username": "ann",
    "email": "ann@example.com",
    "display_name": "Ann",
    "avatar_url": null
  }
}
```

- `400 Bad Request` — validation failure (missing/malformed field).
- `409 Conflict` — email or username already taken (case-insensitive
  match). The `error` message says which one.

## POST /auth/login

**Request**

| Field | Type | Required | Notes |
|---|---|---|---|
| `login` | string | yes | email **or** username, case-insensitive |
| `password` | string | yes | |

**Responses**

- `200 OK` — same body as register's `201`.
- `400 Bad Request` — missing field.
- `401 Unauthorized` — bad credentials. Do not distinguish "unknown user"
  from "wrong password" in the response.

## Token usage

Clients send `Authorization: Bearer <access_token>`. The token is signed
with `JWT_SECRET` and validated by the filter from BE-1.3; expiry is set
by BE-2.2.
