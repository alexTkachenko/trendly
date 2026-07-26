# Trendly frontend

Next.js (App Router) + TypeScript + Tailwind CSS skeleton for the Trendly
frontend. See `tasks/frontend/FE-1.1.md` for the task that created this
skeleton and `docs/local-dev.md` for how it fits into the local Docker
Compose stack.

## Local development

```bash
npm install
cp .env.example .env.local   # or export NEXT_PUBLIC_API_BASE_URL another way
npm run dev
```

Open http://localhost:3000.

`NEXT_PUBLIC_API_BASE_URL` is inlined into the build at `next build` time
(not read at container runtime) — see `src/lib/config.ts`.

## Scripts

- `npm run dev` — start the dev server.
- `npm run build` — production build (`output: 'standalone'`, required by
  `Dockerfile`).
- `npm start` — run the production build with `next start` (for
  non-Docker use; the Docker image runs `node server.js` from the
  standalone output instead).
- `npm run lint` — ESLint.

## Docker

Built via the repo-root `docker-compose.yml` (`frontend` service) or
standalone:

```bash
docker build --build-arg NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 -t trendly-frontend .
docker run -p 3000:3000 trendly-frontend
```
