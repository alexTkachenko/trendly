# OPS-2.1 — Backend CI workflow

**Epic:** E-OPS-2 — CI pipeline
**Owning agent:** devops-agent
**Depends on:** OPS-1.1, BE-1.1
**Size:** M

## Description
GitHub Actions workflow that builds and tests the backend on every push
and PR (GitHub Actions because no other CI tool is in use here).

## Scope
- `.github/workflows/backend-ci.yml`, triggered on `push` and
  `pull_request`; path-filter to `backend/**` + the workflow file itself.
- Steps: checkout → `actions/setup-java` (Temurin 21, Maven cache) →
  `./mvnw -B verify`.
- Postgres 16 **service container** with the same DB name/user as
  `docker-compose.yml`, exposed to the job via the datasource env vars
  BE-1.1 reads. There may be no integration tests yet — wire it correctly
  anyway so the first one that lands just works.

## Acceptance criteria
- [x] `.github/workflows/backend-ci.yml` exists and runs green on a PR.
- [x] A deliberate compile error or failing test turns the run red
      (verify by actually breaking something locally/on a scratch branch,
      or state why that wasn't possible and what was checked instead).
- [x] Postgres service container is reachable from the build job (a
      trivial connectivity check is acceptable evidence while no
      integration tests exist).

## Implementation notes
- `.github/workflows/backend-ci.yml`: triggers on `push`/`pull_request` to
  `main`, path-filtered to `backend/**` + the workflow file (matches the
  existing `frontend-ci.yml` pattern). Steps: checkout ->
  `actions/setup-java@v4` (Temurin 21, Maven cache keyed on
  `backend/pom.xml`) -> a Postgres connectivity check -> `./mvnw -B verify`.
- Postgres 16 (`postgres:16-alpine`) service container with
  `POSTGRES_DB=trendly` / `POSTGRES_USER=trendly`, matching
  `docker-compose.yml`; a CI-only password (`trendly_ci_password`, not a
  real secret) since GH Actions services can't read repo secrets into
  their own `env:` block. `SPRING_DATASOURCE_*` job env vars point
  `./mvnw verify` at `localhost:5432` (service container ports are
  published directly to the runner host). `JWT_SECRET` is set to a
  CI-only placeholder since `application.yml` fails fast at context
  startup if it's unbound.
- Not GitHub-runnable from here, so validated instead by: (1)
  `actionlint` via `docker run --rm -v $(pwd):/repo -w /repo
  rhysd/actionlint:latest .github/workflows/backend-ci.yml` — clean,
  exit 0, no findings (sanity-checked the tool itself catches real YAML
  errors by breaking a copy of the file first); (2) ran the exact
  `./mvnw -B verify` command locally against a real
  `postgres:16-alpine` container provisioned with the same DB
  name/user/env vars the workflow uses, confirming the connectivity
  check (`pg_isready` + `psql -c "SELECT 1;"`) and the build command
  both work end-to-end; (3) red-build check: introduced a deliberate
  Java syntax error in `TrendlyApplication.java`, reran the same
  `./mvnw -B verify` command, confirmed `BUILD FAILURE` with a
  non-zero exit code (verified explicitly, not just via log text),
  then reverted the file (confirmed no residual diff).
- Found and flagging, NOT fixed here (out of scope for devops-agent —
  application/test code): `./mvnw -B verify` currently fails on
  `main`'s backend as of this task, independent of any CI wiring.
  `GlobalExceptionHandlerTest` (`@WebMvcTest(controllers =
  ThrowawayTestController.class)` with `@AutoConfigureMockMvc(addFilters
  = false)`) fails ApplicationContext startup with
  `UnsatisfiedDependencyException` on `jwtAuthenticationFilter` ->
  `JwtService`: the `@WebMvcTest` slice picks up
  `JwtAuthenticationFilter` (a `Filter` bean, included by the slice's
  default filters) but not its constructor dependency `JwtService`
  (a plain `@Component`, excluded from the slice), even though
  `addFilters = false` disables filter *invocation*, not bean
  *creation*. This will make the first PR run of this workflow go red
  for a real reason. Needs a backend-agent fix (e.g. `@MockBean
  JwtService` in the test, or excluding the filter from this slice) —
  raised here as the reviewer/backend follow-up.

**Status:** Done
