# Schema

Per-table reference for the applied schema. One `##` section per table:
columns/types, constraints, indexes, FK delete behavior where relevant.
Naming/typing rules live in `docs/db/conventions.md` — this doc just
records what each table currently looks like. Updated by each `E-DB-2+`
migration as tables land.

## users

Introduced in `V2__create_users_table.sql` (DB-2.1). `email`/`username`
uniqueness is case-insensitive (indexes on `lower(...)`, not the raw
column) — any lookup must filter on `lower(email) = lower(?)` /
`lower(username) = lower(?)` to match and to use the index.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `id` | `BIGINT` (identity) | no | `pk_users` |
| `email` | `TEXT` | no | |
| `username` | `TEXT` | no | |
| `password_hash` | `TEXT` | no | |
| `display_name` | `TEXT` | yes | |
| `bio` | `TEXT` | yes | |
| `avatar_url` | `TEXT` | yes | |
| `created_at` | `TIMESTAMPTZ` | no | default `now()` |
| `updated_at` | `TIMESTAMPTZ` | no | default `now()`; bumped by `trg_users_set_updated_at` |

Constraints/indexes:
- `pk_users` — primary key on `id`.
- `uq_users_email_lower` — unique index on `lower(email)`.
- `uq_users_username_lower` — unique index on `lower(username)`.

Trigger:
- `trg_users_set_updated_at` (`BEFORE UPDATE`) calls the shared
  `set_updated_at()` function, defined in this same migration, which
  every later table with `updated_at` reuses.

## posts

Introduced in `V3__create_posts_table.sql` (DB-2.2). `text_content` is
nullable at the DB level — a post may be media-only once `post_media`
lands in a later E-DB-2 task; "text or media required" is an
application-layer invariant only.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `id` | `BIGINT` (identity) | no | `pk_posts` |
| `author_id` | `BIGINT` | no | FK to `users(id)`, `ON DELETE CASCADE` |
| `text_content` | `TEXT` | yes | post may be media-only |
| `created_at` | `TIMESTAMPTZ` | no | default `now()` |
| `updated_at` | `TIMESTAMPTZ` | no | default `now()`; bumped by `trg_posts_set_updated_at` |

Constraints/indexes:
- `pk_posts` — primary key on `id`.
- `fk_posts_author_id` — FK on `author_id` → `users(id)`, `ON DELETE
  CASCADE` (deleting a user removes their posts; verified with real rows —
  insert a user, insert a post referencing it, delete the user, confirm
  the post row is gone).
- `idx_posts_author_id` — non-unique index on `author_id`, both for FK
  lookup performance and as the base for future feed-query composite
  indexes (`E-DB-3`).

Trigger:
- `trg_posts_set_updated_at` (`BEFORE UPDATE`) reuses the shared
  `set_updated_at()` function from `V2__create_users_table.sql`.

## follows

Introduced in `V4__create_follows_table.sql` (DB-2.4). Pure many-to-many
join table between users (follower → followee) — per the join-table
carve-out in `docs/db/conventions.md`, it uses a composite `PRIMARY KEY`
over its two FK columns instead of a surrogate `id`, and has no
`updated_at` column/trigger (a follow edge is created once and deleted
once, never updated).

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `follower_id` | `BIGINT` | no | FK → `users(id)`, part of `pk_follows` |
| `followee_id` | `BIGINT` | no | FK → `users(id)`, part of `pk_follows` |
| `created_at` | `TIMESTAMPTZ` | no | default `now()` |

Constraints/indexes:
- `pk_follows` — composite primary key on `(follower_id, followee_id)`;
  also doubles as the "no duplicate follow pair" constraint and as an
  index on the leading column `follower_id`.
- `fk_follows_follower_id` — FK to `users(id)`, `ON DELETE CASCADE`.
- `fk_follows_followee_id` — FK to `users(id)`, `ON DELETE CASCADE`.
- `chk_follows_no_self_follow` — `CHECK (follower_id <> followee_id)`,
  blocks a user following themselves.
- `idx_follows_followee_id` — non-unique index on `followee_id` (the
  "who follows me" lookup; `pk_follows` only covers `follower_id` as its
  leading column).

FK delete behavior: both FKs cascade per ADR-002 — deleting a user
removes every follow edge where they are either side. Verified locally
(Postgres 16.14, Docker): self-follow insert rejected by
`chk_follows_no_self_follow`, duplicate pair rejected by `pk_follows`,
and deleting a user removes rows where they're the follower and rows
where they're the followee.

## comments

