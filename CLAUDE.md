# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current state

This repo is pre-implementation: `backend/` and `frontend/` contain only `.gitkeep`
placeholders, and there is no `docker-compose.yml` yet. What exists today is the plan
and env scaffolding. There are no build/lint/test commands to run until the
corresponding setup task (T1 in `docs/MVP_PLAN.md`, or OPS-1.1/BE-1.1/FE-1.1 in
`docs/PROJECT_PLAN.md`) has been done — don't assume a Maven/Next.js project layout
exists until you've checked.

Once the backend exists, it's a Maven-based Spring Boot app (`.claude/settings.local.json`
pre-allows `./mvnw compile` and `./mvnw test -Dtest=<ClassName>`), started via
`docker compose up`.

## Project

Trendly — MVP social network: register, post (text + image), follow, feed, comment.
Stack: **Spring Boot** (backend) · **PostgreSQL** (database) · **Next.js** (frontend).

Two planning docs, at two different scopes — check which one the current task belongs to:

- `docs/MVP_PLAN.md` — the active plan: 6 sequential tasks (T1–T6, ~2.5h) to a bare
  working demo. Cut from scope: Flyway, CI/CD, tests beyond smoke, likes, refresh
  tokens, S3, profile editing, a11y/mobile polish, API docs. **This is what "the MVP"
  means in this repo** — don't pull in PROJECT_PLAN.md scope unless asked.
- `docs/PROJECT_PLAN.md` — the full post-MVP plan, kept for later. Epics/tasks (e.g.
  `BE-2.1`, `DB-2.3`) referenced elsewhere refer to IDs in this file's section 4 table.

`docs/brainstorming/branding.md` has name/tagline exploration — not binding, reference only.

## Working model: builder / reviewer split

Every task (from either plan) goes through the same lifecycle:

```
Backlog → In Progress (implementer) → Ready for Review
        → reviewer, fresh context → APPROVED → Done
                                   → CHANGES REQUESTED → back to In Progress → re-review
```

- The agent that implements a task never marks it "Done" itself.
- A review must run in a **fresh/separate context** from the implementation — it sees
  only the task description and the diff, not the chat history that produced it. If
  you just implemented something, don't also review it in the same session.
- Per user preference, review is scoped to functional/security/data-loss issues only
  (not style/process nitpicks), capped at 2 review rounds — this is an MVP, not a
  production hardening pass.
- `docs/PROJECT_PLAN.md` section 1 names an agent per domain (architect/backend/
  frontend/database/devops/reviewer) and says to drop configs into `.claude/agents/`
  — that directory doesn't exist yet in this repo; recreate agents only if actually
  asked to follow that multi-agent structure, otherwise just implement tasks directly.

## Env / config

- `.env` (already present, gitignored pattern not yet set up — check before committing
  secrets) defines Postgres creds, `BACKEND_PORT`, `JWT_SECRET` (dev-only placeholder),
  and `NEXT_PUBLIC_API_BASE_URL`.
- `NEXT_PUBLIC_API_BASE_URL` is a **build-time** var: Next.js inlines `NEXT_PUBLIC_*`
  values at `next build`, and docker-compose passes it as a build ARG. Changing it
  requires `docker compose up -d --build` (or `docker compose build frontend`) — a
  plain `docker compose up` reuses the already-built image's baked-in value.
