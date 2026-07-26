-- V5__create_post_media_table.sql
--
-- DB-2.3: `post_media` table. Not a join table -- each row is a media
-- item (image/video) attached to a post, ordered by `position`, so it
-- gets the standard surrogate `id` and the full created_at/updated_at
-- pair (unlike follows/post_likes).
--
-- FK delete behavior: post_media.post_id -> posts.id ON DELETE CASCADE,
-- per docs/db/conventions.md "Foreign key delete behavior" -- ownership/
-- composition relationship: media rows have no meaning without their
-- parent post, so deleting a post removes its media.
--
-- media_type is TEXT + CHECK, not a Postgres ENUM, per docs/db/
-- conventions.md "Column types" -- simpler, fully transactional to
-- extend later via DROP/ADD CONSTRAINT.
--
-- Reuses the shared set_updated_at() trigger function defined in
-- V2__create_users_table.sql -- not redefined here.

CREATE TABLE post_media (
    id         BIGINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_post_media PRIMARY KEY,
    post_id    BIGINT NOT NULL CONSTRAINT fk_post_media_post_id REFERENCES posts(id) ON DELETE CASCADE,
    url        TEXT NOT NULL,
    media_type TEXT NOT NULL CONSTRAINT chk_post_media_media_type CHECK (media_type IN ('IMAGE', 'VIDEO')),
    position   INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_post_media_post_id ON post_media (post_id);

CREATE TRIGGER trg_post_media_set_updated_at
BEFORE UPDATE ON post_media
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
