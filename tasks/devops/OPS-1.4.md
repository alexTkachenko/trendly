# OPS-1.4 — `.env.example` files

**Epic:** E-OPS-1 — Local dev environment
**Owning agent:** devops-agent
**Depends on:** OPS-1.1
**Size:** S

## Description
Provide `.env.example` files documenting every environment variable the
stack needs, so a new contributor can `cp .env.example .env` and be
running in one step (after filling in nothing, or trivial local values).

## Scope
- Root `.env.example` (consumed by `docker-compose.yml`):
  - `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
  - `JWT_SECRET` (placeholder value clearly marked "dev only, do not use in prod")
  - `NEXT_PUBLIC_API_BASE_URL`
  - Any port overrides (`POSTGRES_PORT`, `BACKEND_PORT`, `FRONTEND_PORT`)
- `backend/.env.example` if the backend also reads a local `.env` outside
  compose (e.g. for running it directly from an IDE).
- `frontend/.env.example` likewise for local `next dev` runs outside
  Docker.
- `.gitignore` entries ensuring real `.env` files are never committed.

## Acceptance criteria
- [x] Every variable referenced in `docker-compose.yml`, backend config,
      and frontend config has a corresponding entry in some
      `.env.example` file.
- [x] `.env` (real, filled-in file) is gitignored at every level where an
      example exists.
- [x] `docs/local-dev.md` (from OPS-1.1) references these files and
      explains the copy step.
- [x] No real secrets, tokens, or credentials appear anywhere in
      `.env.example` — placeholder values only.

## Notes for implementer
- Keep this file in sync as other tasks introduce new config (e.g. image
  storage credentials if S3-compatible storage is chosen per the open
  decision in PROJECT_PLAN.md section 6). Treat drift here as a review
  finding on future tasks.

## Implementation notes (devops-agent, 2026-07-25)

Reconciled this task against everything that landed since it was written
(OPS-1.1/1.2/1.3, root `.env.example`, `frontend/.env.example`). Root
`.env.example` and `frontend/.env.example` already existed and needed no
structural changes; the actual gap was the missing `backend/.env.example`.

- **Created `backend/.env.example`** — documents `SPRING_DATASOURCE_URL`,
  `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`
  for running the backend directly on the host (IDE, `./mvnw
  spring-boot:run`, `java -jar`) outside Docker/compose. Values mirror the
  root file (Postgres 16, db `trendly`, user `trendly`, same dev-only JWT
  placeholder) with the one intentional difference being the datasource
  host: `localhost:5432` instead of the compose network's `postgres:5432`,
  since a host-run backend isn't attached to `trendly_net`. Also documents
  `JDK_JAVA_OPTIONS` (a real JDK env var, honored by any `java` launch —
  relevant here) vs `JAVA_OPTS` (Docker-entrypoint-only, `backend/Dockerfile`
  shell-expands it; explicitly noted as *not* applicable to a host run, so
  it's mentioned but not set).
- **Audited `docker-compose.yml` for var coverage**: every var it actually
  reads (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`,
  `POSTGRES_PORT`, `BACKEND_PORT`, `JWT_SECRET`, `FRONTEND_PORT`,
  `NEXT_PUBLIC_API_BASE_URL`) already has a root `.env.example` entry — no
  gap found there. `SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD` are
  *constructed* inside `docker-compose.yml` from `POSTGRES_*` rather than
  read as their own root env vars (correct for the compose path — the DB
  host differs by definition between compose and host-run, so they
  couldn't share one literal value anyway), which is why they only need
  documenting in `backend/.env.example`, not the root file.
- **`JAVA_OPTS`/`JDK_JAVA_OPTIONS` in `docker-compose.yml`**: confirmed
  neither is actually wired into the `backend` service's `environment:`
  block (`docs/local-dev.md`'s "Backend JVM options" section already said
  so accurately) — they remain Docker-image-level knobs
  (`docker run -e JAVA_OPTS=...`), not compose/`.env`-driven, and are out
  of scope to wire in here since the task didn't ask for it and no gap
  exists (documented correctly, just not plumbed through compose).
- **`.gitignore` audit**: `.env` / `*.env` / `!*.env.example` at the repo
  root are **not path-anchored** (no leading `/`, no `/` in the middle),
  so per gitignore semantics they already match `.env` files at *any*
  depth, including `backend/.env` and `frontend/.env` — no new entries
  needed. Verified empirically (see below), not just by reading the
  pattern.
- **`docs/local-dev.md`**: added a new "Running services outside Docker"
  section pointing at both `backend/.env.example` and
  `frontend/.env.example`, explaining they're for host-run dev only (never
  read by `docker-compose.yml`), with the datasource-host caveat and a
  cross-reference to the existing "Backend JVM options" / "Frontend
  build-time config" sections.
