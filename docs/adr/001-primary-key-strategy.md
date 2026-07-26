# ADR 001 — Primary key strategy: bigint identity

**Status:** Accepted
**Date:** 2026-07-25
**Deciders:** User (product owner), architect-agent
**Affects:** All tables (`users`, `posts`, `post_media`, `follows`,
`comments`, `post_likes`, and every future table), `docs/db/conventions.md`
(DB-1.3), all E-DB-2 migrations, and any API contract that exposes a
resource identifier.

## Context

`docs/PROJECT_PLAN.md` §6 flagged "IDs: bigint identity vs UUID for primary
keys?" as an open cross-cutting decision. It blocks `tasks/database/DB-1.3.md`
(naming & typing conventions doc), whose acceptance criteria require the PK
strategy to be stated unambiguously and cross-referenced to an ADR rather
than chosen unilaterally by database-agent. Because primary keys propagate
into every foreign key, index, entity class, and public URL, changing this
after E-DB-2 tables exist is expensive — so it is settled before the first
domain migration is written.

Three options were put to the user:

1. **bigint identity** (`BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`)
   - Smallest storage (8 bytes), fastest joins, good B-tree index locality
     because values are monotonically increasing.
   - Natural creation-order sort — useful for feeds and keyset pagination.
   - Native to Postgres, no extension or app-side generation needed.
   - Downside: IDs are sequential, therefore enumerable and guessable if
     exposed in URLs, and they leak approximate row counts / growth rate.

2. **UUID (v4 or v7)**
   - Not enumerable; safe to expose directly in public URLs.
   - Client- or app-generatable before insert (no round-trip for the ID).
   - Downside: 16 bytes (2x storage, and larger for every FK and index);
     requires `pgcrypto`/`uuid-ossp` or application-side generation; v4 has
     poor index locality (random inserts across the B-tree), v7 fixes
     locality but is newer and less uniformly supported by tooling.

3. **Hybrid** — bigint identity internally plus a separate public UUID
   column on user-facing tables
   - Gets internal join/storage performance *and* non-enumerable public IDs.
   - Downside: two identifiers per exposed table, an extra unique index per
     table, and a lookup indirection (public UUID → internal bigint) on
     every externally-addressed read; more surface for bugs where the wrong
     ID leaks or is used.

Expected scale for this project is modest (a portfolio/early-stage social
network), and no requirement currently exists to hide row counts, growth
rate, or resource existence.

## Decision

**All tables use `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`.**

This applies uniformly:

- Every table gets a bigint identity `id`, including tables whose IDs appear
  in public-facing URLs (e.g. `GET /posts/{id}`, `/users/{username}/posts/{id}`).
- **No separate public/external UUID column is introduced at this time** —
  the hybrid scheme is explicitly rejected for now.
- Foreign key columns are `BIGINT` and reference these identity PKs.
- Join tables (`follows`, `post_likes`) may use composite natural primary
  keys over their bigint FK columns instead of a surrogate `id`, where the
  task file already specifies that (see DB-2.4, DB-2.6); this ADR governs
  the *type* of identifier, not whether a surrogate key is added to a pure
  join table.

The user chose this option accepting the enumerability/guessability
tradeoff, in exchange for simplicity, storage efficiency, and join
performance at this project's expected scale — rather than paying UUID's
storage/generation overhead or the hybrid scheme's added complexity.

## Consequences

### Positive
- Smallest practical key size: 8 bytes per key, repeated across every FK
  and index.
- Good index locality and predictable insert behavior under load.
- `id` ordering approximates creation order, which simplifies deterministic
  sorting and keyset (`WHERE id < ?`) pagination for feeds and comment
  threads.
- No extensions, no application-side ID generation, no ID-mapping layer —
  entities, DTOs, and API path variables all carry a plain `Long`/`number`.
- DB-1.3 can now state a single default with a rationale and link here, and
  E-DB-2 migrations can be written without further clarification.

### Negative / accepted risks
- **Main risk being accepted:** IDs exposed in routes (`GET /posts/{id}`,
  `GET /comments/{id}`, etc.) are sequential and therefore guessable. A
  third party can enumerate resources by incrementing an ID, and can infer
  approximate total row counts and growth rate over time from observed ID
  values. This is a deliberate, informed tradeoff, not an oversight.
- Because IDs are guessable, **authorization must never rely on ID
  unguessability**. Every endpoint that returns or mutates a resource by ID
  must perform an explicit ownership/visibility check (see BE-3.3, BE-6.2 —
  "only author can delete"), and must return the correct status for
  unauthorized access rather than assuming an attacker cannot construct the
  URL.
- Revisiting this decision later is expensive. If enumerability becomes a
  concrete problem (a real abuse vector, or a product requirement not to
  leak growth rate/row counts), the options are:
  - migrate PKs to UUIDs — costly, touches every FK, index, entity, and
    client; or
  - retrofit the hybrid approach by adding a public UUID column plus a
    unique index on the affected tables and switching the API to address
    resources by it — cheaper than a full migration but still a schema and
    contract change.
- **Lighter-weight mitigation available without a schema change:** an
  opaque/obfuscated ID encoding at the API layer (e.g. hashids or a similar
  reversible encoding, or a signed slug) can be added later so that public
  URLs no longer expose raw sequential integers, while the database keeps
  bigint identity keys. This changes the API contract but not the schema,
  and is the preferred first response if enumerability becomes a concern.

### Follow-ups
- DB-1.3 (`docs/db/conventions.md`) states bigint identity as the PK
  standard and cross-references this ADR.
- E-DB-2 migrations declare `id BIGINT GENERATED ALWAYS AS IDENTITY
  PRIMARY KEY` and `BIGINT` FK columns. **Exact DDL syntax note (added
  after DB-1.3 landed):** this ADR's decision is the identifier *type*
  (bigint identity), not the exact constraint syntax — `docs/db/conventions.md`'s
  "Constraint, index, and trigger naming" section additionally requires
  every constraint to be explicitly named
  (`CONSTRAINT pk_<table> PRIMARY KEY`, not a bare inline `PRIMARY KEY`).
  Where this ADR's examples show the unnamed form, treat
  `docs/db/conventions.md` as authoritative for syntax; this ADR's
  Decision (bigint identity, no UUID/hybrid) is unaffected.
- API contracts under `docs/api/` document path/response IDs as integers
  (JSON number, Java `Long`, TypeScript `number`).
