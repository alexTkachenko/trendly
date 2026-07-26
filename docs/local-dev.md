# Local development

The full stack (Postgres, backend, frontend) runs via a single root
`docker-compose.yml`. There are no manual setup steps beyond having Docker
installed and copying the env template.

> **Placeholder notice:** `backend/Dockerfile` (OPS-1.2) and
> `frontend/Dockerfile` (OPS-1.3) are both real multi-stage builds
> (Maven → JRE, and npm → `next build` → Next.js standalone runtime), but
> each still builds a **minimal placeholder app** rather than the real one,
> since `BE-1.1` and `FE-1.1` haven't landed yet:
> - Backend: `backend/pom.xml` + `backend/src/` (trivial Maven project).
> - Frontend: `frontend/package.json` + `frontend/app/` (minimal App
>   Router + TypeScript + Tailwind Next.js app, `output: 'standalone'`).
>
> Both will be swapped out for the real apps once BE-1.1/FE-1.1 land.
> **Do not assume this needs no changes** — round 1 of OPS-1.3's review
> caught a real Dockerfile gap (a missing `public/` copy) that only
> surfaces once a real app with static assets replaces the placeholder.
> Re-run the full verification in the "follow-up" sections of
> `tasks/devops/OPS-1.2.md` / `tasks/devops/OPS-1.3.md` before trusting
> either Dockerfile against the real app.
>
> Building the frontend image directly (`docker build ./frontend`, e.g. in
> CI) now requires `--build-arg NEXT_PUBLIC_API_BASE_URL=<value>` — the
> build fails loudly without it (see "Frontend build-time config" below).

## Prerequisites

- Docker and the Docker Compose plugin (`docker compose version`). Nothing
  else needs to be installed locally — no local Postgres, Node, or Java
  required.

## Start the stack

```bash
cp .env.example .env   # first time only; fill in/override values as needed
docker compose up
```

This builds the backend and frontend images (first run only, then cached)
and starts:

| Service    | Host port (default)         | Notes                                  |
|------------|------------------------------|-----------------------------------------|
| `postgres` | `${POSTGRES_PORT}` → 5432    | Data persisted in the `trendly_pgdata` named volume |
| `backend`  | `${BACKEND_PORT}` → 8080     | Waits for postgres to report healthy before starting |
| `frontend` | `${FRONTEND_PORT}` → 3000    | Starts after `backend` |

Run in the background with `docker compose up -d`.

### Port conflicts

If you already have something listening on 5432/8080/3000 locally, override
the relevant `*_PORT` variable in your `.env` (e.g. `POSTGRES_PORT=15432`)
— nothing else needs to change, the compose file reads these at startup.

## Stop the stack

```bash
docker compose down       # stops and removes containers; postgres data survives (named volume)
docker compose down -v    # also deletes the trendly_pgdata volume (full reset)
```

## Logs

```bash
docker compose logs -f              # all services, follow
docker compose logs -f backend      # single service
docker compose logs -f postgres frontend
```

## Health checks

All three services define container healthchecks
(`docker compose ps` shows current status):

- `postgres`: `pg_isready`
- `backend`: `GET /actuator/health` (placeholder Maven app returns
  `{"status":"UP"}`; the real Spring Boot app (BE-1.1) will expose the
  same path via `spring-boot-actuator`)
- `frontend`: `GET http://127.0.0.1:3000/` (from *inside* the container;
  `127.0.0.1` rather than `localhost` — the Next.js standalone server binds
  only to IPv4, and `localhost` resolves to `::1` first inside the alpine
  image, which makes `wget` fail even though the app is healthy)

`backend` will not attempt to start until `postgres` is reported healthy
(`depends_on: condition: service_healthy`), and `frontend` starts after
`backend`.

## Connecting locally

To inspect the database directly with `psql`, TablePlus, DBeaver, or
similar, connect using the `.env.example` values (Postgres 16, database
`trendly` — see `docs/db/conventions.md`):

```bash
psql "postgresql://trendly:trendly_dev_password@localhost:5432/trendly"
```

If you overrode `POSTGRES_PORT` (e.g. due to a local port conflict on
5432), substitute that value instead. This was verified end-to-end: with
the `postgres` service healthy, the connection string above (built
directly from `.env.example`) connects successfully and reports
`PostgreSQL 16.x` with `current_database = trendly`.

## Environment variables

