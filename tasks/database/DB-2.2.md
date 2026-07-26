# DB-2.2 — `posts` table

**Epic:** E-DB-2 — Core domain schema
**Owning agent:** database-agent
**Depends on:** DB-2.1
**Size:** S

## Description
Create the `posts` table in `V3__create_posts_table.sql`.

## Scope
- Columns: `id`, `author_id`, `text_content`, `created_at`, `updated_at`.
- `author_id BIGINT NOT NULL` → `users(id)` `ON DELETE CASCADE`
  (`fk_posts_author_id`), indexed.
- `text_content` nullable at the DB level — a post may be media-only;
  "text or media required" is enforced at the app layer.
- Attach `trg_posts_set_updated_at` to the existing `set_updated_at()`
  from V2 (do not redefine the function).

Naming/typing per `docs/db/conventions.md`.

## Acceptance criteria
- [x] `V3__create_posts_table.sql` applies cleanly on a fresh DB.
- [x] Deleting a user removes their posts (cascade verified with real
      rows).
- [x] `docs/db/schema.md` gains a `posts` section in DB-2.1's format.

## Implementation notes

Verified locally against `postgres:16-alpine` via Docker (flyway/flyway
CLI, `V1`-`V3` applied cleanly on a fresh DB; `V4__create_follows_table.sql`
from the parallel DB-2.4 session also applied alongside with no conflict).
Cascade verified with real rows: inserted a user, inserted a post with
`author_id` referencing it, deleted the user, confirmed the post row was
gone. Also confirmed `trg_posts_set_updated_at` bumps `updated_at` on
`UPDATE` (reuses `set_updated_at()` from `V2`, not redefined). Re-ran
`flyway migrate` to confirm idempotency (no-op at version 4). Docker
container/volume/network torn down afterward (`docker compose down -v`).

**Status:** Done
