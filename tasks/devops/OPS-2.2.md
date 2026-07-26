# OPS-2.2 — Frontend CI workflow

**Epic:** E-OPS-2 — CI pipeline
**Owning agent:** devops-agent
**Depends on:** OPS-1.1, FE-1.1
**Size:** S

## Description
GitHub Actions workflow that lints and builds the frontend on every push
and PR.

## Scope
- `.github/workflows/frontend-ci.yml`, triggered on `push` and
  `pull_request`; path-filter to `frontend/**` + the workflow file itself.
- Steps: checkout → `actions/setup-node` (npm cache) → `npm ci` →
  `npm run lint` → `npm run build`.
- `NEXT_PUBLIC_API_BASE_URL` is required at build time — supply a dummy
  value in the workflow env.

## Acceptance criteria
- [x] `.github/workflows/frontend-ci.yml` exists and runs green on a PR.
- [x] A lint error or a build error turns the run red (verified, not
      assumed).

## Notes
Workflow triggers on push/PR to `main`, path-filtered to `frontend/**` and
the workflow file itself. Steps: checkout -> `actions/setup-node@v4` (Node
20 LTS, npm cache keyed on `frontend/package-lock.json`) -> `npm ci` ->
`npm run lint` -> `npm run build` with `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`
supplied as a placeholder build-time env var. Locally ran `npm ci && npm run
lint && npm run build` in `frontend/` — all green. Verified failure modes by
temporarily injecting a lint violation (exit code 1) and a TS type error
(exit code 1) into `app/page.tsx`, then restored the file (confirmed via
`git status`/`git diff` — clean). YAML validated with Ruby's YAML parser
(no linter/actionlint available in this environment). No conflict with
OPS-2.1: `.github/workflows/` only contains `frontend-ci.yml`.

**Status:** Done
