# Multi-stage build for the Next.js frontend.
#
# NOTE (OPS-1.3, 2026-07-25): FE-1.1 (the real Next.js skeleton, see
# tasks/frontend/FE-1.1.md) has not landed yet. This Dockerfile is written
# and verified against a **minimal hand-rolled placeholder Next.js app**
# living directly under frontend/ (app/, src/lib/config.ts, package.json,
# etc.) that mirrors what FE-1.1 commits to: App Router, TypeScript,
# Tailwind, npm as the locked installer, `output: 'standalone'` in
# next.config.js, and NEXT_PUBLIC_API_BASE_URL consumed as a build-time
# var via a single typed accessor. This supersedes OPS-1.1's throwaway,
# dependency-free `frontend/placeholder/` (plain Node http server).
#
# Follow-up required once FE-1.1 lands: delete this placeholder app
# (package.json, package-lock.json, app/, src/, public/, next.config.js,
# tsconfig.json, tailwind/postcss configs) and re-run `docker build -t
# trendly-frontend ./frontend` plus `docker compose up` against the real
# Next.js app. Do NOT assume this Dockerfile needs no changes: a prior
# revision of this file omitted `COPY ... /app/public ./public` in the
# runtime stage, which happened to build fine only because the
# placeholder app had no public/ dir -- a real `create-next-app` output
# (favicon.ico, static assets) would have 404'd silently while the
# healthcheck stayed green. This is now fixed (see the runtime stage
# below) and the placeholder app now ships a `public/` dir specifically so
# this step is exercised, but re-verify explicitly against FE-1.1's real
# output before trusting it again -- do not take "same shape" as a given.
#
# NEXT_PUBLIC_API_BASE_URL is a Next.js *build-time* var (inlined into the
# JS bundle by `next build`, not read at container runtime) — see
# docs/local-dev.md and tasks/devops/OPS-1.3.md. It must be supplied as a
# Docker build ARG (docker-compose.yml's frontend.build.args), not as a
# `docker run -e` / compose `environment:` var. The build stage fails
# loudly (via a `test -n` guard) if it's missing or empty, rather than
# silently shipping an image with an empty value baked in.

# ---- deps stage -------------------------------------------------------
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# ---- build stage -------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /app
ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
# Fail the build loudly if the build-arg wasn't supplied, instead of
# silently shipping an image with NEXT_PUBLIC_API_BASE_URL baked in as an
# empty string (the ENV line above always *defines* the var, just as ""
# when the ARG is unset -- so a `?? '(unset)'` fallback in application
# code would never trigger and the breakage would only surface once
# something tries to actually call the (empty) API base URL at runtime).
RUN test -n "$NEXT_PUBLIC_API_BASE_URL" || \
    (echo "NEXT_PUBLIC_API_BASE_URL build-arg is required (pass via docker build --build-arg or docker-compose.yml's frontend.build.args)" >&2; exit 1)
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ---- runtime stage -------------------------------------------------------
FROM node:20-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# `output: 'standalone'` produces a self-contained server bundle
# (.next/standalone/server.js + a pruned node_modules) plus static assets
# that must be copied in separately (not included in standalone/ itself):
# .next/static (build-hashed JS/CSS chunks) AND public/ (favicon, static
# images, robots.txt, etc -- served at the site root, not under /_next).
# Omitting public/ builds fine and passes a naive healthcheck; it only
# manifests as silent 404s on static assets once a real app has one.
COPY --from=build --chown=app:app /app/.next/standalone ./
COPY --from=build --chown=app:app /app/.next/static ./.next/static
COPY --from=build --chown=app:app /app/public ./public

USER app
EXPOSE 3000
ENTRYPOINT ["node", "server.js"]
