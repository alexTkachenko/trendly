# DB-2.6 — `post_likes` table

**Epic:** E-DB-2 — Core domain schema
**Owning agent:** database-agent
**Depends on:** DB-2.1, DB-2.2
**Size:** S

## Description
Create the `post_likes` join table in `V7__create_post_likes_table.sql`.

## Scope
- Columns: `post_id`, `user_id`, `created_at`. No `id`, no `updated_at`,
  no trigger — pure join table, same shape as `follows` (see the
  carve-outs in `docs/db/conventions.md`).
- Composite `CONSTRAINT pk_post_likes PRIMARY KEY (post_id, user_id)`.
- `post_id` → `posts(id)` `ON DELETE CASCADE`
  (`fk_post_likes_post_id`); `user_id` → `users(id)` `ON DELETE CASCADE`
  (`fk_post_likes_user_id`). Both explicitly named.
- Index `user_id` (the PK already covers the leading `post_id`).

## Acceptance criteria
- [x] `V7__create_post_likes_table.sql` applies cleanly on top of V1-V6 on
      a fresh DB.
- [x] A duplicate `(post_id, user_id)` pair is rejected by
      `pk_post_likes` — one like per user per post.
- [x] Deleting a post removes its likes, and deleting a user removes
      their likes — both cascades verified with real rows.
- [x] `docs/db/schema.md` gains a `post_likes` section in the existing
      format.

## Notes for implementer
- V1-V6 are already applied — never edit them; this is a new `V7`.

## Implementation notes

`V7__create_post_likes_table.sql` mirrors `follows` (DB-2.4): composite
`PRIMARY KEY (post_id, user_id)`, both FKs named and `ON DELETE
CASCADE` (`post_id` → `posts.id` as ownership/composition, `user_id` →
`users.id` per ADR-002), no surrogate `id`, no `updated_at`. Index
`idx_post_likes_user_id` added (the PK's leading column already covers
`post_id`).

Verified locally with an isolated `postgres:16-alpine` container (not
the shared `docker-compose.yml` instance, to avoid clashing with
DB-2.3/DB-2.5's concurrent sessions) via the `flyway/flyway:10` CLI:
- V1-V7 (real files, including the now-landed `V5__create_post_media_table.sql`
  and `V6__create_comments_table.sql`) migrate cleanly on a fresh DB.
- Duplicate `(post_id, user_id)` insert rejected with `duplicate key
  value violates unique constraint "pk_post_likes"`.
- Deleting a liked post cascades its like rows away (1 → 0).
- Deleting the liking user cascades their like row away, leaving the
  (different) post untouched.

All Docker test resources (container, network) removed afterward.

`docs/db/schema.md` gained a `post_likes` section (appended after the
already-landed `post_media` section from DB-2.3).

**Status:** Ready for review
