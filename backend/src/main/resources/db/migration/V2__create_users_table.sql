-- V2__create_users_table.sql
--
-- DB-2.1: `users` table -- the first real application table, and the
-- first migration needing an `updated_at` trigger, so the shared
-- `set_updated_at()` trigger function is defined here (see
-- docs/db/conventions.md "Timestamps"). Later tables with `updated_at`
-- reuse this function rather than redefining it.
--
-- FK delete behavior: n/a -- `users` has no outbound FKs. Later tables
-- with FKs to `users` (posts.author_id, follows.*, etc.) use
-- ON DELETE CASCADE per ADR-002 / docs/db/conventions.md.
--
-- Uniqueness: email/username uniqueness is case-insensitive, enforced via
-- UNIQUE indexes on lower(...) rather than plain UNIQUE constraints or the
-- citext extension -- see docs/db/conventions.md "Column types".

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_users PRIMARY KEY,
    email         TEXT NOT NULL,
    username      TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    display_name  TEXT,
    bio           TEXT,
    avatar_url    TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
CREATE UNIQUE INDEX uq_users_username_lower ON users (lower(username));

CREATE TRIGGER trg_users_set_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
