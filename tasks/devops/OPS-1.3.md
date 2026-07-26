# OPS-1.3 — Frontend Dockerfile

**Epic:** E-OPS-1 — Local dev environment
**Owning agent:** devops-agent
**Depends on:** OPS-1.1 (compose file references this)
**Size:** S

## Description
Multi-stage Dockerfile for the Next.js frontend under `frontend/`.

## Scope
- **Deps stage:** installs dependencies with a locked installer
  (`npm ci` / `pnpm install --frozen-lockfile`, whichever frontend-agent
  standardizes on in FE-1.1).
- **Build stage:** runs `next build`, ideally with `output: 'standalone'`
  in `next.config.js` to minimize the final image.
- **Runtime stage:** slim Node image, copies only the standalone build
  output, runs `node server.js` (or `next start` if standalone isn't
  used).
- Runs as a non-root user.
- Exposes 3000.

## Acceptance criteria
- [x] `docker build -t trendly-frontend ./frontend` succeeds.
- [x] Resulting container serves the app on port 3000 and can reach the
      backend service by name inside the compose network.
- [x] Final image excludes `node_modules` dev dependencies and source
      maps not needed at runtime (verify image size is reasonable).
- [x] Container runs as non-root.

## Notes for implementer
- Same caveat as OPS-1.2: if FE-1.1 isn't done yet, build against a
  minimal placeholder Next.js app first and re-verify once FE-1.1 lands.
- `NEXT_PUBLIC_API_BASE_URL` must be injectable at build or runtime —
  confirm which Next.js requires (build-time for `NEXT_PUBLIC_*` vars)
  and document that clearly in `.env.example`.
- OPS-1.1's reviewer flagged that the current `docker-compose.yml` passes
  `NEXT_PUBLIC_API_BASE_URL` as a runtime `environment` var, which will
  silently no-op once the real Next.js app replaces the placeholder
  (`NEXT_PUBLIC_*` is inlined at build time). This task must change the
  frontend service to pass it as a Docker build `arg` instead.

## Implementation notes

- FE-1.1 (the real Next.js skeleton) has not landed yet, so
  `frontend/Dockerfile` was written and verified against a **minimal
  hand-rolled placeholder Next.js app** living directly under `frontend/`
  (`package.json`, `next.config.js` with `output: 'standalone'`,
  App Router `app/layout.tsx` + `app/page.tsx`, `src/lib/config.ts` as a
  single typed accessor for `NEXT_PUBLIC_API_BASE_URL`, TypeScript,
  Tailwind/PostCSS config, `package-lock.json` as the locked installer
  file for `npm ci`). This supersedes OPS-1.1's throwaway
  `frontend/placeholder/` (plain Node `http` server, no npm install,
  no real build) — `frontend/placeholder/` and `frontend/.gitkeep` have
  both been removed.
- `frontend/Dockerfile` is a real three-stage build:
  - **deps**: `node:20-alpine`, `COPY package.json package-lock.json`,
    `npm ci`.
  - **build**: takes `ARG NEXT_PUBLIC_API_BASE_URL`, sets it as `ENV`
    before `npm run build` (`next build`) so Next.js inlines it into the
    bundle; reuses the `deps` stage's `node_modules` via
    `COPY --from=deps`.
  - **runtime**: slim `node:20-alpine`, non-root `app` user
    (`addgroup -S app && adduser -S app -G app`), copies
    `.next/standalone` (self-contained server + pruned `node_modules`),
    `.next/static`, and `public/` from the build stage, `USER app`,
    `EXPOSE 3000`, `ENTRYPOINT ["node", "server.js"]`. No `next start`,
    no full `node_modules`, no source tree in the final image.
- **Bug fix required by this task (OPS-1.1 reviewer finding):**
  `docker-compose.yml`'s `frontend` service previously passed
  `NEXT_PUBLIC_API_BASE_URL` as a runtime `environment:` var, which is
  wrong for real Next.js — `NEXT_PUBLIC_*` vars are inlined into the JS
  bundle at `next build` time, not read at container start. Fixed by
  moving it to `frontend.build.args.NEXT_PUBLIC_API_BASE_URL` in
  `docker-compose.yml`, sourced from the same `.env` (`${NEXT_PUBLIC_API_BASE_URL}`),
  and adding `ARG NEXT_PUBLIC_API_BASE_URL` + `ENV
  NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL` in
  `frontend/Dockerfile`'s build stage ahead of `RUN npm run build`. The
  runtime `environment:` block for the frontend service now only sets
  `PORT: 3000`.
