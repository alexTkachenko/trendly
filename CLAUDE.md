# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current state

This repo is pre-implementation: `backend/` and `frontend/` contain only `.gitkeep`
placeholders, and there is no `docker-compose.yml` yet. What exists today is the plan,
the agent configs, and env scaffolding. There are no build/lint/test commands to run
until the corresponding setup task (T1 in `docs/MVP_PLAN.md`) has been done — don't
assume a Maven/Next.js project layout exists until you've checked. `mvn` is not on
PATH; T1 is expected to generate the `./mvnw` wrapper (`.claude/settings.local.json`
pre-allows `./mvnw compile` and `./mvnw test -Dtest=<ClassName>`, and specifically
`-Dtest=AuthServiceTest`, implying the auth task should include a unit test with that
exact name). Java 21, Docker, and Docker Compose v2 (`docker compose`, not the standalone
`docker-compose`) are available locally.

## Project

Trendly — MVP social network: register, post (text + image), follow, feed, comment.
Stack: **Spring Boot 3 + Java 21** (backend) · **PostgreSQL** (database) ·
**Next.js App Router + TypeScript + Tailwind** (frontend).

Two planning docs, at two different scopes — check which one the current task belongs to:

- `docs/MVP_PLAN.md` — the active plan: 6 sequential tasks (T1–T6, ~2.5h) to a bare
  working demo. Cut from scope: Flyway, CI/CD, tests beyond smoke, likes, refresh
  tokens, S3, profile editing, a11y/mobile polish, API docs. **This is what "the MVP"
  means in this repo** — don't pull in PROJECT_PLAN.md scope unless asked.
- `docs/PROJECT_PLAN.md` — the full post-MVP plan, kept for later, with a much larger
  agent roster (architect/backend/frontend/database/devops/reviewer) than what's
  actually implemented in `.claude/agents/`. Epics/tasks (e.g. `BE-2.1`, `DB-2.3`)
  referenced elsewhere refer to IDs in this file's section 4 table.

`docs/brainstorming/branding.md` and `docs/brainstorming/concept.md` are product/naming
exploration for a much bigger long-term vision (fashion trend intelligence platform) —
not binding for the MVP, reference only. Don't let this scope-creep the 6 tasks above.

## Working model: builder / reviewer split

`.claude/agents/builder.md` and `.claude/agents/reviewer.md` are the two agents actually
configured (the wider PROJECT_PLAN.md roster was never built out — don't recreate it
unless asked). Lifecycle for every task in `docs/MVP_PLAN.md`:

```
builder implements task → verifies app builds/runs → hands off
        → reviewer, fresh context (task description + diff only, no chat history)
              ├── APPROVED           → next task
              └── CHANGES REQUESTED  → back to builder → re-review
```

Rules baked into the agent configs (apply these even when not literally invoking the
subagents):
- Simplest thing that works: JPA with `ddl-auto: update` (no Flyway/migrations), a
  single JWT access token in localStorage (no refresh tokens), images on local disk
  served as static files, DTO shortcuts allowed.
- Two things are **not optional** despite the "speed over polish" mandate: every list
  endpoint is paginated, and every write endpoint checks the caller owns the resource
  they're modifying.
- Reviewer checks exactly four things (does it do what the task says, can a user modify
  someone else's data, any committed secrets/injection/unvalidated input, does the app
  still start) and outputs `APPROVED` or `CHANGES REQUESTED` + blocking issues only,
  max 10 lines — no style nits, no full code review.
- The agent that implements a task never marks it "Done" itself; review must run in a
  separate/fresh context from the implementation.

## Env / config

- `.env` / `.env.example` define Postgres creds, `BACKEND_PORT`, `JWT_SECRET` (dev-only
  placeholder), `FRONTEND_PORT`, and `NEXT_PUBLIC_API_BASE_URL`. `.gitignore` excludes
  `.env` (but not `.env.example`), `backend/target/`, `frontend/node_modules/`, and
  `frontend/.next/`.
- `.env` currently defines `POSTGRES_PORT` twice (`5432`, then `55432` at the end) —
  the second assignment wins, so the effective host port is **55432**, not the `5432`
  shown in `.env.example`. Worth resolving/consolidating in T1 rather than propagating
  the duplicate into docker-compose.
- `NEXT_PUBLIC_API_BASE_URL` is a **build-time** var: Next.js inlines `NEXT_PUBLIC_*`
  values at `next build`, and docker-compose passes it as a build ARG. Changing it
  requires `docker compose up -d --build` (or `docker compose build frontend`) — a
  plain `docker compose up` reuses the already-built image's baked-in value. (This
  comment lives in `.env` and references `docs/local-dev.md`, which doesn't exist yet.)
- `.claude/settings.local.json` also pre-allows `curl http://localhost:8080/actuator/health`
  and `curl http://localhost:3000/` — Spring Boot Actuator's health endpoint is expected
  to be enabled as the backend smoke-test hook for T1/T6.