Introduced in `V6__create_comments_table.sql` (DB-2.5). A content table
(not a pure join table) — has its own content and identity for API
addressing / future FKs like a "reply-to" self-reference, so it gets the
standard surrogate `id` and the full `created_at`/`updated_at` pair, per
`docs/db/conventions.md` "Primary keys" / "Timestamps".

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `id` | `BIGINT` (identity) | no | `pk_comments` |
| `post_id` | `BIGINT` | no | FK → `posts(id)`, `ON DELETE CASCADE` |
| `author_id` | `BIGINT` | no | FK → `users(id)`, `ON DELETE CASCADE` |
| `content` | `TEXT` | no | |
| `created_at` | `TIMESTAMPTZ` | no | default `now()` |
| `updated_at` | `TIMESTAMPTZ` | no | default `now()`; bumped by `trg_comments_set_updated_at` |

Constraints/indexes:
- `pk_comments` — primary key on `id`.
- `fk_comments_post_id` — FK on `post_id` → `posts(id)`, `ON DELETE
  CASCADE` (ownership/composition — a comment has no meaning without its
  post; deleting a post removes its comments).
- `fk_comments_author_id` — FK on `author_id` → `users(id)`, `ON DELETE
  CASCADE` per ADR-002 (deleting a user removes their comments,
  including comments they left on other users' posts).
- `idx_comments_post_id` — non-unique index on `post_id`.
- `idx_comments_author_id` — non-unique index on `author_id`.

Trigger:
- `trg_comments_set_updated_at` (`BEFORE UPDATE`) reuses the shared
  `set_updated_at()` function from `V2__create_users_table.sql`.

FK delete behavior: verified locally (Postgres 16.14, Docker) with real
rows — deleting a post removed both comments on it (2 → 0); separately,
with a comment left by one user on a different user's post, deleting the
commenting user removed their comment while the post itself and the
other user's own comment remained untouched (0 orphaned comments after
the user delete, post row count unaffected).

## post_media

Introduced in `V5__create_post_media_table.sql` (DB-2.3). Each row is one
media item (image/video) attached to a post; not a join table, so it
gets the standard surrogate `id` and the full `created_at`/`updated_at`
pair. `position` orders multiple media items within one post.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `id` | `BIGINT` (identity) | no | `pk_post_media` |
| `post_id` | `BIGINT` | no | FK to `posts(id)`, `ON DELETE CASCADE` |
| `url` | `TEXT` | no | |
| `media_type` | `TEXT` | no | `chk_post_media_media_type CHECK (media_type IN ('IMAGE', 'VIDEO'))` |
| `position` | `INT` | no | ordering within a post |
| `created_at` | `TIMESTAMPTZ` | no | default `now()` |
| `updated_at` | `TIMESTAMPTZ` | no | default `now()`; bumped by `trg_post_media_set_updated_at` |

Constraints/indexes:
- `pk_post_media` — primary key on `id`.
- `fk_post_media_post_id` — FK on `post_id` → `posts(id)`, `ON DELETE
  CASCADE` (ownership/composition: media has no meaning without its
  parent post). Verified with real rows — insert a post, insert two
  media rows referencing it, delete the post, confirm both media rows
  are gone.
- `chk_post_media_media_type` — `CHECK (media_type IN ('IMAGE',
  'VIDEO'))`; verified an out-of-set value (`'GIF'`) is rejected.
- `idx_post_media_post_id` — non-unique index on `post_id`, for FK
  lookup performance (e.g. fetching all media for a post).

Trigger:
- `trg_post_media_set_updated_at` (`BEFORE UPDATE`) reuses the shared
  `set_updated_at()` function from `V2__create_users_table.sql`.

## post_likes

Introduced in `V7__create_post_likes_table.sql` (DB-2.6). Pure
many-to-many join table between posts and users (who liked which post)
— per the join-table carve-out in `docs/db/conventions.md`, it uses a
composite `PRIMARY KEY` over its two FK columns instead of a surrogate
`id`, and has no `updated_at` column/trigger (a like is created once
and deleted once, never updated).

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `post_id` | `BIGINT` | no | FK → `posts(id)`, part of `pk_post_likes` |
| `user_id` | `BIGINT` | no | FK → `users(id)`, part of `pk_post_likes` |
| `created_at` | `TIMESTAMPTZ` | no | default `now()` |

Constraints/indexes:
- `pk_post_likes` — composite primary key on `(post_id, user_id)`; also
  doubles as the "no duplicate like pair" constraint and as an index on
  the leading column `post_id`.
- `fk_post_likes_post_id` — FK to `posts(id)`, `ON DELETE CASCADE`
  (ownership/composition: a like has no meaning without its post).
- `fk_post_likes_user_id` — FK to `users(id)`, `ON DELETE CASCADE` per
  ADR-002.
- `idx_post_likes_user_id` — non-unique index on `user_id` (the "posts
  a user has liked" lookup; `pk_post_likes` only covers `post_id` as its
  leading column).

FK delete behavior: verified locally (Postgres 16.14, Docker) with real
rows — a duplicate `(post_id, user_id)` insert was rejected by
`pk_post_likes`; deleting a liked post removed its like rows (1 → 0);
separately, deleting the liking user removed their like row while
leaving the (different) post untouched.
