-- V6__create_comments_table.sql
--
-- DB-2.5: `comments` table. A content table (has its own content and
-- identity for API addressing / future FKs like a "reply-to"
-- self-reference), not a pure join table -- gets the standard surrogate
-- `id` and the full created_at/updated_at pair per docs/db/conventions.md
-- "Primary keys" / "Timestamps".
--
-- FK delete behavior (both cascade, no exceptions):
--   - comments.post_id -> posts.id ON DELETE CASCADE: ownership/
--     composition -- a comment has no meaning without its post, per
--     docs/db/conventions.md "Foreign key delete behavior".
--   - comments.author_id -> users.id ON DELETE CASCADE: settled by
--     ADR-002 -- every FK to `users` cascades so deleting a user removes
--     their entire footprint, including comments they left on other
--     users' posts. Not re-litigated here.
--
-- Reuses the shared set_updated_at() trigger function defined in
-- V2__create_users_table.sql -- not redefined here.

CREATE TABLE comments (
    id         BIGINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_comments PRIMARY KEY,
    post_id    BIGINT NOT NULL CONSTRAINT fk_comments_post_id REFERENCES posts(id) ON DELETE CASCADE,
    author_id  BIGINT NOT NULL CONSTRAINT fk_comments_author_id REFERENCES users(id) ON DELETE CASCADE,
    content    TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_post_id ON comments (post_id);
CREATE INDEX idx_comments_author_id ON comments (author_id);

CREATE TRIGGER trg_comments_set_updated_at
BEFORE UPDATE ON comments
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
