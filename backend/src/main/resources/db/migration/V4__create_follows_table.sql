-- V4__create_follows_table.sql
--
-- DB-2.4: `follows` -- pure many-to-many join table between users
-- (follower -> followee). Per the join-table carve-out in
-- docs/db/conventions.md ("Primary keys" / "Timestamps"), this table
-- uses a composite PRIMARY KEY over its two FK columns instead of a
-- surrogate `id`, and has no `updated_at` column/trigger: a follow edge
-- is an immutable fact (created once, deleted once), never updated.
--
-- FK delete behavior: both follower_id and followee_id reference
-- users.id ON DELETE CASCADE per ADR-002 / docs/db/conventions.md
-- "Foreign key delete behavior" -- deleting a user removes every follow
-- edge where they are either side (follower or followee), with no
-- exceptions for FKs to `users`.
--
-- Integrity: chk_follows_no_self_follow blocks a user following
-- themselves; the composite PRIMARY KEY blocks duplicate follow pairs.

CREATE TABLE follows (
    follower_id BIGINT NOT NULL
        CONSTRAINT fk_follows_follower_id REFERENCES users(id) ON DELETE CASCADE,
    followee_id BIGINT NOT NULL
        CONSTRAINT fk_follows_followee_id REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_follows PRIMARY KEY (follower_id, followee_id),
    CONSTRAINT chk_follows_no_self_follow CHECK (follower_id <> followee_id)
);

-- pk_follows(follower_id, followee_id) already indexes the leading
-- column (follower_id, "who do I follow"); followee_id ("who follows
-- me") still needs its own explicit index per docs/db/conventions.md.
CREATE INDEX idx_follows_followee_id ON follows (followee_id);
