# BE-1.1 — Initialize Spring Boot skeleton

**Epic:** E-BE-1 — Backend project setup & security skeleton
**Owning agent:** backend-agent
**Depends on:** OPS-1.1, DB-1.1, DB-1.2
**Size:** M

## Description
Replace the placeholder Maven project under `backend/` with a real,
buildable Spring Boot skeleton: builds, connects to Postgres, runs
Flyway migrations on startup, exposes a real health check. No business
logic, no auth, no error handling — those are separate tasks (BE-1.2,
BE-1.3).

## Scope
- Spring Boot (current stable), Java 21, **Maven** (`pom.xml` + wrapper).
  Starters: `web`, `data-jpa`, `security`, `validation`, `actuator`,
  `flyway-core` + `flyway-database-postgresql`, Postgres JDBC driver.
- Datasource via env vars (relaxed binding, matches `docker-compose.yml`):
  `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`. No hardcoded
  credentials. Also bind `JWT_SECRET` to a property (not consumed yet).
- `spring.flyway.enabled=true`, `spring.jpa.hibernate.ddl-auto=validate`
  — Flyway is the only schema writer.
- `/actuator/health` reachable without auth (security starter is on the
  classpath, so it needs an explicit permit rule).
- Target Postgres 16 / db `trendly` per `docs/db/conventions.md`. Port
  8080 (matches compose).
- Remove the placeholder `backend/pom.xml` + `backend/src/` entirely.

## Acceptance criteria
- [x] `./mvnw package` builds a runnable JAR with no manual setup beyond a JDK.
- [x] App starts against the OPS-1.1 Postgres service via the env-var
      datasource config, and Flyway auto-applies
      `db/migration/V1__init.sql` on startup (logs the version).
- [x] `GET /actuator/health` returns 200 `UP` (incl. DB health), no auth
      required.
- [x] `ddl-auto` is `validate`/`none`, never `update`/`create`.

## Notes for implementer
- `backend/pom.xml`/`backend/src/` currently hold a throwaway
  placeholder (from OPS-1.2, used only to verify the Dockerfile) —
  replace it wholesale.
- `spring-boot-maven-plugin` defaults to `<artifactId>-<version>.jar`;
  OPS-1.2's Dockerfile expects `app.jar` — set `<finalName>app</finalName>`
  or flag OPS-1.2 to adjust.

## Implementation notes

- **Build tool confirmed: Maven**, per scope. `backend/pom.xml` and
  `backend/src/` (the OPS-1.2 placeholder, plain `HttpServer` + shade
  plugin) were deleted wholesale and replaced with a real Spring Boot
  project: `groupId com.trendly`, `artifactId backend`, parent
  `org.springframework.boot:spring-boot-starter-parent:3.5.16` (latest
  stable 3.x per CLAUDE.md/backend-agent conventions — 4.x exists upstream
  but is out of scope here), Java 21. Starters: `web`, `data-jpa`,
  `security`, `validation`, `actuator`, plus `flyway-core` +
  `flyway-database-postgresql` and the `postgresql` JDBC driver
  (runtime scope), all with versions managed by the Spring Boot BOM (no
  explicit version pins needed/added). `spring-boot-maven-plugin` sets
  `<finalName>app</finalName>` so `target/app.jar` matches what OPS-1.2's
  Dockerfile already copies — **no change needed to OPS-1.2's Dockerfile**,
  confirmed by a real `docker compose build backend` (see below).
- **Maven wrapper**: no system `mvn` was available in this environment, so
  the wrapper (`mvnw`/`mvnw.cmd`/`.mvn/wrapper/`) was generated via
  `docker run maven:3.9-eclipse-temurin-21 mvn -N wrapper:wrapper
  -Dmaven=3.9.9` (same Maven image OPS-1.2's Dockerfile build stage uses),
  then committed. `./mvnw -v` and `./mvnw package` both verified working
  from a clean host afterward.
- **Code added**: `com.trendly.TrendlyApplication` (`@SpringBootApplication`
  entry point) and `com.trendly.config.SecurityConfig` — a minimal
  `SecurityFilterChain` that permits `/actuator/health` (and
  `/actuator/health/**`) unauthenticated, stateless session policy, CSRF
  disabled, and `anyRequest().authenticated()` for everything else (there
  is nothing else yet). No JWT filter, no business logic — out of scope
  per this task, deferred to BE-1.2/BE-1.3.
- **`application.yml`**: datasource `url`/`username`/`password` bound from
  `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD` env vars (relaxed
  binding, no hardcoded credentials), `spring.jpa.hibernate.ddl-auto:
  validate`, `spring.flyway.enabled: true` (default `classpath:db/migration`
  location, unchanged), `server.port: 8080`, `management.endpoints.web
  .exposure.include: health`. `JWT_SECRET` bound to `app.jwt.secret`
  (fails fast if missing; not consumed by any code yet, per scope).
- **Verification performed** (all resources cleaned up afterward):
  1. `./mvnw -B package` — `BUILD SUCCESS`, produced `target/app.jar`.
  2. Started `postgres` via `docker compose up -d postgres` (root
     `.env` copied from `.env.example`, `POSTGRES_PORT` bumped to `55432`
     for this session only — port `5432` was already bound by an
     unrelated local container). Ran `java -jar target/app.jar` directly
     with the same env vars `docker-compose.yml` passes to `backend`
     (translated to `localhost:55432`). Logs confirmed: Flyway connected,
     validated, and migrated schema `public` to `version "1 - init"`
     (`Successfully applied 1 migration ... now at version v1`); Hibernate
     then validated the (empty) schema successfully at
     `ddl-auto: validate` — no `update`/`create` DDL executed anywhere.
  3. `curl http://localhost:8080/actuator/health` (no `Authorization`
     header) → `HTTP 200`, body `{"status":"UP"}`. Confirmed with the
     dev-only Spring Security generated password that the `db` component
     feeding that aggregate status is present and `UP`
     (`{"db":{"status":"UP",...}}`), so the `UP` result genuinely reflects
     DB connectivity, not just app liveness.
  4. `docker compose build backend` — builds clean against the real
     `pom.xml`/`src`, no Dockerfile changes required.
  5. `docker compose up -d postgres backend` (full compose path, in-network
     `postgres:5432` hostname) — `backend` container reported `healthy`
     via its own `wget /actuator/health` healthcheck; a second Flyway run
     against the same already-migrated DB logged `Schema "public" is up
     to date. No migration necessary.`, confirming idempotency end-to-end
     through the real compose wiring (not just the standalone Flyway CLI
     check from DB-1.2).
  6. Cleanup: `docker compose down -v` (removed containers, `trendly_net`,
     `trendly_pgdata`), `docker image rm trendly-backend`, deleted the
     session-local root `.env`. Nothing left running.
- **Deviations from the task file**: none. `finalName app` was applied as
  suggested in "Notes for implementer" rather than flagging OPS-1.2 — no
  Dockerfile change was necessary, confirmed by the Docker build above.
- **No unit/integration tests added**: this task is infrastructure/
  skeleton only (no business logic, no endpoints beyond the
  auto-configured `/actuator/health`), consistent with the task's explicit
  "no business logic, no auth, no error handling" scope — verification was
  done via the manual startup/curl/compose checks above instead. The first
  real endpoint task (BE-1.2+) should add the project's first
  `@SpringBootTest`/`@WebMvcTest`.

**Status:** Done