- **Secrets check**: grepped both new/touched files and all `.env.example`
  files for anything beyond placeholders — only the existing dev-only
  `JWT_SECRET` placeholder (already clearly marked, reused verbatim from
  the root file) and the dev-only Postgres password (`trendly_dev_password`,
  already in the root file, unchanged) appear anywhere. No new secrets
  introduced.

### Verification performed
- `git check-ignore -v .env backend/.env frontend/.env` after `touch`-ing
  all three (and separately after `cp`-ing each real `.env.example` to
  `.env`): all three resolved to `.gitignore:3:*.env`, confirming the
  existing root-level patterns already cover every level with an example
  file — no `.gitignore` changes were necessary.
- Root regression check: `cp .env.example .env`, `docker compose config`
  validated, then `docker compose up -d --build`. Hit the same pre-existing
  local port conflict on host `5432` noted in OPS-1.1/1.2/1.3 (unrelated
  container already bound to it on this dev machine); overrode
  `POSTGRES_PORT=15432` in `.env` only (no code changes) and re-ran. All
  three services (`postgres`, `backend`, `frontend`) came up `healthy`;
  `curl localhost:8080/actuator/health` → `{"status":"UP"}`; `curl -o
  /dev/null -w '%{http_code}' localhost:3000/` → `200`; `psql
  "postgresql://trendly:trendly_dev_password@localhost:15432/trendly" -c
  "select current_database(), version();"` returned `trendly` /
  `PostgreSQL 16.14`. Confirms this task's changes (additive `.env.example`
  + docs only) didn't regress the compose stack.
- Full teardown: `docker compose down -v --remove-orphans`, removed the
  locally built `trendly-backend`/`trendly-frontend` images, deleted the
  test `.env`/`backend/.env`/`frontend/.env` copies used for the
  `git check-ignore` and compose checks. `docker ps -a` / `docker images`
  confirmed no leftover `trendly-*` resources; `git status` shows only the
  intended new/modified files (`backend/.env.example`, `docs/local-dev.md`,
  this task file).

### Files changed (round 1)
- `backend/.env.example` (new)
- `docs/local-dev.md` (added "Running services outside Docker" section)
- No changes to root `.env.example`, `frontend/.env.example`,
  `docker-compose.yml`, or `.gitignore` — audited and confirmed already
  correct/complete for this task's scope.

**Note:** two statements in the round-1 notes above turned out to be
wrong — see "Round 2 review fixes" immediately below for the corrections
and what actually changed as a result. Left the round-1 text as-is (rather
than silently editing it) so the history of what was claimed and why it
was wrong stays visible.

### Round 2 review fixes (reviewer-agent: CHANGES REQUESTED)

