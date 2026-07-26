# ADR 002 — Account deletion: cascade every FK referencing `users`

**Status:** Accepted
**Date:** 2026-07-25
**Deciders:** User (product owner), architect-agent
**Affects:** `posts`, `comments`, `follows`, `post_likes` (DB-2.2, DB-2.4,
DB-2.5, DB-2.6), `docs/db/conventions.md` "Foreign key delete behavior"
(DB-1.3), and E-BE-2's account-deletion behavior.

## Context

`docs/PROJECT_PLAN.md`'s DB-2.5 row states its acceptance criteria as
"Cascade delete with post; **FK to users restrict/cascade decided in ADR**"
— i.e. the delete behavior of `comments.author_id → users.id` was always
intended to be settled by an ADR, not chosen by the implementing agent.

During DB-1.3, database-agent drafted the "Foreign key delete behavior"
section of `docs/db/conventions.md` with a blanket `ON DELETE CASCADE`
stance covering both ownership/composition FKs *and* every FK to `users`,
and recorded in the DB-1.3 task notes that "no separate ADR was warranted."
reviewer-agent raised this as a blocking finding: cascading every FK to
`users` is a product/data-loss decision (it defines what "delete my
account" does to content other users can see), it has direct consequences
for E-BE-2, and DB-2.5 explicitly expected an ADR. The technical content of
the draft was sound; what was missing was the authority to make the call.

Two options were put to the user:

1. **Cascade everything** — every FK to `users.id` is `ON DELETE CASCADE`.
   - Deleting an account removes that user's posts, comments, follow edges,
     and likes entirely, atomically, in one operation.
   - Simple: no orphaned rows, no nullable attribution columns, no
     `RESTRICT` errors blocking the account-deletion endpoint, no
     app-layer branch for "author is missing."
   - Downside: a deleted user's comments vanish from *other people's*
     threads. Replies that only make sense in the context of the deleted
     comment are left dangling, and conversation history on posts the
     deleted user did not own is silently altered.

2. **Restrict-on-comments (`SET NULL`/`RESTRICT` for `comments.author_id`)**
   — comments survive account deletion and render as "[deleted user]".
   - Preserves conversation continuity on other users' posts; replies keep
     the context they were written against.
   - Downside: `comments.author_id` must become nullable, which contradicts
     it being a required attribution field, and every read path
     (repository, DTO mapper, API contract, frontend rendering) needs an
     explicit null-author case. `RESTRICT` instead of `SET NULL` would go
     further and block account deletion outright until comments are dealt
     with, which is worse for the delete-my-account flow.

This is v1/MVP scope; there is no moderation, audit, or legal-retention
requirement today that depends on preserving a deleted user's content.

## Decision

**Every foreign key referencing `users.id` uses `ON DELETE CASCADE`, with
no exceptions.**

Specifically:

- `posts.author_id → users.id ON DELETE CASCADE`
- `comments.author_id → users.id ON DELETE CASCADE`
- `follows.follower_id → users.id ON DELETE CASCADE`
- `follows.followee_id → users.id ON DELETE CASCADE`
- `post_likes.user_id → users.id ON DELETE CASCADE`

No nullable "orphaned content" columns are introduced: attribution columns
stay `NOT NULL`. There is no `SET NULL` and no `RESTRICT` on any FK to
`users`, and no soft-delete or anonymization step in front of account
deletion.

The user chose option 1, explicitly accepting that deleting an account can
silently remove reply context visible to other users, in exchange for a
simple and predictable delete path for the MVP.

**Scope note:** this ADR governs FKs *to `users`* only. It does not change
the already-decided cascade behavior of pure ownership/composition FKs that
have nothing to do with user deletion — e.g. `post_media.post_id → posts.id`
and `comments.post_id → posts.id` cascade because deleting a post should
delete its media and its comments, and that rationale stands independently
of this decision.

## Consequences

### Positive
- Account deletion is a single `DELETE FROM users WHERE id = ?`; the
  database guarantees no orphaned posts, comments, follow edges, or likes.
- No nullable attribution columns, so entities, DTOs, and frontend types
  can treat `author` as always present — no null-author rendering path.
- The account-deletion endpoint can never fail with an FK violation, and
  needs no pre-deletion cleanup pass over child tables.
- Unblocks DB-1.3's re-review and removes the ambiguity from DB-2.5's
  acceptance criteria: `comments.author_id` is cascade, on the record.

### Negative / accepted risks
- **Account deletion is destructive and total.** A user's posts, their
  comments *including those left on other users' posts*, their follows, and
  their likes all disappear at once. There is no "[deleted user]"
  placeholder and no tombstone row.
- Other users' threads can lose context without those users doing anything:
  replies to a deleted user's comment remain but the comment they answer is
  gone. Like and comment counts on unrelated posts can drop. This is the
  explicitly accepted tradeoff, not an oversight.
- Deletion is unrecoverable at the application layer — restoring a deleted
  account's content would require a database backup restore.
- E-BE-2 must therefore treat account deletion as a high-consequence
  operation: require re-authentication or explicit confirmation, and state
  plainly in the UI/API docs that all content is permanently removed.

### If this needs to change later
Reversing this is a **breaking schema change**, not a config tweak. If the
product later wants deleted users' comments preserved as "[deleted user]"
for conversation continuity, it requires:

- making `comments.author_id` nullable and dropping/recreating the FK with
  `ON DELETE SET NULL` (a new Flyway migration — existing applied
  migrations are never edited);
- app-layer handling for a null author across repository queries, DTO
  mapping, API contracts under `docs/api/`, and frontend rendering;
- a decision about already-deleted accounts, whose comments will be gone
  by then and cannot be recovered from the schema change.

That cost is understood and accepted at v1.

### Follow-ups
- `docs/db/conventions.md` "Foreign key delete behavior" cross-references
  this ADR as the authority for the FK-to-`users` stance.
- `docs/PROJECT_PLAN.md`'s DB-2.5 row points at this ADR instead of
  "decided in ADR".
- DB-2.2, DB-2.4, DB-2.5, DB-2.6 migrations declare `ON DELETE CASCADE` on
  their FKs to `users` and reference this ADR in the migration header.
- E-BE-2's account-deletion task must document the destructive semantics in
  its API contract.
