-- V7__create_post_likes_table.sql
--
-- DB-2.6: `post_likes` -- pure many-to-many join table between posts and
-- users (who liked which post). Per the join-table carve-out in
-- docs/db/conventions.md ("Primary keys" / "Timestamps"), this table
-- uses a composite PRIMARY KEY over its two FK columns instead of a
-- surrogate `id`, and has no `updated_at` column/trigger: a like is an
-- immutable fact (created once, deleted once), never updated.
--
-- FK delete behavior: post_id references posts.id ON DELETE CASCADE
-- (ownership/composition -- a like has no meaning without its post);
-- user_id references users.id ON DELETE CASCADE per ADR-002 /
-- docs/db/conventions.md "Foreign key delete behavior" -- deleting a
-- user removes their likes with no exceptions for FKs to `users`.
--
-- Integrity: the composite PRIMARY KEY blocks a user liking the same
-- post twice.

CREATE TABLE post_likes (
    post_id    BIGINT NOT NULL
        CONSTRAINT fk_post_likes_post_id REFERENCES posts(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL
        CONSTRAINT fk_post_likes_user_id REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_post_likes PRIMARY KEY (post_id, user_id)
);

-- pk_post_likes(post_id, user_id) already indexes the leading column
-- (post_id, "who liked this post" / like-count lookups); user_id
-- ("posts a user has liked") still needs its own explicit index per
-- docs/db/conventions.md.
CREATE INDEX idx_post_likes_user_id ON post_likes (user_id);
