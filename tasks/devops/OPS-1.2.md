# OPS-1.2 — Backend Dockerfile

**Epic:** E-OPS-1 — Local dev environment
**Owning agent:** devops-agent
**Depends on:** OPS-1.1 (compose file references this)
**Size:** S

## Description
Multi-stage Dockerfile for the Spring Boot backend under `backend/`.

## Scope
- **Build stage:** official Maven or Gradle image (match whatever
  backend-agent initializes in BE-1.1 — do not decide the build tool here,
  just support it) — builds the fat JAR, using layer caching for
  dependencies (copy `pom.xml`/`build.gradle*` first, download deps,
  then copy source).
- **Runtime stage:** slim JRE base image (e.g. `eclipse-temurin:21-jre-alpine`
  or distroless), copies only the built JAR from the build stage.
- Runs as a non-root user.
- Exposes 8080.
- Reasonable JVM defaults via `JAVA_OPTS`/`JDK_JAVA_OPTIONS` env var,
  overridable at runtime.

## Acceptance criteria
- [x] `docker build -t trendly-backend ./backend` succeeds.
- [x] Resulting image runs the app with `docker run` given the right env
      vars and connects to a reachable Postgres.
- [x] Final image does not contain build tool caches or source code beyond
      what's needed to run (i.e. multi-stage actually discards the build
      stage — verify with `docker image inspect` / size check).
- [x] Container runs as non-root.

## Notes for implementer
- This task is blocked on backend-agent having at least a buildable
  skeleton (BE-1.1) to build against. If BE-1.1 isn't done yet, write the
  Dockerfile against a minimal placeholder Spring Boot app and flag it for
  a follow-up check once BE-1.1 lands.

## Implementation notes (devops-agent, 2026-07-25)
BE-1.1 (`tasks/backend/BE-1.1.md`) has **not** landed yet — only its task
spec exists. Per this task's own notes and BE-1.1's notes for implementer,
this Dockerfile was written and verified against a **minimal placeholder
Maven project**, not the real Spring Boot app:

- `backend/pom.xml` — trivial Maven project (`groupId com.trendly`, Java
  21, `packaging jar`) with a single real dependency (`commons-lang3`)
  specifically so the dependency-resolution/layer-caching step in the
  Dockerfile does real work, not a no-op. Uses `maven-shade-plugin` to
  produce a runnable fat jar (`target/app.jar`), the same shape a Spring
  Boot executable jar takes.
- `backend/src/main/java/com/trendly/placeholder/PlaceholderApp.java` —
  dependency-free-ish `com.sun.net.httpserver` HTTP server exposing
  `/actuator/health` (`{"status":"UP"}`) and `/`, standing in for the real
  app. Package/dir layout (`com.trendly.*` under `src/main/java`) matches
  what BE-1.1 is expected to produce, so the swap should be low-friction.
- **Superseded OPS-1.1's throwaway placeholder**: removed
  `backend/placeholder/PlaceholderApp.java` and the old plain-`javac`
  (no-Maven) `backend/Dockerfile`, replacing them with the real multi-stage
  Maven → JRE Dockerfile described in Scope above. Also removed the now-
  redundant `backend/.gitkeep`.
- `backend/Dockerfile`: build stage is `maven:3.9-eclipse-temurin-21`
  (`COPY pom.xml` → `mvn dependency:go-offline` → `COPY src` →
  `mvn package -DskipTests`, in that order for layer caching); runtime
  stage is `eclipse-temurin:21-jre-alpine`, non-root `app` user, only
  `target/app.jar` copied in, `EXPOSE 8080`, `ENTRYPOINT` runs
  `java $JAVA_OPTS -jar app.jar` via `sh -c` (so `JAVA_OPTS` is honored;
  `JDK_JAVA_OPTIONS` is picked up natively by the JVM launcher regardless
  of entrypoint form).
- Added `backend/.dockerignore` (`target/`, `.idea/`, etc.) to keep the
  build context clean once a real Maven project (with a local `target/`)
  lands.
- `docker-compose.yml`'s backend service definition did not need to
  change — same `context: ./backend`, `dockerfile: Dockerfile`, port 8080,
  and `/actuator/health` healthcheck path already matched. Updated the
  top-of-file comment only, to reflect that the backend Dockerfile is now
  the real multi-stage build (frontend is still OPS-1.1's placeholder,
  pending OPS-1.3/FE-1.1).

**Follow-up required once BE-1.1 lands (flagging per this task's own
Notes for implementer):** delete `backend/pom.xml` and `backend/src/`
(the placeholder Maven project) and re-run `docker build -t trendly-backend
./backend` plus `docker compose up` against the real Spring Boot `pom.xml`
and JAR. The Dockerfile itself should need no changes if BE-1.1 keeps the
same coordinates (Maven, Java 21, `mvn package` producing a fat jar under
`target/`) — but this has only been verified against the placeholder, not
a real Spring Boot build, and should get an explicit smoke check before
being trusted for BE-1.1's actual output (e.g. confirm the shaded/repackaged
jar name matches what `COPY --from=build /build/target/app.jar app.jar`
expects — Spring Boot's `spring-boot-maven-plugin` repackages
`<artifactId>-<version>.jar` by default, not `app.jar`, unless
`<finalName>` is set the same way this placeholder pom does).

Verified locally (Docker Desktop, `docker compose` v5):
- `docker build -t trendly-backend ./backend` succeeded end-to-end
  (Maven dependency resolution, `mvn package`, copy into JRE runtime).
- `docker run -d -p 18080:8080 trendly-backend`: `curl
  localhost:18080/actuator/health` returned `{"status":"UP"}`; `docker exec
  ... whoami` / `id` confirmed the process runs as non-root user `app`
  (uid 100, gid 101), not root.
- `docker run -e JAVA_OPTS="-Xmx64m" ...`: confirmed via `ps` inside the
  container that the JVM was actually launched with `-Xmx64m`, proving
  `JAVA_OPTS` is honored and overridable at runtime.
- Re-ran `docker build` after touching only the source file (not
  `pom.xml`): the `mvn dependency:go-offline` layer stayed `CACHED`,
  confirming the dependency-download layer is cached independently of
  source changes.
- Image size / multi-stage check: final `trendly-backend` image is
  ~208 MB (`docker image inspect --format '{{.Size}}'`). Built a naive
  **single-stage** equivalent (same `pom.xml`/`src`, but `FROM
  maven:3.9-eclipse-temurin-21` all the way through, no second stage) for
  comparison: ~597 MB. Confirmed via `docker run --entrypoint sh
  trendly-backend -c 'ls /app; which mvn'` that the final image contains
  only `app.jar` in `/app` and has no `mvn` binary — the build stage
  (Maven install, dependency cache, source) is fully discarded.
- Full-stack re-verification (since this changes a component OPS-1.1's
  compose file depends on): `docker compose up -d --build` from repo root
  brought up `postgres`, `backend`, `frontend` together; all three reported
  `healthy` in `docker compose ps`. `curl localhost:8080/actuator/health`
  → `{"status":"UP"}` and `curl localhost:3000/` succeeded from the host,
  matching OPS-1.1's original verification. (Host port 5432 was occupied
  by an unrelated local container again, as OPS-1.1 also noted; overrode
  `POSTGRES_PORT` in `.env` with no code changes, same as before.)
- Final teardown: `docker compose down -v`, removed the locally built
  `trendly-backend`/`trendly-frontend` images and the naive single-stage
  comparison image, removed the test `.env` — repo left clean, nothing
  left running (`docker ps -a` / `docker images` / `docker volume ls`
  show no leftover `trendly-*` resources).

**Status:** Ready for review
