# IN PROGRESS! RELEVANT VERSION IN MVP BRANCH! 

# Trendly

A social network for fashion discovery — post, follow, comment, and see
what's trending in real time.

Stack: Spring Boot (backend) · PostgreSQL (database) · Next.js (frontend)

## Getting started

1. `cp .env.example .env` and fill in local values.
2. `docker compose up` — brings up Postgres, backend, and frontend.
3. See `docs/PROJECT_PLAN.md` for the full epic/task breakdown and
   `docs/db/conventions.md` for schema conventions.

## Project structure

```
docs/            Planning, ADRs, API contracts, DB conventions
tasks/           One markdown file per task, grouped by direction
backend/         Spring Boot application
frontend/        Next.js application
.claude/agents/  Specialized agent configs (architect, backend, frontend,
                 database, devops, reviewer) — see docs/PROJECT_PLAN.md
                 section 1 for how to use them.
```

## Workflow

Every task moves: Backlog → In Progress → Ready for Review →
reviewer-agent (fresh context) → Done. See `docs/PROJECT_PLAN.md` section
2 for the full lifecycle and `.claude/agents/reviewer-agent.md` for how
review works.

Suggested first tasks: `tasks/devops/OPS-1.1.md` and
`tasks/database/DB-1.1.md`.
