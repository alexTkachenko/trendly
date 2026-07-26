# DB-2.1 — `users` table

**Epic:** E-DB-2 — Core domain schema
**Owning agent:** database-agent
**Depends on:** DB-1.1, DB-1.2, DB-1.3
**Size:** M

## Description
Create the `users` table in `V2__create_users_table.sql`, plus the shared
`set_updated_at()` trigger function this is the first migration to need.

## Scope
- Columns: `id`, `email`, `username`, `password_hash`, `display_name`,
  `bio`, `avatar_url`, `created_at`, `updated_at`.
- `email`, `username`, `password_hash` are `NOT NULL`; `display_name`,
  `bio`, `avatar_url` nullable.
- PK `pk_users`; case-insensitive unique indexes `uq_users_email_lower` /
  `uq_users_username_lower` on `lower(...)`.
- Define `set_updated_at()` once here and attach
  `trg_users_set_updated_at`.
- Create `docs/db/schema.md` and establish its format: one `##` section
  per table listing columns/types, constraints, indexes. Nothing fancier.

Everything about types, naming, and trigger wiring is in
`docs/db/conventions.md` — follow it, don't re-derive it.

## Acceptance criteria
- [x] `V2__create_users_table.sql` applies cleanly on a fresh DB (and is a
      no-op on a second run).
- [x] Inserting `Ann@Example.com` after `ann@example.com` is rejected;
      same for `username`. `UPDATE` bumps `updated_at`.
- [x] `docs/db/schema.md` exists with a `users` section (2-3 lines of prose
      max around the column/constraint list).

## Notes for implementer
- V1 is the already-applied no-op baseline — never edit it; the trigger
  function goes in V2.

## Implementation notes

`backend/src/main/resources/db/migration/V2__create_users_table.sql`
creates the shared `set_updated_at()` trigger function and the `users`
table per `docs/db/conventions.md` (named `pk_users`, case-insensitive
`uq_users_email_lower`/`uq_users_username_lower` on `lower(...)`,
`trg_users_set_updated_at`). Verified against a throwaway
`postgres:16-alpine` container (`docker compose up -d postgres`, port
remapped to avoid a local port-5432 conflict unrelated to this repo) via
the standalone `flyway/flyway` Docker CLI, same as DB-1.2:
- `flyway migrate` applied V1 then V2 cleanly on an empty schema; a
  second `migrate` run reported "up to date, no migration necessary"
  (idempotent).
- `INSERT ... ('ann@example.com', 'ann', ...)` succeeded; a follow-up
  insert of `'Ann@Example.com'` (different username) failed with
  `duplicate key value violates unique constraint "uq_users_email_lower"`;
  a follow-up insert of username `'Ann'` (different email) failed with
  `duplicate key value violates unique constraint "uq_users_username_lower"`.
- `UPDATE users SET bio = 'hello' WHERE id = 1` bumped `updated_at` past
  `created_at` via `trg_users_set_updated_at`.

Docker resources (container, volume, network) torn down afterward via
`docker compose down -v`.

**Status:** Done
