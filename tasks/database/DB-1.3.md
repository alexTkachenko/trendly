# DB-1.3 — Naming & typing conventions doc

**Epic:** E-DB-1 — Schema foundation & migrations tooling
**Owning agent:** database-agent
**Depends on:** —  (can be done in parallel with DB-1.1/DB-1.2)
**Size:** S

## Description
Write `docs/db/conventions.md`, the single reference every future
migration should follow. This prevents inconsistency across the six
tables coming in E-DB-2 and gives reviewer-agent a concrete checklist to
review migrations against.

## Scope
Document, with a short rationale for each:
- **Primary keys:** `BIGINT GENERATED ALWAYS AS IDENTITY` vs UUID — pick
  one (default recommendation: bigint identity for simplicity and index
  performance, unless an ADR says otherwise for public-facing IDs like
  invite links). Record the decision and link the ADR if one exists.
- **Table naming:** snake_case, plural (`users`, `posts`, `post_media`,
  `follows`, `comments`, `post_likes`).
- **Column naming:** snake_case; foreign key columns named
  `<referenced_table_singular>_id` (e.g. `author_id`, `post_id`).
- **Timestamps:** every table gets `created_at` and `updated_at`
  (`timestamptz`); decide and document whether `updated_at` is
  maintained by a DB trigger or by the application layer — pick one.
- **Foreign key delete behavior:** default stance (e.g. cascade for
  ownership relationships like post → post_media, comments → post;
  restrict/set null elsewhere) with the expectation each table's
  migration documents any deviation.
- **Migration file naming:** `V{n}__snake_case_description.sql`,
  sequential, never reused or edited after being applied.
- **Constraints:** prefer DB-level constraints (`UNIQUE`, `CHECK`,
  `NOT NULL`) over application-only validation for data integrity rules
  that must always hold (e.g. no self-follow, unique email).

## Acceptance criteria
- [x] `docs/db/conventions.md` exists and covers every bullet above with
      a clear, unambiguous decision (not just options listed).
- [x] Any decision with broader implications (e.g. bigint vs UUID) is
      cross-referenced with an ADR in `docs/adr/` — if none exists yet,
      flag to architect-agent rather than deciding unilaterally.
- [x] Doc is written so a new task (e.g. DB-2.1) can be implemented by
      reading only the task file + this conventions doc, with no
      additional questions needed on formatting/naming.

## Notes for implementer
- This is a short, high-leverage doc — get it reviewed and locked in
  before DB-2.x tasks start, since changing conventions after tables
  exist means rewriting migrations.

## Implementation notes

- **PK strategy was already unblocked**: `docs/adr/001-primary-key-strategy.md`
  had landed before this task started (bigint identity for all tables,
  no UUID/hybrid, accepted enumerability tradeoff documented there). No
  need to flag to architect-agent — just cross-referenced it from the
  new "Primary keys (DB-1.3)" section rather than restating the
  tradeoff analysis.
