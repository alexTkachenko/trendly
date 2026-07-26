# DB-2.4 — `follows` table

**Epic:** E-DB-2 — Core domain schema
**Owning agent:** database-agent
**Depends on:** DB-2.1
**Size:** S

## Description
Create the `follows` join table in `V4__create_follows_table.sql`.

## Scope
- Columns: `follower_id`, `followee_id`, `created_at`. No `id`, no
  `updated_at`, no trigger (pure join table — see the carve-outs in
  `docs/db/conventions.md`).
- Composite `CONSTRAINT pk_follows PRIMARY KEY (follower_id, followee_id)`.
- Both FKs → `users(id)` `ON DELETE CASCADE`, explicitly named.
- `CONSTRAINT chk_follows_no_self_follow CHECK (follower_id <> followee_id)`.
- Index `followee_id` (the PK already covers `follower_id`).

## Acceptance criteria
- [x] `V4__create_follows_table.sql` applies cleanly on a fresh DB.
- [x] Self-follow insert is rejected; duplicate follow pair is rejected.
- [x] Deleting a user removes follow rows where they are either side.
- [x] `docs/db/schema.md` gains a `follows` section in DB-2.1's format.

## Notes for implementer
- No dependency on `posts` — this can land before or after DB-2.2/DB-2.3,
  as long as the version number stays sequential and unused.

## Implementation notes

`V4__create_follows_table.sql` created (composite `pk_follows` on
`(follower_id, followee_id)`, both FKs to `users(id) ON DELETE CASCADE`
named `fk_follows_follower_id`/`fk_follows_followee_id`,
`chk_follows_no_self_follow`, `idx_follows_followee_id`; no `id`, no
`updated_at`, per the join-table carve-out). Verified against a
disposable `postgres:16-alpine` container + `flyway/flyway:10` CLI:
V1→V4 (including DB-2.2's `V3__create_posts_table.sql`, which landed in
a parallel session mid-task) applied cleanly in sequence; self-follow
insert rejected by `chk_follows_no_self_follow`; duplicate
`(follower_id, followee_id)` pair rejected by `pk_follows`; deleting a
user cascades to remove follow rows on both the follower side and the
followee side. Docker container and volumes cleaned up after
verification. `docs/db/schema.md` gained a `follows` section, appended
after `users` (DB-2.2's `posts` section hadn't landed in the doc yet at
time of writing, so nothing to append after).

Sample backend queries against this table:

```sql
-- follower count for a user (uses idx_follows_followee_id)
SELECT count(*) FROM follows WHERE followee_id = $1;

-- "who does user X follow" feed fan-out source, paginated
SELECT followee_id FROM follows
WHERE follower_id = $1
ORDER BY created_at DESC
LIMIT $2 OFFSET $3;
-- uses pk_follows (follower_id is the leading column)

-- "who follows user X" (uses idx_follows_followee_id)
SELECT follower_id FROM follows
WHERE followee_id = $1
ORDER BY created_at DESC
LIMIT $2 OFFSET $3;

-- is A following B? (point lookup, uses pk_follows directly)
SELECT EXISTS (
  SELECT 1 FROM follows WHERE follower_id = $1 AND followee_id = $2
);
```

Note for backend-agent: no existing JPA entity maps this table yet
(pre-BE-1.x), so no entity-update handoff needed for this task —
whoever implements the `Follow` entity should use a composite
`@EmbeddedId`/`@IdClass` (no surrogate `id` field) to match this schema.

**Status:** Done
