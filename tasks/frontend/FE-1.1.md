# FE-1.1 — Initialize Next.js skeleton

**Epic:** E-FE-1 — Frontend project setup
**Owning agent:** frontend-agent
**Depends on:** OPS-1.1, OPS-1.3
**Size:** M

## Description
Replace the placeholder Next.js app under `frontend/` with a real,
buildable skeleton: App Router, TypeScript, Tailwind, builds and runs.
No API client, no app shell — those are separate tasks (FE-1.2, FE-1.3).

## Scope
- Next.js, App Router, TypeScript, Tailwind CSS. `npm` + `package-lock.json`
  (regenerate the lockfile — don't build on the placeholder's).
- `output: 'standalone'` in `next.config.js` (OPS-1.3's Dockerfile depends on it).
- `NEXT_PUBLIC_API_BASE_URL` consumed as a **build-time** var (Next.js
  inlines `NEXT_PUBLIC_*` at build time) through one typed accessor
  module, so FE-1.2 has a single place to import it from later.
- Minimal landing route (`app/page.tsx`) rendering the configured base
  URL, to prove the wiring works. Port 3000.
- Remove the placeholder app files under `frontend/` (everything except
  `frontend/Dockerfile`, which is OPS-1.3's and stays).

## Acceptance criteria
- [x] `npm run build` succeeds and produces standalone output; app
      serves on port 3000.
- [x] `NEXT_PUBLIC_API_BASE_URL` is build-time only, via the typed
      accessor; the landing route reflects it.
- [x] After landing, re-run `docker build ./frontend` + `docker compose up`
      to smoke-check OPS-1.3's Dockerfile/compose wiring against the
      real app (it was only verified against the placeholder so far).

## Notes for implementer
- Read `tasks/devops/OPS-1.3.md`'s implementation notes for the exact
  shape the Dockerfile expects (npm, `output: 'standalone'`, `.next/standalone` + `.next/static`).
- `docker-compose.yml` passes `NEXT_PUBLIC_API_BASE_URL` as a Docker
  build arg (not a runtime env var) — design for build-time only.

## Implementation notes

- Rebuilt `frontend/` from scratch via `create-next-app@latest` (App
  Router, TypeScript, Tailwind, ESLint, `npm`), scaffolded outside the
  repo and merged in since `create-next-app` refuses a non-empty target
  dir (it conflicted with the existing `Dockerfile`, which was preserved
  unchanged per the task's instruction not to touch it absent a real
  bug). Removed every other placeholder file first
  (`package.json`, `package-lock.json`, `app/`, `src/`, `public/`,
  `next.config.js`, `tsconfig.json`, `tailwind.config.js`,
  `postcss.config.js`) as scoped.
- Picked current-latest majors since this is greenfield with no version
  pin elsewhere in the repo: Next.js 16.2.12, React 19.2.4, Tailwind CSS
  4 (`@tailwindcss/postcss`, no separate `tailwind.config.js` — v4's
  CSS-first config via `@import "tailwindcss"` in `app/globals.css`),
  TypeScript 5, ESLint 9 flat config. `npm` + a freshly regenerated
  `package-lock.json` (deleted `node_modules`/lockfile and ran a clean
  `npm install`, not built on the placeholder's lockfile).
- `next.config.ts`: `output: 'standalone'` + `reactStrictMode: true`,
  same requirement as the placeholder — `frontend/Dockerfile`'s runtime
  stage depends on `.next/standalone` + `.next/static` + `public/`.
- `src/lib/config.ts`: single typed accessor,
  `process.env.NEXT_PUBLIC_API_BASE_URL ?? '(unset)'`, documented as
  build-time-only (no runtime `process.env` re-read), for FE-1.2 to
  import from later.
- `app/page.tsx`: minimal landing route rendering `apiBaseUrl` in a
  `data-testid="api-base-url"` element (Home page replaces
  `create-next-app`'s demo content entirely — no Vercel/Next.js
  boilerplate links). Dropped `next/font/google` (Geist) from
  `app/layout.tsx` to avoid an extra external-network dependency during
  the Docker build stage; using the system font stack instead — an
  intentional skeleton-scope simplification, not a gap.
  `public/robots.txt` kept as a trivial real static file (per OPS-1.3's
  note that an empty `public/` would silently mask a missing-copy bug).
- No API client, no app shell, no automated component/flow tests added
  — out of scope per the task description (FE-1.2/FE-1.3) and there are
  no interactive components yet to test; `npm run build` + the Docker/
  compose smoke checks below are the verification for this task.
- Root `.gitignore`: added `frontend/*.tsbuildinfo` (TS incremental
  build cache artifact); `node_modules/`, `.next/`, `out/`, and
  `.env*` were already covered.

### Verification

- `npx tsc --noEmit` — clean, no errors.
- `npm run lint` (ESLint, flat config) — clean, no errors/warnings.
- `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 npm run build` —
  succeeds, `.next/standalone/server.js` + `.next/static` produced; ran
  reproducibility re-check after cleanup, same result. Route `/`
  prerendered as static content (build-time value baked into the HTML).
- Ran `.next/standalone/server.js` directly on `PORT=3100` (with
  `public/` and `.next/static` copied alongside, matching what the
  Dockerfile does) and `curl`'d `/`: rendered
  `data-testid="api-base-url">http://localhost:8080`; `/robots.txt`
  served from `public/`.
- `docker build --build-arg NEXT_PUBLIC_API_BASE_URL=http://backend:8080
  -t trendly-frontend-fe11 ./frontend` — succeeded against the real app
  (first real-app run of OPS-1.3's Dockerfile, superseding its
  placeholder-only verification).
- `docker run` the built image on port 3101: `docker exec ... whoami` →
  `app` (non-root); `/` rendered `http://backend:8080` baked in;
  `/robots.txt` served correctly; image size 187MB (reasonable, no dev
  deps/source tree — same shape OPS-1.3 verified).
- Re-ran the same already-built image with `docker run -e
  NEXT_PUBLIC_API_BASE_URL=http://SHOULD-NOT-APPEAR:9999` (runtime
  override): page still rendered `http://backend:8080`, confirming the
  value is compiled in at `next build` time and a runtime env var is a
  no-op, per FE-1.1/OPS-1.3's build-time-only design.
- Full-stack smoke check: `cp .env.example .env` (overrode
  `POSTGRES_PORT` to 5434 for this run only — host 5432 was occupied by
  an unrelated local container, same caveat OPS-1.1/OPS-1.3 hit) then
  `docker compose up -d --build`. All three services (`postgres`,
  `backend`, `frontend`) reported `healthy`. `curl localhost:3000/` →
  landing page with `http://localhost:8080` baked in (this run's
  `.env` value); `curl localhost:3000/robots.txt` → served;
  `curl localhost:8080/actuator/health` → `{"status":"UP"}`;
  `docker exec trendly-frontend-1 wget -qO- http://backend:8080/actuator/health`
  → `{"status":"UP"}`, confirming frontend reaches backend by service
  name inside `trendly_net`; `docker exec trendly-frontend-1 whoami` →
  `app`. No regressions to OPS-1.3's Dockerfile or `docker-compose.yml`
  wiring now that the real app replaced the placeholder.
- Teardown: `docker compose down -v --remove-orphans`, removed the
  `trendly-frontend`/`trendly-backend` compose-built images and the
  standalone `trendly-frontend-fe11` test image, removed the test
  `.env`, removed local `.next`/`tsconfig.tsbuildinfo` build artifacts —
  `git status` confirms no stray files, `docker ps -a` / `docker images`
  show no leftover `trendly-*` resources.

**Status:** Done