- **Proving it's build-time, not runtime** (the core thing to verify for
  this fix):
  1. Built the image with `--build-arg
     NEXT_PUBLIC_API_BASE_URL=http://backend:8080`. Ran the container and
     `curl`'d `/`: the placeholder page rendered
     `NEXT_PUBLIC_API_BASE_URL=http://backend:8080`, baked into the
     server-rendered HTML.
  2. Re-ran the *same already-built image* with `docker run -e
     NEXT_PUBLIC_API_BASE_URL=http://SHOULD-NOT-APPEAR:9999` (i.e.
     overriding the runtime env var after the build). The rendered page
     was unchanged — still `http://backend:8080` — proving the value is
     compiled into the bundle at `next build` time and a runtime env var
     override is a no-op, exactly the bug the reviewer flagged and that
     this fix addresses.
  3. Built a second image with a different `--build-arg
     NEXT_PUBLIC_API_BASE_URL=http://localhost:8080` and confirmed the
     rendered page reflected *that* different value instead — showing the
     mechanism does work correctly when the value is supplied at build
     time (as `docker-compose.yml`'s `build.args` now does), and only
     fails silently when supplied as a runtime var.
- Other acceptance-criteria checks:
  - `docker build -t trendly-frontend ./frontend` succeeded (multi-stage,
    `npm ci` → `next build` → standalone runtime copy).
  - `docker exec ... whoami` → `app` (non-root) inside the running
    container.
  - `docker exec ... ls node_modules` confirmed no dev deps
    (`typescript`, `tailwindcss`, `postcss`, `autoprefixer`) present in
    the pruned standalone `node_modules`; `find . -maxdepth 2` inside the
    container showed only `.next/`, `server.js`, `package.json` — no
    `src/`, `app/`, or other source files copied into the final image.
    Final image size: ~154 MB.
  - Full-stack re-verification: `docker compose up -d --build` from repo
    root brought up `postgres`, `backend`, `frontend`, all reporting
    `healthy` in `docker compose ps`; `curl localhost:8080/actuator/health`
    → `{"status":"UP"}`; `curl localhost:3000/` served the page with the
    `.env`-configured build-arg value baked in; `docker exec
    trendly-frontend-1 wget -qO- http://backend:8080/actuator/health` →
    `{"status":"UP"}`, confirming the frontend container can reach the
    backend by service name inside `trendly_net`. No regression to
    OPS-1.1/OPS-1.2. (Host port 5432 was occupied by an unrelated local
    container, as OPS-1.1/OPS-1.2 also noted; overrode `POSTGRES_PORT` for
    this verification run only, no code changes.)
- Final teardown: `docker compose down -v --remove-orphans`, removed all
  locally built `trendly-frontend`/`trendly-backend` test images
  (including the second build-arg comparison image), removed the test
  `.env` — repo left clean, `docker ps -a` / `docker images` show no
  leftover `trendly-*` resources.
- **Follow-up required once FE-1.1 lands:** delete the placeholder
  frontend app (`package.json`, `package-lock.json`, `app/`, `src/`,
  `public/`, `next.config.js`, `tsconfig.json`, `tailwind.config.js`,
  `postcss.config.js`) and re-run `docker build -t trendly-frontend
  ./frontend` plus `docker compose up` against the real Next.js app.
  **Do not assume no changes are needed** — this Dockerfile has already
  had one gap slip through under the placeholder that a real
  `create-next-app` output would have exposed (see round 1 fixes below);
  re-verify explicitly (image builds, static assets under `public/`
  actually resolve over HTTP, no other `.next`-adjacent output directory
  was missed) rather than treating "same shape" as a given.

### Round 1 review fixes (reviewer-agent: CHANGES REQUESTED)

- **B1 (blocking) — missing `public/` copy in the runtime stage.** The
  first version of `frontend/Dockerfile` only copied
  `.next/standalone` and `.next/static` into the runtime image, omitting
  `public/`. This built and ran fine only because the placeholder app had
  no `public/` directory to begin with — a real `create-next-app` output
  (favicon, static images) would 404 on all `public/`-served assets while
  the container healthcheck stayed green, exactly the failure mode this
  task exists to catch. Also flagged: the Dockerfile header and these
  notes previously asserted the file "should need no changes" once FE-1.1
  lands, which was false as long as this gap existed.
  - Fix: added `frontend/public/robots.txt` (a trivial real static file,
    not just a `.gitkeep`, so the copy step is exercised and the served
    output is checkable) and added
    `COPY --from=build --chown=app:app /app/public ./public` to the
    runtime stage in `frontend/Dockerfile`, after the `.next/static` copy.
    Corrected the header comment and the follow-up note above to no
    longer claim "no changes needed" for FE-1.1 — replaced with an
    explicit instruction to re-verify.
  - Re-verified: rebuilt (`docker build -t trendly-frontend --build-arg
    NEXT_PUBLIC_API_BASE_URL=http://backend:8080 ./frontend`); ran the
    container and confirmed `curl http://localhost:3100/robots.txt`
    returns the file's contents (served from `public/`, not `.next/`);
    `docker exec ... ls public` showed `robots.txt` owned by `app:app`;
    confirmed the page's baked-in `NEXT_PUBLIC_API_BASE_URL` value was
    unaffected by the added copy step.
- **S1 (should-fix) — missing build-arg failed silently.** Building
  without `--build-arg NEXT_PUBLIC_API_BASE_URL=...` previously produced
  a working image with the value baked in as an empty string (the `ENV
  NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL` line always defines
  the var, just as `""` when the `ARG` is unset) — no build failure, no
  visible fallback, and `src/lib/config.ts`'s `?? '(unset)'` fallback
  never triggers since the var is defined-but-empty rather than
  `undefined`.
  - Fix: added `RUN test -n "$NEXT_PUBLIC_API_BASE_URL" || (echo
    "NEXT_PUBLIC_API_BASE_URL build-arg is required ..." >&2; exit 1)`
    in `frontend/Dockerfile`'s build stage, immediately after the
    `ARG`/`ENV` lines and before `COPY . .` / `RUN npm run build`.
  - Re-verified: `docker build -t trendly-frontend-noarg ./frontend`
    (no `--build-arg` at all) now fails at that `RUN test` step with the
    explicit error message printed to stderr, and no image is produced.
    Same result re-confirmed with an explicit empty value
    (`--build-arg NEXT_PUBLIC_API_BASE_URL=`). The normal case (non-empty
    value supplied, as `docker-compose.yml`'s `build.args` does) is
    unaffected — confirmed by the successful rebuild above.
- **Nits addressed (cheap, optional):** removed the stale `placeholder`
  line from `frontend/.dockerignore` (dead reference to OPS-1.1's
  already-removed throwaway dir); resolved the `frontend/.gitkeep`
  add/delete staging artifact (`git status` showed `AD` — staged as
  added, deleted in the working tree — from a prior session; re-staged
  so it now shows as a clean deletion). **Nits left as-is (optional,
  reviewer flagged as non-blocking):** `ENTRYPOINT` vs `CMD` (kept
  `ENTRYPOINT` for consistency with `backend/Dockerfile`, which uses the
  same pattern); the placeholder's non-functional `next lint` script
  (inherited from the standard `create-next-app` template, not something
  this task's scope covers); floating base image tags
  (`node:20-alpine`, `postgres:16-alpine` etc. — a repo-wide pinning
  policy question, not specific to this Dockerfile).
- Full regression re-run after both fixes: `docker compose up -d --build`
  brought up all three services `healthy`; `curl
  localhost:8080/actuator/health` → `{"status":"UP"}`; `curl
  localhost:3000/` served the page with the build-time value baked in;
  `curl localhost:3000/robots.txt` served the placeholder static asset
  through the full compose stack (not just a standalone `docker run`);
  `docker exec trendly-frontend-1 wget -qO- http://backend:8080/actuator/health`
  → `{"status":"UP"}`. No regression to OPS-1.1/OPS-1.2. Full teardown
  afterward (`docker compose down -v --remove-orphans`, removed all test
  images including the two failed-build attempts, which never produced
  an image to begin with, and the test `.env`) — repo left clean.

**Status:** Ready for review
