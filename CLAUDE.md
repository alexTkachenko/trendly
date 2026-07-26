# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Trendly — a social network for fashion discovery (post, follow, comment, trending feed).
Stack: Spring Boot (`backend/`) · PostgreSQL · Next.js (`frontend/`).

**Current state: pre-implementation.** `backend/` and `frontend/` are empty
(`.gitkeep` only) — no build, lint, or test commands exist yet because no
application code has been written. This repo currently holds planning
artifacts (epics/tasks, ADRs-to-be, API/DB contracts-to-be) and the
multi-agent workflow that will produce the code. Once `backend/` or
`frontend/` gain real projects, add their actual build/lint/test commands
here — check `tasks/backend/BE-1.1.md` and `tasks/frontend/FE-1.1.md` for
the setup acceptance criteria that determine what those commands will be
(Maven/Gradle for backend; npm/pnpm scripts for frontend).

Local stack is meant to run via `docker compose up` after `cp .env.example .env`
(see `.env.example` for the required vars: Postgres creds, `BACKEND_PORT`,
`JWT_SECRET`, `FRONTEND_PORT`, `NEXT_PUBLIC_API_BASE_URL`).

## Repo structure

```
docs/            Planning docs, ADRs, API contracts, DB conventions/schema
  PROJECT_PLAN.md    Full epic → task breakdown, dependencies, build order
  adr/               Architecture Decision Records (Context/Decision/Consequences)
  api/               REST endpoint contracts (must exist before backend/frontend
                     implement a feature)
  db/                Schema doc + naming/typing conventions
tasks/           One markdown file per task, grouped by direction
  backend/ frontend/ database/ devops/
backend/         Spring Boot application (not yet initialized)
frontend/        Next.js application (not yet initialized)
.claude/agents/  Specialized subagent configs — see below
```

Task files follow a fixed shape: Description, Scope, Acceptance criteria
(checklist), Notes for implementer. Read the linked ADR/contract/schema doc
before implementing — task files intentionally don't restate decisions that
live in `docs/`.

## MVP mode

This project is an MVP that needs to ship fast — optimize for working
software over exhaustive polish. Concretely: keep task files short (2-4
acceptance criteria, no edge-case enumeration); reviewer-agent blocks
only on unmet acceptance criteria, broken builds/migrations, security
holes, or data loss — not on cross-doc wording consistency, naming
pedantry, or theoretical improvements; two review rounds is the
practical ceiling per task, approve on the second pass unless something
is genuinely broken. See `.claude/agents/reviewer-agent.md` and
`.claude/agents/architect-agent.md` for the full detail.

## Multi-agent workflow

This repo is designed to be worked on through named subagents in
`.claude/agents/`, each scoped to one layer of the stack. Read the relevant
agent file before doing that kind of work even outside a subagent
invocation — they encode the binding stack conventions:

| Agent | Owns | Key conventions to know |
|---|---|---|
| `architect-agent` | Epics/tasks, API contracts, schema design, ADRs — no code | Writes ADRs to `docs/adr/NNN-title.md`; defines `docs/api/` and `docs/db/schema.md` *before* backend/frontend/database agents build against them |
| `backend-agent` | Spring Boot code | Spring Boot 3.x / Java 21; Controller→Service→Repository→Entity, no business logic in controllers, no repo calls from controllers; DTOs only over the API (never expose entities); Bean Validation on DTOs; JWT stateless security unless an ADR says otherwise; `@ControllerAdvice` for errors; `Pageable` on every list endpoint; Flyway migrations only, never `ddl-auto: update` |
| `frontend-agent` | Next.js code | App Router + TypeScript; typed API client against `docs/api/` contracts (never hand-rolled untyped fetches); Tailwind; loading/empty/error states on every data-fetching screen; JWT storage per the auth ADR (not naive localStorage) |
| `database-agent` | Postgres schema & migrations | Versioned Flyway migrations (`V{n}__description.sql`), never edit an applied migration; snake_case, plural table names; every table gets `id`, `created_at`, `updated_at`; explicit `ON DELETE` behavior per FK, documented in the migration header; DB-level constraints (uniques, checks) not just app-level; index every FK and every named query pattern |
| `devops-agent` | Docker, CI/CD, environments | `docker-compose.yml` with health checks + named volumes; multi-stage Dockerfiles; CI order is lint → unit → build → integration (service-container Postgres) → image build; env vars only for secrets/config, documented in `.env.example` |
| `reviewer-agent` | Independent review of every task | **Must run in a fresh context with zero memory of the implementation session** — sees only the task file, the linked contract/schema doc, and the diff, like a cold PR review |

### Task lifecycle

```
Backlog → In Progress (owning agent) → Ready for Review
        → reviewer-agent review (clean context)
              ├── APPROVED           → Done
              └── CHANGES REQUESTED  → back to In Progress → re-review
```

Hard rules:
- No agent marks its own work "Done" — only `reviewer-agent`, and only after
  review. Implementers set status to "Ready for review", never "Done".
- `reviewer-agent` must be invoked as a separate session/context from
  whoever implemented the task — never chain it onto the same conversation.
- `architect-agent` is the only one that writes ADRs and updates
  `PROJECT_PLAN.md`'s status column after a review verdict.
- Cross-layer decisions affecting data model or public API shape (auth
  strategy, image storage, ID scheme, feed fan-out strategy — see
  `docs/PROJECT_PLAN.md` §6 for the open ones) must be resolved via
  architect-agent/ADR before backend, frontend, or database agents build
  against them — don't guess.

Build order for epics is documented in `docs/PROJECT_PLAN.md` (§3); it
generally goes OPS-1 → DB-1 → DB-2 → BE-1 → ... — check there before
picking up out-of-order work, since later epics assume earlier ones' schema
and contracts already exist.
