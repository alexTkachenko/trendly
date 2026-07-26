# Database conventions

> This document is built up incrementally across the E-DB-1 epic.
> "Postgres version & database name" (DB-1.1) and "Migration workflow"
> (DB-1.2) were added first. DB-1.3 adds the remaining sections below
> (primary keys, table/column naming, column types, constraint/index/
> trigger naming, timestamps, FK delete behavior, migration file naming,
> constraints, schema documentation). With DB-1.3 merged, this doc covers
> everything E-DB-2
> tasks need to implement a table without further clarification — read
> this doc plus the individual task file, nothing else should be
> required for formatting/naming/PK/timestamp/FK questions.

## Postgres version & database name

- **Target Postgres major version:** 16 (pinned via `postgres:16-alpine`
  in `docker-compose.yml`, OPS-1.1). No reason to pin an older major
  version for a greenfield project; verified locally against
  `postgres:16-alpine` (reports `PostgreSQL 16.14`).
- **Database name:** `trendly` (`POSTGRES_DB` in `.env.example`). This is
  the name the backend datasource config (BE-1.1) must target — confirmed
  no mismatch between `docker-compose.yml` (OPS-1.1), `.env.example`, and
  this task.
- **Default user:** `trendly` (`POSTGRES_USER` in `.env.example`), same
  user owns the database for local dev.

## Migration workflow (DB-1.2)

All schema changes go through versioned Flyway migrations. This section
documents the file location, naming rule, and how to run them — the
Spring-Boot-triggers-Flyway-on-startup wiring itself is BE-1.1's
responsibility (see "Status" note below).

### Location

Migrations live at:

```
backend/src/main/resources/db/migration/
```

This is Flyway's conventional classpath default
(`classpath:db/migration`), so once BE-1.1 adds `flyway-core` to the
build, no extra `spring.flyway.locations` config is needed.

### Naming convention

`V{n}__description.sql` — e.g. `V1__init.sql`, `V2__create_users_table.sql`.

- `V` prefix (versioned migration), then an integer version, **two**
  underscores, then a snake_case description, then `.sql`.
- Versions are sequential and never reused. Pick the next unused integer;
  don't reuse or reorder a version once any teammate/CI/environment has
  applied it.
