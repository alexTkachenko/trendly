-- V3__create_posts_table.sql
--
-- DB-2.2: `posts` table.
--
-- FK delete behavior: posts.author_id -> users.id ON DELETE CASCADE, per
-- ADR-002 / docs/db/conventions.md "Foreign key delete behavior" -- every
-- FK to `users` representing authorship cascades so deleting a user
-- removes their entire footprint, including their posts.
--
-- text_content is nullable at the DB level -- a post may be media-only
-- (post_media lands in a later E-DB-2 task); "text or media required" is
-- an application-layer invariant, not enforceable here without the
-- post_media table existing yet.
--
-- Reuses the shared set_updated_at() trigger function defined in
-- V2__create_users_table.sql -- not redefined here.

CREATE TABLE posts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_posts PRIMARY KEY,
    author_id    BIGINT NOT NULL CONSTRAINT fk_posts_author_id REFERENCES users(id) ON DELETE CASCADE,
    text_content TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_posts_author_id ON posts (author_id);

CREATE TRIGGER trg_posts_set_updated_at
BEFORE UPDATE ON posts
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
