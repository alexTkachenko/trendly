# OPS-1.1 — docker-compose for local stack

**Epic:** E-OPS-1 — Local dev environment
**Owning agent:** devops-agent
**Depends on:** —
**Size:** M

## Description
Create a `docker-compose.yml` at the repo root that brings up the full local
stack: PostgreSQL, the Spring Boot backend, and the Next.js frontend. Each
service should be independently buildable/runnable and the compose file
should be the single source of truth for local dev — no undocumented manual
setup steps.

## Scope
- `postgres` service:
  - Official `postgres:16` (or newer LTS) image.
  - Named volume for data persistence (`trendly_pgdata`).
  - Exposes 5432 to host for local tooling (psql, DB clients).
  - Healthcheck using `pg_isready`.
  - Reads `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` from `.env`.
- `backend` service:
  - Builds from `backend/Dockerfile` (see OPS-1.2).
  - Depends on `postgres` with `condition: service_healthy`.
  - Reads DB connection + JWT secret + any other config from `.env`.
  - Exposes 8080.
- `frontend` service:
  - Builds from `frontend/Dockerfile` (see OPS-1.3).
  - Depends on `backend`.
  - Reads `NEXT_PUBLIC_API_BASE_URL` from `.env`.
  - Exposes 3000.
- A shared Docker network so services can reach each other by service name.

## Acceptance criteria
- [x] `docker compose up` from repo root starts all three services with no
      manual steps beyond copying `.env.example` → `.env`.
- [x] `docker compose up` succeeds on a machine with nothing pre-installed
      except Docker (no local Postgres/Node/Java required).
- [x] Postgres data survives a `docker compose down` (without `-v`) and
      comes back on `docker compose up`.
- [x] Backend does not attempt to connect to Postgres before it's healthy
      (verified via `depends_on: condition: service_healthy`).
- [x] `docs/deployment.md` (or a new `docs/local-dev.md`) documents how to
      start/stop the stack and where logs can be viewed.

## Notes for implementer
- Coordinate table/DB name with database-agent's DB-1.x work — the same
  `POSTGRES_DB` value must be what Flyway migrations target.
- Do not hardcode ports if avoidable; allow override via `.env` for
  contributors who already have 5432/8080/3000 in use.

## Implementation notes (devops-agent, 2026-07-25)
`backend/Dockerfile` and `frontend/Dockerfile` did not exist yet (OPS-1.2 /
OPS-1.3, and the apps themselves in BE-1.1 / FE-1.1, are separate
not-yet-done tasks). To actually satisfy the "`docker compose up` succeeds"
acceptance criteria end-to-end rather than leaving it unverified, this task
also adds **throwaway placeholder** Dockerfiles + minimal dependency-free
apps:
- `backend/placeholder/PlaceholderApp.java` + `backend/Dockerfile` — plain
  JDK `HttpServer`, no Maven/framework deps, exposes `/actuator/health`.
- `frontend/placeholder/server.js` + `frontend/Dockerfile` — plain Node
  `http` server, no npm install, echoes `NEXT_PUBLIC_API_BASE_URL`.

Both are clearly commented as throwaway and should be deleted/replaced
wholesale by OPS-1.2/OPS-1.3 + BE-1.1/FE-1.1. `docker-compose.yml` itself
does not depend on the placeholders' internals (just that `backend/` and
`frontend/` each have a `Dockerfile` exposing 8080/3000) and should not need
changes when the real Dockerfiles land.

Verified locally (Docker Desktop, `docker compose` v5):
- `docker compose config` validates with `.env` populated from
  `.env.example`.
- `docker compose up -d --build` brought up all three containers;
  `postgres` reported healthy before `backend` started (confirmed via
  `depends_on: condition: service_healthy` and container start-order in
  `docker compose up` output).
- `curl localhost:8080/actuator/health` and `curl localhost:3000/`
  succeeded from the host; `frontend` reached `backend` by service name
  (`http://backend:8080/...`) from inside the network.
- Inserted a marker row into Postgres, ran `docker compose down` (no
  `-v`), ran `docker compose up` again, confirmed the row was still
  present in the `trendly_pgdata` volume, then cleaned up.
- Host port 5432 was already occupied on this dev machine by an unrelated
  container; overriding `POSTGRES_PORT` in `.env` (no code changes)
  resolved it, confirming the "don't hardcode ports" requirement works in
  practice.
- Final teardown: `docker compose down -v` + removed the locally built
  `trendly-backend`/`trendly-frontend` images and the test `.env` — repo
  is left clean, nothing left running.

**Status:** Ready for review