- **Never edit a migration that has already been applied anywhere**
  (a teammate's local DB, CI, or any deployed environment). Flyway
  validates each applied file's checksum against what's on disk and will
  refuse to run (`FlywayValidateException`) if a previously-applied file
  changed. If a mistake needs fixing after the fact, ship a new
  higher-numbered migration that corrects it — don't touch the old file.

### Running migrations manually

Once BE-1.1 lands with the real Maven `pom.xml` and `flyway-core`/
`flyway-database-postgresql` on the build, migrations can be run
standalone (independent of starting the Spring Boot app) via the Maven
plugin:

```bash
./mvnw flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/trendly \
  -Dflyway.user=trendly \
  -Dflyway.password=$POSTGRES_PASSWORD
```

The `flyway-maven-plugin` does **not** read Spring Boot's `application.yml`
— its config sources are its own `<configuration>` block in `pom.xml`,
`flyway.conf`, `-Dflyway.*` system properties, or `FLYWAY_*` env vars.
BE-1.1 must configure one of those explicitly if a bare
`./mvnw flyway:migrate` (no flags) should work.

Before BE-1.1 lands (no real `pom.xml` yet), this was verified instead via
the standalone `flyway/flyway` Docker CLI against DB-1.1's Postgres
container — same engine, same `flyway_schema_history` bookkeeping, just
without a Maven project in front of it. See `tasks/database/DB-1.2.md`'s
implementation notes for the exact command and full verification
transcript.

### Idempotency

Running `migrate` twice in a row is a no-op by Flyway's default behavior:
the first run applies pending versions and records them in
`flyway_schema_history`; the second run sees the recorded version, finds
nothing new to apply, and reports `Schema is up to date. No migration
necessary.` without re-running any SQL. Verified for `V1__init.sql` in
this task (see DB-1.2 implementation notes for the full transcript).

### Status

The migration file, naming rules, and manual-run workflow above are in
place and verified (DB-1.2). Wiring Flyway into the real Spring Boot app
(`flyway-core` dependency, `spring.flyway.enabled=true`,
`spring.jpa.hibernate.ddl-auto=validate`, confirming auto-migration on
startup) is tracked as acceptance criteria on `tasks/backend/BE-1.1.md`,
not here — see that task file for current status.

## Primary keys (DB-1.3)

**Decision: every table's surrogate key is a named primary key,
`id BIGINT GENERATED ALWAYS AS IDENTITY CONSTRAINT pk_<table> PRIMARY KEY`**
(e.g. `CONSTRAINT pk_users PRIMARY KEY` on `users`, `CONSTRAINT pk_posts
PRIMARY KEY` on `posts`) — never the bare inline `PRIMARY KEY` with no
`CONSTRAINT` clause, which Postgres auto-names (`posts_pkey`,
`users_pkey`) and contradicts "Constraint, index, and trigger naming",
below. This is settled
by [ADR-001 — Primary key strategy: bigint identity](../adr/001-primary-key-strategy.md);
this section is a pointer to that ADR for the day-to-day rule, not a
re-litigation of it — see the ADR for the full UUID/hybrid tradeoff
discussion and the accepted enumerability risk.

- Applies uniformly, including tables whose IDs appear in public URLs
  (`GET /posts/{id}`, etc.) — no UUIDs, no separate public-UUID column,
  per the ADR.
- Every foreign key column referencing a table's `id` is typed `BIGINT`.

**Join-table exception — read carefully, this is not a contradiction of
the ADR:** ADR-001 governs identifier *type* (bigint vs UUID), not
whether a pure join table gets a surrogate `id` at all. Per
`docs/PROJECT_PLAN.md`'s E-DB-2 epic table, the two pure many-to-many
join tables in this schema use a **composite `PRIMARY KEY` over their
bigint FK columns instead of a surrogate `id` column**. Both tables use
the *same* mechanism — a real composite `PRIMARY KEY`, not one table
with a PK and the other with a bare `UNIQUE` and no PK at all — so every
table in the schema still has exactly one primary key:

- `follows` — `CONSTRAINT pk_follows PRIMARY KEY (follower_id,
  followee_id)`; both FK columns use the full named form, e.g.
  `follower_id BIGINT NOT NULL CONSTRAINT fk_follows_follower_id
  REFERENCES users(id) ON DELETE CASCADE` (and the equivalent for
  `followee_id`) — **never** a bare `BIGINT REFERENCES users(id)`, which
  both auto-names the FK (contradicting "Constraint, index, and trigger
  naming") and silently omits the `ON DELETE CASCADE` this table's FKs
  to `users` require (see "Foreign key delete behavior", below). No `id`
  column.
- `post_likes` — `CONSTRAINT pk_post_likes PRIMARY KEY (post_id,
  user_id)`; same full named form for both FKs: `post_id BIGINT NOT NULL
  CONSTRAINT fk_post_likes_post_id REFERENCES posts(id) ON DELETE
  CASCADE`, `user_id BIGINT NOT NULL CONSTRAINT fk_post_likes_user_id
  REFERENCES users(id) ON DELETE CASCADE`. No `id` column.

These two tables also skip `updated_at` entirely (no column, no
trigger) — see the join-table carve-out under "Timestamps", below, for
the full rationale.

Rule of thumb for future tables: if a table exists *only* to represent
a many-to-many relationship between two other tables, with no
attributes of its own beyond the relationship and its `created_at`, use
a composite `PRIMARY KEY` on the natural FK pair, skip the surrogate
`id`, and skip `updated_at` (see Timestamps). Any table that carries
additional identity-bearing attributes of its own (e.g. `comments`,
which has its own content and needs
its own single-column identity for API addressing and future FKs like a
"reply-to" self-reference) gets the standard surrogate `id` and the
full `created_at`/`updated_at` pair.

## Table naming (DB-1.3)

**snake_case, plural.** Already used consistently across
`docs/PROJECT_PLAN.md`'s epic tables — this section confirms them as
canonical, it does not introduce new names:

`users`, `posts`, `post_media`, `follows`, `comments`, `post_likes`.

Don't invent synonyms or alternate pluralizations (no `post_medias`, no
`user_follows`) — match these exact names when a migration creates one
of these tables.

## Column naming (DB-1.3)

- All columns snake_case (`display_name`, `password_hash`,
  `media_type`, not `displayName`/`passwordHash`).
- The surrogate primary key column is always named `id` (see Primary
  keys, above; join tables per the composite-key exception have no
  `id` column at all).
- Foreign key columns are named `<referenced_table_singular>_id`, e.g.
  `author_id`, `post_id`. When a table has more than one FK to the same
  referenced table, the column name reflects the **role**, not a repeat
  of the table name, since `<table_singular>_id` would collide:
  `follows.follower_id` and `follows.followee_id` both reference
  `users(id)` but are named for the role each plays in the relationship,
  not `user_id` twice.
- `author_id` is the standard name for "the user who owns/wrote this
  content" (`posts.author_id`, `comments.author_id`) — prefer it over
  `user_id` on content tables so intent is unambiguous; reserve plain
  `user_id` for tables where the FK is a participant rather than an
  author (e.g. `post_likes.user_id` — the liker, not an author).

## Column types (DB-1.3)

- **Free-form strings: `TEXT`, not `VARCHAR(n)`.** Every text column
  (`display_name`, `bio`, `content`, `text_content`, `url`, etc.) is
  `TEXT`. Postgres stores `TEXT` and `VARCHAR` identically on disk, so
  `VARCHAR(n)` buys nothing. If a column needs a length limit, add it as
  an explicit, named `CHECK` constraint instead (see "Constraint, index,
  and trigger naming", below) —
  `CONSTRAINT chk_posts_text_content_length CHECK (char_length(text_content) <= 2000)`,
  for example. A named `CHECK` is easy to change later (`DROP
  CONSTRAINT` / `ADD CONSTRAINT`, no table rewrite), unlike
  `ALTER COLUMN ... TYPE VARCHAR(m)`, which does rewrite the table.
- **`users.email` / `users.username` uniqueness is case-insensitive**,
  enforced via a `UNIQUE` index on the lowercased value, e.g.
  `CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));`
  (and the equivalent for `username`) — **not** the `citext` extension.
  Rationale: this needs no `CREATE EXTENSION citext` step (an extra,
  easy-to-forget per-database dependency), at the cost of one
  requirement DB-2.1 and every later repository query must honor: any
  lookup by email/username must filter on `lower(email) = lower(?)` /
  `lower(username) = lower(?)` for both correctness (case-insensitive
  match) and to actually use the index (a plain `WHERE email = ?` won't
  hit an index built on `lower(email)`). The column type itself stays
  plain `TEXT`.
- **`post_media.media_type`: `TEXT` + `CHECK`, not a Postgres `ENUM`
  type.** E.g. `media_type TEXT NOT NULL CONSTRAINT
  chk_post_media_media_type CHECK (media_type IN ('IMAGE', 'VIDEO'))`
  (exact allowed values reconciled with `docs/api/` when DB-2.3 is
  implemented). Rationale: adding a value to a native Postgres `ENUM` type has more
  operational edge cases — on older Postgres versions, `ALTER TYPE ...
  ADD VALUE` historically couldn't run inside the same transaction as
  other DDL and couldn't be rolled back — than a `CHECK` constraint,
  which is a plain `DROP CONSTRAINT` / `ADD CONSTRAINT` in a new
  migration: simpler and fully transactional.

## Constraint, index, and trigger naming (DB-1.3)

**Decision: name every constraint, index, and trigger explicitly —
don't rely on Postgres's auto-generated names** (`posts_author_id_fkey`,
`users_email_key`, etc.). Auto-generated names work until two tables'
names collide in the generated pattern or a later migration needs to
`DROP`/`ALTER` a specific one by name; explicit names make intent
self-documenting in the migration and safe to reference from a later
migration.

| Object | Pattern | Example |
|---|---|---|
| Primary key | `pk_<table>` | `pk_follows` |
| Unique constraint/index | `uq_<table>_<col(s)>` | `uq_users_email_lower` |
| Check constraint | `chk_<table>_<what>` | `chk_follows_no_self_follow` |
| Foreign key | `fk_<table>_<col>` | `fk_posts_author_id` |
| Non-unique index | `idx_<table>_<col(s)>` | `idx_posts_author_id_created_at` |
| Trigger | `trg_<table>_<what>` | `trg_posts_set_updated_at` |

Every `CREATE TABLE` / `ALTER TABLE` / `CREATE INDEX` / `CREATE TRIGGER`
in an E-DB-2+ migration uses `CONSTRAINT <name> ...`, `CREATE INDEX
<name> ON ...`, or `CREATE TRIGGER <name> ...` explicitly rather than
letting Postgres choose a name.

## Timestamps (DB-1.3)

Every table gets `created_at`, `TIMESTAMPTZ NOT NULL DEFAULT now()`.
Every table *except the two pure join tables* (see the join-table
carve-out immediately below) also gets `updated_at`, `TIMESTAMPTZ NOT
NULL DEFAULT now()`.

**Join-table carve-out:** `follows` and `post_likes` get `created_at`
only — **no `updated_at` column, and no `updated_at` trigger.** A follow
or a like is an immutable fact: it either exists (one `INSERT`) or it
doesn't (one `DELETE`); there is no legitimate `UPDATE` on either row,
so there is nothing for `updated_at` to track. This matches
`docs/PROJECT_PLAN.md`'s DB-2.4/DB-2.6 column lists, which name only
`created_at` for these two tables. **Do not attach the shared
`set_updated_at()` trigger (below) to `follows` or `post_likes`** — a
table with no `updated_at` column makes the trigger fail on its first
`UPDATE` with `ERROR: record "new" has no field "updated_at"` (verified
against Postgres 16.14: attach `set_updated_at()` to a table lacking the
`updated_at` column and run any `UPDATE` on it to reproduce).

**Decision: on every table that *does* have `updated_at`, it is
maintained by a generic, reusable database trigger — not by the
application layer.** A single trigger function is defined once and
attached to each such table via a `BEFORE UPDATE` trigger:

```sql
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- per table, e.g. for `posts`:
CREATE TRIGGER trg_posts_set_updated_at
BEFORE UPDATE ON posts
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
```

Rationale: a DB trigger cannot be forgotten in some future code path —
bulk updates, admin scripts, a future service, or a repository method
where someone simply forgets to set `updatedAt`. Every `UPDATE`
statement gets the timestamp right with zero app-layer discipline
required. The one downside (an extra trigger definition per table) is a
fixed, one-time cost paid in the migration that creates the table,
versus an ongoing, easy-to-miss obligation in every application code
path that performs an `UPDATE`.

**The shared `set_updated_at()` function is created once, in
`V2__create_users_table.sql` (DB-2.1)** — the first migration after the
no-op `V1__init.sql` baseline (DB-1.2) that needs it, since `users` is
the first table with an `updated_at` column. It must not be added
retroactively to `V1__init.sql`, since that migration is already
applied and, per "Migration workflow (DB-1.2)", already-applied
migrations are never edited. Every later table migration that has an
`updated_at` column (every table except `follows`/`post_likes`) adds its
own `CREATE TRIGGER ... EXECUTE FUNCTION set_updated_at();` line
referencing this shared function — it does not redefine the function.

`created_at` has no trigger on any table — it's set once via
`DEFAULT now()` at insert and never updated after.

## Foreign key delete behavior (DB-1.3)

> The stance for **FKs to `users`** below is settled by
> [ADR 002 — Account deletion: cascade every FK referencing `users`](../adr/002-user-deletion-fk-behavior.md),
> which records the product decision (cascade everything vs. preserving
> comments as "[deleted user]") and its accepted tradeoffs.

**Default stance: `ON DELETE CASCADE` for both (a) ownership/composition
relationships, and (b) any FK to `users` representing authorship or
participation.** In this schema almost every relationship is one of
these two cases, so cascade is the default a migration should reach for
unless it has a specific reason not to — and if it deviates, **that
table's migration must say so explicitly in its header comment.**

- **Ownership/composition** (child row has no meaning without its
  parent): `post_media.post_id → posts.id ON DELETE CASCADE`,
  `comments.post_id → posts.id ON DELETE CASCADE`,
  `post_likes.post_id → posts.id ON DELETE CASCADE`. Deleting a post
  removes its media, its comments, and its likes.
- **FKs to `users`** (authorship or participation in content):
  `posts.author_id → users.id ON DELETE CASCADE`,
  `comments.author_id → users.id ON DELETE CASCADE`,
  `follows.follower_id → users.id ON DELETE CASCADE`,
  `follows.followee_id → users.id ON DELETE CASCADE`,
  `post_likes.user_id → users.id ON DELETE CASCADE`. (`post_likes.post_id`
  is listed under ownership/composition above, not here — it's an FK to
  `posts`, not `users`.)

  Rationale: deleting a user account should remove that user's entire
  footprint (their posts, their comments — including comments they left
  on other users' posts — their follow edges, their likes) in one
  consistent, predictable operation, with no orphaned rows and no
  `RESTRICT` errors blocking an account-deletion feature. It also avoids
  the alternative of making `author_id`/`follower_id`/etc. nullable to
  support `ON DELETE SET NULL`, which would contradict these columns
  being required (`NOT NULL`) identity/attribution fields.
- **`RESTRICT` / `SET NULL`** are not used anywhere in the current
  E-DB-2 schema — there is no table today where we want to block a
  parent's deletion or preserve a child row with its FK nulled out. If a
  future **non-`users`** table needs to *retain* history after its
  referenced row is gone (e.g. an audit log, a moderation record keyed
  on `post_id`), that table's own migration should use `SET NULL` (with
  a nullable FK column) or `RESTRICT` deliberately, and must document
  the reasoning in its header comment — don't default to cascade in that
  case just for consistency with this section. **This escape hatch does
  not apply to FKs referencing `users.id`**: ADR-002 is explicit that
  every FK to `users` is `ON DELETE CASCADE` with no exceptions,
  including a future audit-log-style table with a `user_id` column —
  deviating there requires a new ADR superseding ADR-002, not a
  migration-header comment.

## Migration file naming (DB-1.3)

Covered already under "Migration workflow (DB-1.2)" above
(`V{n}__description.sql`, sequential, never reused/edited after being
applied) — not repeated here.

## Constraints (DB-1.3)

**Prefer DB-level constraints (`UNIQUE`, `CHECK`, `NOT NULL`, `FOREIGN
KEY`) over application-only validation for every invariant that must
always hold**, regardless of which code path writes the row (a REST
endpoint, a bulk admin script, a future service, direct SQL run by a
human). Application-layer validation (Bean Validation annotations on
DTOs, etc.) is still expected for user-facing error messages and early
rejection, but it is a UX layer on top of the DB constraint, never a
substitute for it.

Concrete examples required by this schema:

- **Unique email:** enforced by the case-insensitive
  `UNIQUE INDEX uq_users_email_lower ON users (lower(email))` from
  "Column types", above — **not** a plain `UNIQUE` constraint on the raw
  column, which would allow `ann@example.com` and `Ann@Example.com` as
  two distinct rows. Also `NOT NULL`. Enforced at the DB level either
  way, so a race-prone "check email doesn't exist" service-layer query
  is never load-bearing — but the specific mechanism matters here, and
  it's the lowercase index, not a bare `UNIQUE`.
- **Unique username:** same reasoning and same mechanism —
  `uq_users_username_lower` on `lower(username)`, not a plain `UNIQUE`
  on `username`.
- **No self-follow:** `follows` gets `CONSTRAINT
  chk_follows_no_self_follow CHECK (follower_id <> followee_id)` — a
  user cannot follow themselves, enforced at insert time regardless of
  what the application layer does or doesn't check.
- **No duplicate follow/like pairs:** the composite `PRIMARY KEY` on
  `follows(follower_id, followee_id)` **and** the composite `PRIMARY KEY`
  on `post_likes(post_id, user_id)` (see Primary keys, above — both join
  tables use the same mechanism, a real `PRIMARY KEY`, not one PK and
  one bare `UNIQUE`) double as the "can't follow/like the same target
  twice" constraint — no separate application-level duplicate check is
  load-bearing.
- **Required fields:** any column that the domain requires to always
  have a value (`users.email`, `users.username`, `users.password_hash`,
  `posts.author_id`, `comments.content`, etc.) is `NOT NULL` in the
  migration, not merely `@NotNull` on a DTO. `users`' remaining optional
  columns (`display_name`, `bio`, `avatar_url`) are nullable — this doc
  doesn't enumerate every column's nullability, that's `DB-2.1`'s task
  file's job; if that task file doesn't already say so explicitly for a
  given column, treat it as nullable unless there's a clear domain
  reason not to.
- **Index every foreign key column that isn't already covered by a
  table's primary key**, in the migration that creates it, named per
  the `idx_<table>_<col(s)>` pattern above. A composite `PRIMARY KEY`
  already provides an index usable for lookups on its **leading**
  column — e.g. `pk_follows (follower_id, followee_id)` already indexes
  `follower_id`, so `follows` needs no separate `idx_follows_follower_id`
  — but not its trailing column, so `follows.followee_id` (the "who
  follows me" lookup) still needs its own explicit index. `E-DB-3`'s
  tasks (`DB-3.1`-`DB-3.4`) cover indexes beyond a bare single-column FK
  lookup — composite/ordered indexes for specific query patterns (e.g.
  `posts(author_id, created_at desc)` for feed queries) — and any
  single-column FK index E-DB-3 lists that a table's own migration
  already created per this rule should be treated as already satisfied,
  not duplicated.

## Schema documentation (DB-1.3 note)

`docs/db/schema.md` — the per-table schema doc referenced by DB-2.1's
acceptance criteria — **does not exist yet**. This task covers naming/
typing *conventions* only, not the per-table documentation itself.
DB-2.1 (the `users` table, the first E-DB-2 task) creates
`docs/db/schema.md` and establishes its format; a reasonable shape is
one section per table listing columns/types, constraints, indexes, and
FK behavior, using this conventions doc's terminology. This note exists
so the gap is visible and intentional, not a silent miss.
