/**
 * Single typed accessor for build-time public config.
 *
 * `NEXT_PUBLIC_*` variables are inlined into the client/server bundle at
 * `next build` time (NOT read at container runtime) — see
 * tasks/devops/OPS-1.3.md and tasks/frontend/FE-1.1.md for the reasoning.
 * Do not add a runtime fallback that reads this from `process.env` at
 * request time; it will not reflect a value passed via `docker run -e` or
 * a compose `environment:` block after the image is built — only via a
 * rebuild (`docker compose build frontend` / `--build-arg`).
 *
 * FE-1.2 (typed API client) should import `apiBaseUrl` from here rather
 * than reading `process.env.NEXT_PUBLIC_API_BASE_URL` directly, so there
 * is a single place to change if the config strategy ever evolves.
 */
export const apiBaseUrl: string =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "(unset)";