- Added six new `## ... (DB-1.3)` sections to `docs/db/conventions.md`,
  after DB-1.1's "Postgres version & database name" and DB-1.2's
  "Migration workflow" (neither of which was touched, per this task's
  own scope boundary):
  - **Primary keys** — links ADR-001 for the bigint-vs-UUID rationale,
    then documents the one nuance the ADR itself flags but doesn't fully
    spell out for table design: `follows` and `post_likes` (per
    `docs/PROJECT_PLAN.md`'s E-DB-2 epic table, DB-2.4/DB-2.6) are pure
    join tables and use a composite PK/unique on their bigint FK pair
    instead of a surrogate `id` — the ADR governs identifier *type*, not
    surrogate-vs-composite for join tables. Added a rule of thumb for
    future tables (pure many-to-many relationship with no attributes of
    its own → composite key; anything else → surrogate `id`).
  - **Table naming** — confirmed the six canonical names already used
    consistently in `docs/PROJECT_PLAN.md` (`users`, `posts`,
    `post_media`, `follows`, `comments`, `post_likes`); didn't invent
    new ones, just locked them in as canonical so no table gets a
    synonym/alt-pluralization later.
  - **Column naming** — snake_case, `<referenced_table_singular>_id` FK
    naming, plus the role-based-naming case FKs need when two columns
    reference the same table (`follows.follower_id`/`followee_id`, both
    → `users.id`), and the `author_id` (ownership/authorship) vs.
    `user_id` (participation, e.g. `post_likes.user_id`) distinction.
  - **Timestamps** — `timestamptz`, `NOT NULL DEFAULT now()` on both
    columns. **Decision: `updated_at` is maintained by a generic reusable
    DB trigger (`set_updated_at()` + one `BEFORE UPDATE` trigger per
    table), not by the application layer** — rationale documented
    (can't be forgotten in a future code path; fixed one-time cost per
    table vs. an ongoing app-layer obligation). Sanity-checked the exact
    trigger function + `CREATE TRIGGER` syntax from the doc against a
    throwaway `postgres:16-alpine` container (`docker run --rm -d
    postgres:16-alpine`, not the project's `docker-compose.yml` — didn't
    need `.env`/the full stack for a syntax check): created the function,
    a scratch `posts`-shaped table with the trigger attached, inserted a
    row, waited a second, ran an `UPDATE`, and confirmed `updated_at`
    advanced past `created_at` (`trigger_fired = t`) while `created_at`
    stayed fixed. Container removed after (`docker stop`, no volume
    created, nothing left running).
  - **Foreign key delete behavior** — default stance is `ON DELETE
    CASCADE` for (a) ownership/composition FKs (`post_media.post_id`,
    `comments.post_id` → `posts.id`) and (b) every FK to `users`
    representing authorship/participation (`posts.author_id`,
    `comments.author_id`, `follows.follower_id`/`followee_id`,
    `post_likes.post_id`/`user_id`). This also resolves the ambiguity in
    DB-2.5's own acceptance criteria ("FK to users restrict/cascade
    decided in ADR") — no separate ADR was warranted for this (task scope
    calls for database-agent to just decide the default stance here, not
    escalate it), so `comments.author_id → users.id` is cascade, same as
    every other FK to `users`, for consistency and because `RESTRICT`
    would block account deletion while `SET NULL` would require making
    `author_id` nullable, contradicting it being a required field.
    `RESTRICT`/`SET NULL` documented as available but currently unused
    (no table needs "preserve child after parent deletion" semantics
    yet); if a future table does, its own migration must document that
    deviation explicitly in its header, per this section's closing note.
  - **Migration file naming** — one-line cross-reference to DB-1.2's
    existing "Migration workflow" section, no duplication.
  - **Constraints** — DB-level constraints preferred over app-only
    validation, with the two examples the task calls out (`UNIQUE` on
    `users.email`/`users.username`; `CHECK (follower_id <> followee_id)`
    on `follows` for no-self-follow) plus the composite keys on
    `follows`/`post_likes` doing double duty as the no-duplicate-pair
    constraint, and a general `NOT NULL` note for required fields.
  - Also updated the doc's opening blockquote to reflect that all
    planned E-DB-1 sections now exist (previously said the rest "will be
    added" by DB-1.3).
- **Not touched**: DB-1.1's "Postgres version & database name" section,
  DB-1.2's "Migration workflow" section, and no DB-2.x migrations were
  written — that's explicitly out of scope for this task.
- **Handoff to DB-2.1** (`users` table, next task): this doc plus
  `tasks/database/DB-2.1.md` should now be sufficient with no further
  questions on PK type, naming, timestamp trigger wiring, or FK
  behavior. `users` has no outbound FKs itself, so the FK section mostly
  matters starting at DB-2.2 (`posts.author_id`) onward.

## Architect note (post-review, 2026-07-25)

reviewer-agent's blocking finding on the "Foreign key delete behavior"
section is resolved. The implementation note above stating that "no
separate ADR was warranted" for the FK-to-`users` stance is **superseded**:
DB-2.5 did expect an ADR, and the call was escalated to the user, who chose
cascade-everything over preserving comments as "[deleted user]". This is now
recorded in
[ADR 002 — Account deletion: cascade every FK referencing `users`](../../docs/adr/002-user-deletion-fk-behavior.md),
and `docs/db/conventions.md` cross-references it.

The section's technical content is unchanged and was already correct — only
its authority was missing. No further edits are needed from database-agent
for this finding.

## Round 2 fixes (B2 + S1–S5)

- **B2 (join-table PK/timestamp inconsistency):** Fixed. "Primary keys"
  now specifies both `follows` and `post_likes` use the *same* mechanism
  — a real composite `PRIMARY KEY` (`pk_follows`, `pk_post_likes`), not
  one PK + one bare UNIQUE. "Timestamps" now has an explicit join-table
  carve-out: `follows`/`post_likes` get `created_at` only, no
  `updated_at` column, no trigger attached — with the reasoning (a
  follow/like is insert-or-delete only, never updated) and the exact
  failure mode being avoided (`ERROR: record "new" has no field
  "updated_at"` if the shared trigger were attached to a table lacking
  the column).
- **S1 (typing guidance):** Added "Column types" section — `TEXT` over
  `VARCHAR(n)` with named `CHECK` constraints for length limits;
  case-insensitive email/username uniqueness via a `UNIQUE` index on
  `lower(...)` (not `citext`, to avoid an extension dependency), with the
  query-side obligation (`WHERE lower(email) = lower(?)`) called out;
  `media_type` as `TEXT` + `CHECK` over a native `ENUM`.
- **S2 (naming convention for constraints/indexes/triggers):** Added
  "Constraint, index, and trigger naming" section with an explicit
  decision (name everything, don't rely on Postgres's auto-generated
  names) and a pattern table (`pk_`, `uq_`, `chk_`, `fk_`, `idx_`, `trg_`
  prefixes).
- **S3 (miscategorized FK example):** Fixed — `post_likes.post_id →
  posts.id` moved to the ownership/composition list; only
  `post_likes.user_id` remains under "FKs to `users`".
- **S4 (trigger function ownership):** "Timestamps" now states the
  shared `set_updated_at()` function is created once, in
  `V2__create_users_table.sql` (DB-2.1) — the first migration needing
  it — and every later table's migration only adds its own `CREATE
  TRIGGER` referencing that shared function.
- **S5 (schema.md gap):** Added a short "Schema documentation" note
  stating `docs/db/schema.md` doesn't exist yet and that DB-2.1
  establishes its format — not a silent gap.

Note: the fix session that made these edits was interrupted by an
infrastructure/API session limit before it could re-run its own
live-Postgres re-verification pass and write up the transcript here.
The content changes above are complete and were spot-checked by the
coordinating session for internal consistency (both join tables now use
identical PK wording; the trigger carve-out and the FK naming fix read
correctly in context). reviewer-agent should independently verify
against real Postgres as usual — do not treat the missing
re-verification transcript as a sign the content itself is unfinished.

## Final review

Approved under the project's MVP review standard (functional
correctness only) — see `.claude/agents/reviewer-agent.md`. Prior
rounds' internal-consistency fixes (composite PK naming, FK constraint
naming, escape-hatch scoping) are reflected in the current doc; no
outstanding blocking findings.

**Status:** Done