See `.env.example` at the repo root for the full list with descriptions.
Copy it to `.env` (gitignored) and adjust values locally — never commit
real credentials.

## Running services outside Docker

The root `.env.example` / `docker-compose.yml` pair covers the full
containerized stack. If you instead want to run the backend or frontend
directly on the host (e.g. from an IDE, `./mvnw spring-boot:run`, or
`npm run dev`) while still pointing at Docker-provisioned Postgres (or
running just `docker compose up postgres` for the DB alone), use the
app-specific `.env.example` files instead — `docker-compose.yml` never
reads these; they exist purely for the outside-Docker case:

- `backend/.env.example` — `SPRING_DATASOURCE_URL` /
  `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` (pointed at
  `localhost:5432`, not the `postgres` service name, since a host-run
  backend isn't on `trendly_net`), `JWT_SECRET` (same dev-only placeholder
  as the root file), and notes on the JVM-tuning vars (`JDK_JAVA_OPTIONS`
  works for any host `java` launch; `JAVA_OPTS` is Docker-entrypoint-only,
  see "Backend JVM options" below).
- `frontend/.env.example` — `NEXT_PUBLIC_API_BASE_URL` for a standalone
  `npm run dev` / `npm run build`, with guidance on the host-vs-in-network
  value distinction (see "Frontend build-time config" below).

Copy the relevant file to `<dir>/.env` (both are gitignored, same as the
root `.env`) — but the two apps differ in how that file gets picked up:

- **Frontend:** Next.js (v9.4+; this repo pins `next@14.2.35`) loads
  `.env`/`.env.local` automatically. `cp frontend/.env.example
  frontend/.env` is sufficient on its own — no export or IDE run
  configuration needed — for both `npm run dev` and `npm run build`.
- **Backend:** Spring Boot does **not** load `.env` files automatically
  (no built-in dotenv support). After `cp backend/.env.example
  backend/.env`, you must load it explicitly — export the variables into
  your shell before running `./mvnw spring-boot:run`, configure them in
  your IDE's run configuration, or use a dotenv plugin.

## Rebuilding after Dockerfile/source changes

```bash
docker compose up -d --build
```

## Backend JVM options

`backend/Dockerfile` (OPS-1.2) accepts `JAVA_OPTS` (shell-expanded into the
`java` command) and `JDK_JAVA_OPTIONS` (picked up natively by the JVM
launcher) to tune JVM flags at container start, e.g.:

```bash
docker run -e JAVA_OPTS="-Xmx512m" trendly-backend
```

These are optional and not currently wired through `docker-compose.yml`'s
`environment:` block (no required default is needed); add
`JAVA_OPTS: ${JAVA_OPTS:-}` to the `backend` service there if you want to
tune it via `.env` for local compose runs.

## Frontend build-time config (`NEXT_PUBLIC_API_BASE_URL`)

`NEXT_PUBLIC_API_BASE_URL` is a Next.js **build-time** variable — Next.js
inlines all `NEXT_PUBLIC_*` values into the compiled JS bundle at
`next build` time, not at container runtime. `docker-compose.yml` passes it
to the `frontend` service as a Docker build `arg`
(`frontend.build.args.NEXT_PUBLIC_API_BASE_URL`, read from `.env`), and
`frontend/Dockerfile` declares `ARG NEXT_PUBLIC_API_BASE_URL` +
`ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL` before the
`next build` step so it gets inlined.

**Important consequence:** because it's baked in at build time, changing
`NEXT_PUBLIC_API_BASE_URL` in `.env` and running `docker compose up` again
will **not** change the running app — Compose only rebuilds an image when
told to. Run `docker compose up -d --build` (or `docker compose build
frontend`) after changing this value.

The default in `.env.example` (`http://localhost:8080`) is correct for the
common case: `NEXT_PUBLIC_*` values are inlined into the **browser**
bundle too, and the browser runs on the host (not inside the compose
network), talking to the backend via its host-published port
(`localhost:8080`). Only switch this to `http://backend:8080` if/when
FE-1.2's API client needs the Next.js **server** (SSR / route handlers
running inside the frontend container) to call the backend directly —
that value is only reachable from inside `trendly_net`, not from a host
browser. This distinction is FE-1.2's concern, not this task's; noted here
only so the build-arg mechanism (which value gets baked in, and when
rebuilding is required) is understood correctly. See `frontend/.env.example`
for the same guidance colocated with the frontend app.
