# DB-1.1 — Provision local Postgres

**Epic:** E-DB-1 — Schema foundation & migrations tooling
**Owning agent:** database-agent
**Depends on:** OPS-1.1 (compose service), coordinate directly with devops-agent
**Size:** S

## Description
Confirm and document the local PostgreSQL setup that the rest of the
database epics build on. devops-agent owns the `docker-compose.yml`
mechanics (OPS-1.1); this task is about the database-side configuration
and verification: correct version, correct DB/user, and a documented way
to connect from a local client for inspection.

## Scope
- Confirm target Postgres major version (recommend 16 unless there's a
  reason to pin older) and record it in `docs/db/conventions.md`.
- Verify the `POSTGRES_DB` name matches what the backend's datasource
  config (BE-1.1) will target — coordinate the exact name (suggested:
  `trendly`) so there's no mismatch.
- Confirm the container is reachable from the host for local tooling
  (`psql`, TablePlus, DBeaver, etc.) via the port defined in OPS-1.1.
- Add a short "Connecting locally" section to `docs/local-dev.md` (or
  create it if devops-agent hasn't yet) with a sample `psql` connection
  string using the `.env.example` values.

## Acceptance criteria
- [x] Postgres version and DB name recorded in `docs/db/conventions.md`.
- [x] `psql "postgresql://<user>:<password>@localhost:<port>/<db>"` (using
      `.env.example` values) successfully connects to the running
      container.
- [x] Any mismatch between backend datasource config and actual DB
      name/credentials is caught and fixed here, not discovered later in
      BE-1.1.

## Notes for implementer
- This is a short verification/coordination task, not new infra — the
  actual compose service is OPS-1.1's responsibility. Flag to
  architect-agent if devops-agent's compose file and this task disagree
  on naming.

## Implementation notes

- Created `docs/db/conventions.md` with a minimal "Postgres version &
  database name" section only (Postgres 16, database `trendly`, user
  `trendly`). Left all other conventions sections (naming, PK strategy,
  timestamps, FK/`ON DELETE` policy) unwritten — those belong to DB-1.3.
- Verified end-to-end locally: `cp .env.example .env`, `docker compose up
  -d postgres`, waited for the healthcheck to report `healthy`, then ran
  `psql "postgresql://trendly:trendly_dev_password@localhost:<port>/trendly"`.
  Confirmed `SELECT version()` → `PostgreSQL 16.14` (matches the
  `postgres:16-alpine` image pinned in OPS-1.1's `docker-compose.yml`)
  and `SELECT current_database(), current_user` → `trendly` / `trendly`.
  - Note: on this machine, host port 5432 was already bound by an
    unrelated, pre-existing local Postgres container, so the verification
    run used an overridden `POSTGRES_PORT` for that one session only.
    This is a local-machine artifact, not a project issue — the compose
    file's own default (`${POSTGRES_PORT:-5432}`) is correct and was left
    unchanged. Anyone hitting the same conflict should override
    `POSTGRES_PORT` in their own `.env` per the existing "Port conflicts"
    guidance in `docs/local-dev.md`.
  - Cleaned up fully afterward: `docker compose down -v` (removed
    container, network, and the `trendly_pgdata` volume created during
    verification) and deleted the test `.env`/`.env.bak` files. No
    container, volume, or `.env` was left behind.
- Confirmed no mismatch: `docker-compose.yml` (OPS-1.1), `.env.example`,
  and this task all agree on `POSTGRES_DB=trendly` /
  `POSTGRES_USER=trendly`. Nothing to flag to architect-agent.
- Added a "Connecting locally" section to `docs/local-dev.md` with the
  sample `psql` connection string built from `.env.example` values.
- Out of scope, not touched: Flyway setup (DB-1.2), full
  `docs/db/conventions.md` authoring (DB-1.3).

**Status:** Ready for review
