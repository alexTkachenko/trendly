# DB-2.3 — `post_media` table

**Epic:** E-DB-2 — Core domain schema
**Owning agent:** database-agent
**Depends on:** DB-2.2
**Size:** S

## Description
Create the `post_media` table in `V5__create_post_media_table.sql`.

## Scope
- Columns: `id`, `post_id`, `url`, `media_type`, `position`,
  `created_at`, `updated_at`. Not a join table — full surrogate `id`
  (`pk_post_media`) and the full timestamp pair.
- `post_id BIGINT NOT NULL` → `posts(id)` `ON DELETE CASCADE`
  (`fk_post_media_post_id`), indexed.
- `url TEXT NOT NULL`; `media_type TEXT NOT NULL` +
  `chk_post_media_media_type CHECK (media_type IN ('IMAGE', 'VIDEO'))`
  (TEXT + CHECK, not a Postgres ENUM — see the Column types section of
  `docs/db/conventions.md`).
- `position INT NOT NULL` — ordering of multiple media items within one
  post.
- Attach `trg_post_media_set_updated_at` to the existing
  `set_updated_at()` from V2 (do not redefine the function).

## Acceptance criteria
- [x] `V5__create_post_media_table.sql` applies cleanly on top of V1-V4 on
      a fresh DB.
- [x] Deleting a post removes its media rows (cascade verified with real
      rows); a `media_type` outside the allowed set is rejected.
- [x] `docs/db/schema.md` gains a `post_media` section (file already
      exists — append, don't recreate).

## Notes for implementer
- V1-V4 are already applied — never edit them; this is a new `V5`.

## Implementation notes

Verified locally against `postgres:16-alpine` in a disposable Docker
container (removed after the run): `flyway migrate` applied V1-V5 (plus
V6/V7 from concurrent DB-2.5/DB-2.6 sessions, which had already landed)
cleanly against a fresh DB. Inserted a user, a post, and two
`post_media` rows referencing it; deleting the post cascaded and removed
both media rows (`2 -> 0`). Confirmed `chk_post_media_media_type`
rejects `media_type = 'GIF'` with the expected constraint-violation
error. `docs/db/schema.md` gained a `post_media` section, appended after
`follows` (re-read immediately before appending; no conflicting
concurrent edits found there at the time).

**Status:** Ready for review
