# DB-2.5 — `comments` table

**Epic:** E-DB-2 — Core domain schema
**Owning agent:** database-agent
**Depends on:** DB-2.1, DB-2.2
**Size:** S

## Description
Create the `comments` table in `V6__create_comments_table.sql`.

## Scope
- Columns: `id`, `post_id`, `author_id`, `content`, `created_at`,
  `updated_at`. Standard surrogate `id` (`pk_comments`) and full
  timestamp pair — this is a content table, not a join table.
- `post_id BIGINT NOT NULL` → `posts(id)` `ON DELETE CASCADE`
  (`fk_comments_post_id`), indexed.
- `author_id BIGINT NOT NULL` → `users(id)` `ON DELETE CASCADE`
  (`fk_comments_author_id`), indexed. Settled by
  [ADR 002](../../docs/adr/002-user-deletion-fk-behavior.md) — no
  `SET NULL`, no nullable `author_id`, don't re-open it.
- `content TEXT NOT NULL`.
- Attach `trg_comments_set_updated_at` to the existing `set_updated_at()`
  from V2 (do not redefine the function).

## Acceptance criteria
- [x] `V6__create_comments_table.sql` applies cleanly on top of V1-V5 on a
      fresh DB.
- [x] Deleting a post removes its comments, and deleting a user removes
      the comments they authored — both cascades verified with real rows.
- [x] `docs/db/schema.md` gains a `comments` section in the existing
      format.

## Notes for implementer
- V1-V5 are already applied — never edit them; this is a new `V6`.

## Implementation notes
Applied `V1`-`V7` together (parallel DB-2.3/`V5` post_media and DB-2.6/`V7`
post_likes migrations landed alongside this one, no version collision) via
the `flyway/flyway:10-alpine` Docker CLI against a `postgres:16-alpine`
container — all 7 migrations succeeded. Verified both cascades with real
rows: (1) a post with two comments (one by its own author, one by another
user) — deleting the post dropped both comments (2 → 0); (2) a comment left
by user B on user A's post — deleting user B removed B's comment while A's
post and A's own comment on it were untouched (0 orphaned comments, post
row count unaffected). Test container removed after verification. Appended
the `comments` section to `docs/db/schema.md` after re-reading the file
immediately before the edit to avoid clobbering concurrent sessions'
additions.

**Status:** Done
