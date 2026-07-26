# DB-1.2 — Set up Flyway migrations

**Epic:** E-DB-1 — Schema foundation & migrations tooling
**Owning agent:** database-agent
**Depends on:** DB-1.1
**Size:** M

## Description
Introduce Flyway as the single source of truth for schema changes, wired
into the backend module so migrations run automatically on application
startup (and can also be run standalone for CI/tooling purposes).

## Scope
- Add the Flyway dependency to the backend build (coordinate location —
  `backend/src/main/resources/db/migration/` is the conventional path).
- Ensure `spring.flyway.enabled=true` and
  `spring.jpa.hibernate.ddl-auto=validate` (never `update` or `create`)
  are set in the backend's application config, so Hibernate can never
  silently mutate the schema — Flyway is the only writer.
- Create `V1__init.sql` as an empty/no-op baseline migration (or with a
  minimal `schema_version` sanity table if preferred) so the migration
  chain has a clean starting point before DB-2.x adds real tables.
- Document the migration workflow in `docs/db/conventions.md`:
  naming convention (`V{n}__description.sql`), the "never edit an applied
  migration" rule, and how to run migrations manually
  (`mvn flyway:migrate` / `./gradlew flywayMigrate`, matching whatever
  build tool BE-1.1 uses).

## Acceptance criteria
- [x] `V1__init.sql` exists and applies cleanly against a fresh DB from
      DB-1.1.
- [ ] Starting the backend app (even the BE-1.1 skeleton) triggers Flyway
      migration automatically and logs the applied version. — **Deferred
      to BE-1.1**: there is no real Spring Boot app yet (current
      `backend/pom.xml`/`backend/src` are OPS-1.2's throwaway placeholder,
      no Flyway on the classpath at all). Verified standalone instead —
      see implementation notes.
- [ ] `ddl-auto` is confirmed set to `validate` (or `none`), not `update`.
      — **Deferred to BE-1.1**: no `application.yml` exists yet to set
      this in; nothing to verify against today.
- [x] `docs/db/conventions.md` documents the migration naming/workflow
      rules referenced by database-agent's own agent config.
- [x] Running the migration command twice in a row is a no-op (idempotent
      — Flyway's default behavior, just verify it's not broken by config).

## Notes for implementer
- If BE-1.1 hasn't landed yet, this task can still set up the
  `db/migration` folder and the `V1__init.sql` file; the Spring wiring
  step (build dependency + config in `application.yml`) should be
  coordinated with or handed off to backend-agent, whichever agent
  actually owns the Spring Boot project file at that point. Note the
  handoff explicitly in your task summary so reviewer-agent can check
  both sides landed correctly.

## Implementation notes

- **BE-1.1 has not landed yet.** `backend/pom.xml` is still OPS-1.2's
  throwaway placeholder (no `flyway-core`, no Spring Boot at all — a plain
  `HttpServer` app), and there is no `application.yml`. Per this task's own
  notes, scoped the work to what's achievable without touching that
  placeholder or inventing a Spring config that BE-1.1 would just discard:
  - Created `backend/src/main/resources/db/migration/V1__init.sql` — an
    intentionally no-op baseline migration (comment-only; rationale for
    no-op vs. a hand-rolled `schema_version` table is in the file header:
    Flyway's own `flyway_schema_history` already does that bookkeeping).
    Placed at Flyway's conventional classpath default location so no
    `spring.flyway.locations` override will be needed once BE-1.1 adds the
    dependency.
  - Expanded `docs/db/conventions.md` with a new "Migration workflow
    (DB-1.2)" section: folder location, `V{n}__description.sql` naming
    rule, the never-edit-an-applied-migration rule (with the checksum-
    validation mechanism explained), how to run migrations manually via
    `./mvnw flyway:migrate` (documented for when BE-1.1's real `pom.xml`
    exists), and a "Status" subsection spelling out exactly what's done vs.
    deferred to BE-1.1. Did not touch any other section (naming/PK/FK
    conventions remain DB-1.3's scope, per DB-1.1's precedent).
  - **Verified standalone**, since there's no Maven project to run
    `mvn flyway:migrate` against: brought up DB-1.1's `postgres` service via
    `docker compose up -d postgres` (had to override `POSTGRES_PORT` in a
    local-only `.env` to `15432` for this session — host 5432 was already
    bound by an unrelated local Postgres, same port-conflict artifact noted
    in DB-1.1; not a project issue), waited for the healthcheck, then ran
    the official `flyway/flyway:10-alpine` Docker image on the same
    `trendly_net` network pointed at `jdbc:postgresql://postgres:5432/trendly`
    with the `.env.example` credentials, mounting
    `backend/src/main/resources/db/migration` as `/flyway/sql`.
    - `flyway info` (before migrating): showed `V1 — init` as the only
      migration, state `Pending` — confirms the file is discovered and
      parsed correctly at the conventional path.
    - `flyway migrate` (1st run): `Successfully applied 1 migration to
      schema "public", now at version v1`.
    - `flyway migrate` (2nd run, immediately after): `Schema "public" is up
      to date. No migration necessary.` — confirms idempotency; no SQL
      re-executed, no error.
    - Confirmed via `psql`: `flyway_schema_history` has exactly one row
      (`version=1, description=init, type=SQL, success=t`).
  - Cleaned up fully afterward: `docker compose down -v` (removed the
    container, network, and `trendly_pgdata` volume created for this
    verification) and deleted the local `.env`/`.env.bak`. Nothing left
    running or uncommitted.
- **Explicit handoff to BE-1.1 / backend-agent** (per this task's own
  notes for implementer): when BE-1.1 lands, it still needs to —
  1. Add `flyway-core` (and `flyway-database-postgresql`, required
     separately from Flyway 10 onward) to the real `pom.xml`.
  2. Set `spring.flyway.enabled=true` and
     `spring.jpa.hibernate.ddl-auto=validate` in the real
     `application.yml` (never `update`/`create`).
  3. Confirm on real startup that Flyway auto-runs against `V1__init.sql`
     and logs the applied version (the equivalent of the standalone
     `migrate` output above, but via `./mvnw spring-boot:run` /
     `java -jar app.jar`), and that Hibernate's `ddl-auto=validate` doesn't
     fail against the (currently table-less) schema.
  - Until that lands, the two acceptance criteria above that depend on a
    real Spring Boot app are left unchecked/deferred rather than falsely
    marked done — reviewer-agent should check both this task and BE-1.1's
    implementation notes to confirm the full loop closes.
- Out of scope, not touched: `backend/pom.xml`, any `application.yml`
  (BE-1.1's job — would be discarded work on top of the placeholder), and
  `docs/db/conventions.md`'s other sections (DB-1.3).

**Status:** Ready for review