- **S1 (should-fix) — `docs/local-dev.md` factually wrong about Next.js
  `.env` loading.** Round 1 claimed "neither Spring Boot nor a plain `npm
  run dev` reads `.env` files automatically." The Spring Boot half is
  correct; the Next.js half is not — Next.js has had built-in dotenv
  support since v9.4, and `frontend/package.json` pins `next@14.2.35`.
  Reviewer proved it empirically: with only `frontend/.env` present (no
  exported var), `next build` auto-loaded it and inlined the value into
  the bundle.
  - Fix: rewrote the closing paragraph of "Running services outside
    Docker" in `docs/local-dev.md` to split the guidance by app —
    frontend: `cp frontend/.env.example frontend/.env` is sufficient on
    its own (Next.js auto-loads `.env`/`.env.local`, no export/IDE config
    needed) for both `npm run dev` and `npm run build`; backend: still
    requires an explicit shell export / IDE run configuration / dotenv
    plugin, since Spring Boot has no built-in `.env` loading.
  - Re-verified: read back the updated section; the frontend guidance now
    matches the reviewer's empirical finding and the backend guidance is
    unchanged (it was already correct).
- **B1 (blocking) — `.gitignore` didn't cover `.env.local` / other
  variants, a real gap not a nit.** Round 1's audit concluded the existing
  `.env` / `*.env` patterns were sufficient because they're unanchored and
  match at any depth — true, but incomplete: neither pattern matches
  `.env.local` (or `.env.production`, `.env.development.local`, etc.)
  because those filenames don't *end* in the literal substring `.env`.
  `.env.local` is the canonical Next.js local-override file (takes
  precedence over `.env`; `frontend/.dockerignore` already listed it,
  confirming the convention was expected here even though root
  `.gitignore` didn't cover it). Reviewer proved with `git check-ignore`
  that `.env.local`, `backend/.env.local`, `frontend/.env.local`,
  `frontend/.env.development.local`, and `.env.production` were all NOT
  ignored and would be picked up by `git add`/`git status`.
  - Fix: broadened the root `.gitignore` env pattern from `.env` / `*.env`
    to a single `.env*` (keeping `!*.env.example` unchanged), which
    matches any filename starting with the literal `.env` — covers
    `.env`, `.env.local`, `.env.production`, `.env.*.local`, etc. — while
    the existing negation continues to un-ignore every `*.env.example`
    template. Still unanchored, so it applies at every depth (root,
    `backend/`, `frontend/`) with a single rule.
  - Re-verified (see below) with `git check-ignore -v` and `git add -n`
    across root/backend/frontend for `.env`, `.env.local`,
    `.env.production`, and `.env.development.local`, plus confirmed all
    three `.env.example` files remain addable/committable after the
    change.

**Optional nits also addressed (cheap):**
- `backend/.env.example`: added a comment directly above
  `SPRING_DATASOURCE_URL` noting the hardcoded `:5432` in the example URL
  assumes the default `POSTGRES_PORT` and should be adjusted to match if
  that was overridden in the root `.env`.
- `backend/.dockerignore`: added `.env` / `.env.local` entries, matching
  the convention `frontend/.dockerignore` already followed (excludes any
  local env file from the Docker build context, belt-and-suspenders on
  top of `.gitignore` since Docker build context isn't governed by
  `.gitignore`).

### Verification performed (round 2)
- `git check-ignore -v` across root/backend/frontend for `.env`,
  `.env.local`, `.env.production`, `.env.development.local` (11 paths
  total): all resolved to `.gitignore:6:.env*` (line number shifted after
  adding a comment above the pattern) — confirmed every real env-file
  variant is now ignored at every level, not just the bare `.env` name.
- `git add -n` (dry run) on the same 11 real-file paths plus all three
  `.env.example` files: git refused the 8 real `.env*` paths ("ignored by
  one of your .gitignore files"), while all three `.env.example` files
  were accepted for staging — confirming the negation still works and
  nothing legitimate got swept up by the broader `.env*` pattern.
- Root regression re-check: `cp .env.example .env`, overrode
  `POSTGRES_PORT=15432` (same pre-existing local port conflict as round 1
  and prior OPS-1.x tasks), `docker compose up -d --build`. All three
  services (`postgres`, `backend`, `frontend`) came up `healthy`; `curl
  localhost:8080/actuator/health` → `{"status":"UP"}`; `curl -o /dev/null
  -w '%{http_code}' localhost:3000/` → `200`. Confirms the `.gitignore` /
  `.dockerignore` / docs changes in this round didn't regress the compose
  stack (a broader `.gitignore` doesn't affect what's already tracked, and
  the `.dockerignore` addition only excludes files that were never present
  in the build context to begin with).
- Full teardown: `docker compose down -v --remove-orphans`, removed the
  locally built `trendly-backend`/`trendly-frontend` images, deleted the
  test `.env` and all scratch env-variant files created for the
  `git check-ignore`/`git add -n` checks. `docker ps -a` / `docker images`
  confirmed no leftover `trendly-*` resources; `git status` shows only the
  intended changed files.

### Files changed (round 2, additional to round 1)
- `.gitignore` (broadened `.env` / `*.env` → `.env*`, kept
  `!*.env.example`)
- `docs/local-dev.md` (corrected the Next.js `.env`-loading claim, split
  frontend vs. backend guidance)
- `backend/.env.example` (nit: port-override comment)
- `backend/.dockerignore` (nit: added `.env` / `.env.local`)

**Status:** Ready for review
