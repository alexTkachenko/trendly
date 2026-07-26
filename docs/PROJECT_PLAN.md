# Social Network — Project Plan

Stack: **Spring Boot** (backend) · **PostgreSQL** (database) · **Next.js** (frontend)

## 1. Agent roster

| Agent | Config file | Owns |
|---|---|---|
| architect-agent | `agents/architect-agent.md` | Epics/tasks, API contracts, schema design, ADRs |
| backend-agent | `agents/backend-agent.md` | Spring Boot code |
| frontend-agent | `agents/frontend-agent.md` | Next.js code |
| database-agent | `agents/database-agent.md` | Postgres migrations & schema |
| devops-agent | `agents/devops-agent.md` | Docker, CI/CD, environments |
| reviewer-agent | `agents/reviewer-agent.md` | Independent review of every task, **fresh context only** |

If using Claude Code, drop these files into `.claude/agents/` in the repo and invoke by
name, e.g. `@backend-agent implement task BE-2.3`. Always invoke `reviewer-agent` as a
**new, separate session** — never in the same context as the implementer — so it reviews
the diff cold, the way a human reviewer opens a PR without having watched it get written.

## 2. Task lifecycle (applies to every task below)

```
Backlog → In Progress (owning agent) → Ready for Review
        → reviewer-agent review (clean context)
              ├── APPROVED           → Done
              └── CHANGES REQUESTED  → back to In Progress → re-review
```

Rules:
- A task is never marked "Done" by the agent that implemented it.
- reviewer-agent receives only: the task file, the linked contract/schema doc, and the
  code diff — no implementation chat history.
- architect-agent updates this file's status column after each review verdict.

## 3. Epics overview

| ID | Epic | Direction | Depends on |
|---|---|---|---|
| E-DB-1 | Schema foundation & migrations tooling | Database | — |
| E-DB-2 | Core domain schema (users, posts, follows, comments, media) | Database | E-DB-1 |
| E-DB-3 | Indexing & query performance | Database | E-DB-2 |
| E-OPS-1 | Local dev environment (docker-compose) | DevOps | — |
| E-OPS-2 | CI pipeline | DevOps | E-OPS-1 |
| E-OPS-3 | Deployment | DevOps | E-OPS-2 |
| E-BE-1 | Backend project setup & security skeleton | Backend | E-DB-1, E-OPS-1 |
| E-BE-2 | Auth & user management API | Backend | E-BE-1, E-DB-2 |
| E-BE-3 | Post management API (text + images) | Backend | E-BE-2 |
| E-BE-4 | Follow/subscription API | Backend | E-BE-2 |
| E-BE-5 | Feed/timeline API | Backend | E-BE-3, E-BE-4 |
| E-BE-6 | Comments & likes API | Backend | E-BE-3 |
| E-BE-7 | API documentation | Backend | E-BE-2..E-BE-6 |
| E-FE-1 | Frontend project setup | Frontend | E-OPS-1 |
| E-FE-2 | Auth UI | Frontend | E-FE-1, E-BE-2 |
| E-FE-3 | Profile & account UI | Frontend | E-FE-2 |
| E-FE-4 | Post creation & feed UI | Frontend | E-FE-2, E-BE-5 |
| E-FE-5 | Follow/subscribe UI | Frontend | E-FE-3, E-BE-4 |
| E-FE-6 | Comments & likes UI | Frontend | E-FE-4, E-BE-6 |
| E-FE-7 | Responsiveness & accessibility polish | Frontend | E-FE-2..E-FE-6 |

Recommended build order: E-OPS-1 → E-DB-1 → E-DB-2 → E-BE-1 → (E-DB-3 ∥ E-FE-1) →
E-BE-2 → E-FE-2 → E-BE-3/E-BE-4 (parallel) → E-FE-3 → E-BE-5/E-BE-6 (parallel) →
E-FE-4/E-FE-5/E-FE-6 → E-BE-7 → E-OPS-2 → E-OPS-3 → E-FE-7.

---

## 4. Epics → Tasks

### E-DB-1 — Schema foundation & migrations tooling
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| DB-1.1 | Provision local Postgres via docker-compose (coordinate with E-OPS-1) | `docker compose up` starts a reachable Postgres with a named volume | database-agent |
| DB-1.2 | Set up Flyway (or Liquibase) in the backend module | `V1__init.sql` applies cleanly on a fresh DB; migration runs on app startup | database-agent |
| DB-1.3 | Define naming/typing conventions doc | `docs/db/conventions.md` covers PK strategy, timestamps, naming | database-agent |

### E-DB-2 — Core domain schema
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| DB-2.1 | `users` table (id, email, username, password_hash, display_name, bio, avatar_url, timestamps) | Unique constraints on email & username; migration + `docs/db/schema.md` entry | database-agent |
| DB-2.2 | `posts` table (id, author_id FK, text_content, timestamps) | FK to users with `ON DELETE CASCADE`; NOT NULL text or media required at app level | database-agent |
| DB-2.3 | `post_media` table (id, post_id FK, url, media_type, position) | Supports multiple images per post; cascade delete with post | database-agent |
| DB-2.4 | `follows` table (follower_id, followee_id, created_at) | Composite PK/unique on (follower_id, followee_id); CHECK to prevent self-follow | database-agent |
| DB-2.5 | `comments` table (id, post_id FK, author_id FK, content, timestamps) | Cascade delete with post; FK to users restrict/cascade decided in ADR | database-agent |
| DB-2.6 | `post_likes` table (post_id, user_id, created_at) | Composite unique key; used for like counts | database-agent |

### E-DB-3 — Indexing & query performance
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| DB-3.1 | Index `posts(author_id, created_at desc)` for profile & feed queries | `EXPLAIN ANALYZE` shows index scan for target queries | database-agent |
| DB-3.2 | Index `follows(follower_id)` and `follows(followee_id)` | Supports "who I follow" and "my followers" queries efficiently | database-agent |
| DB-3.3 | Index `comments(post_id, created_at)` | Efficient comment-thread pagination | database-agent |
| DB-3.4 | Feed query design doc + sample SQL for architect/backend | Documented query achieves target follow-count fan-out approach | database-agent |

### E-OPS-1 — Local dev environment
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| OPS-1.1 | `docker-compose.yml` with postgres, backend, frontend services + health checks | `docker compose up` brings the full stack up locally | devops-agent |
| OPS-1.2 | Backend multi-stage Dockerfile | Image builds and runs; JAR built inside container | devops-agent |
| OPS-1.3 | Frontend multi-stage Dockerfile | Image builds and serves the Next.js app | devops-agent |
| OPS-1.4 | `.env.example` files for backend & frontend | Documented required vars; no secrets committed | devops-agent |

### E-OPS-2 — CI pipeline
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| OPS-2.1 | CI workflow: backend lint + unit + integration tests | Pipeline fails on any test failure; runs against service-container Postgres | devops-agent |
| OPS-2.2 | CI workflow: frontend lint + build + tests | Pipeline fails on lint/build/test errors | devops-agent |
| OPS-2.3 | CI builds Docker images on merge to main | Images tagged with commit SHA | devops-agent |

### E-OPS-3 — Deployment
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| OPS-3.1 | Deployment target decision (ADR) + config | `docs/deployment.md` describes target env and how to deploy | devops-agent |
| OPS-3.2 | Environment variable / secrets management for staging/prod | No secret values in repo; documented injection method | devops-agent |
| OPS-3.3 | Basic health-check & logging setup | `/actuator/health` (or equivalent) reachable; structured logs | devops-agent |

### E-BE-1 — Backend project setup & security skeleton
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| BE-1.1 | Initialize Spring Boot project (web, data-jpa, security, validation, flyway) | Builds and starts; connects to Postgres from OPS-1.1 | backend-agent |
| BE-1.2 | Global exception handling (`@ControllerAdvice`) + consistent error shape | Documented error response schema in `docs/api/errors.md` | backend-agent |
| BE-1.3 | Base Spring Security config (stateless, JWT filter skeleton) per auth ADR | Unauthenticated requests to protected routes return 401 | backend-agent |

### E-BE-2 — Auth & user management API
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| BE-2.1 | `POST /auth/register` | Creates user, hashes password (BCrypt), validates uniqueness | backend-agent |
| BE-2.2 | `POST /auth/login` issuing JWT | Valid credentials return access (+refresh) token; invalid return 401 | backend-agent |
| BE-2.3 | `GET/PUT /users/me` (profile view/update) | Auth required; only owner can update; validation on fields | backend-agent |
| BE-2.4 | `GET /users/{username}` public profile | Returns public fields only, 404 if not found | backend-agent |

### E-BE-3 — Post management API
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| BE-3.1 | `POST /posts` (text + image upload) | Accepts multipart; stores media refs via post_media; validates size/type | backend-agent |
| BE-3.2 | `GET /posts/{id}` | Returns post with media, author summary, like/comment counts | backend-agent |
| BE-3.3 | `DELETE /posts/{id}` | Only author can delete; 403 otherwise | backend-agent |
| BE-3.4 | `GET /users/{username}/posts` (paginated) | Uses DB-3.1 index; correct pagination metadata | backend-agent |

### E-BE-4 — Follow/subscription API
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| BE-4.1 | `POST /users/{username}/follow` / `DELETE .../follow` | Idempotent-ish behavior, blocks self-follow (409/400) | backend-agent |
| BE-4.2 | `GET /users/{username}/followers` and `/following` (paginated) | Uses DB-3.2 indexes | backend-agent |

### E-BE-5 — Feed/timeline API
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| BE-5.1 | `GET /feed` — posts from followed accounts, paginated, newest first | Matches DB-3.4 query design; verified with >1 followee in tests | backend-agent |
| BE-5.2 | `GET /feed` performance test with seeded data | Documented p95 latency under target load in review notes | backend-agent |

### E-BE-6 — Comments & likes API
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| BE-6.1 | `POST /posts/{id}/comments`, `GET /posts/{id}/comments` (paginated) | Validates content length; correct pagination | backend-agent |
| BE-6.2 | `DELETE /comments/{id}` | Only author (or post owner) can delete | backend-agent |
| BE-6.3 | `POST/DELETE /posts/{id}/like` + like count on post payload | Unique like per user enforced at DB level | backend-agent |

### E-BE-7 — API documentation
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| BE-7.1 | OpenAPI/Swagger setup (springdoc) | `/swagger-ui` reflects all implemented endpoints accurately | backend-agent |

### E-FE-1 — Frontend project setup
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| FE-1.1 | Initialize Next.js (App Router, TypeScript, Tailwind) | Builds and runs; connects to backend base URL via env config | frontend-agent |
| FE-1.2 | Typed API client scaffold against `docs/api/` contracts | Central fetch wrapper with auth header injection & error handling | frontend-agent |
| FE-1.3 | App shell (layout, nav, auth-aware header) | Renders on all routes; shows login state correctly | frontend-agent |

### E-FE-2 — Auth UI
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| FE-2.1 | Register page + form validation | Matches backend validation rules; shows field errors | frontend-agent |
| FE-2.2 | Login page + token storage/refresh per auth ADR | Successful login redirects to feed; failure shows error | frontend-agent |
| FE-2.3 | Route protection for authenticated pages | Unauthenticated users redirected to login | frontend-agent |

### E-FE-3 — Profile & account UI
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| FE-3.1 | Public profile page (`/[username]`) | Shows bio, avatar, follower/following counts, posts grid | frontend-agent |
| FE-3.2 | Edit-profile page | Updates bio/avatar/display name; optimistic or confirmed update UX | frontend-agent |

### E-FE-4 — Post creation & feed UI
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| FE-4.1 | Post composer (text + multi-image upload with preview) | Upload progress/error states; matches BE-3.1 contract | frontend-agent |
| FE-4.2 | Feed page consuming `GET /feed` with pagination/infinite scroll | Loading, empty, and error states implemented | frontend-agent |
| FE-4.3 | Post detail view | Shows media gallery, like/comment counts, delete for owner | frontend-agent |

### E-FE-5 — Follow/subscribe UI
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| FE-5.1 | Follow/unfollow button with optimistic UI | Reflects real state after backend confirmation; handles errors | frontend-agent |
| FE-5.2 | Followers/following list pages | Paginated, links to profiles | frontend-agent |

### E-FE-6 — Comments & likes UI
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| FE-6.1 | Comment list + add-comment form on post detail | Paginated, optimistic add with rollback on failure | frontend-agent |
| FE-6.2 | Like button with count | Optimistic toggle, syncs with backend truth on error | frontend-agent |

### E-FE-7 — Responsiveness & accessibility polish
| Task | Description | Acceptance criteria | Agent |
|---|---|---|---|
| FE-7.1 | Mobile layout pass across all screens built so far | No horizontal scroll/broken layout at 375px width | frontend-agent |
| FE-7.2 | Accessibility pass (labels, contrast, keyboard nav) | Automated a11y check (e.g. axe) passes with no critical issues | frontend-agent |

---

## 5. Suggested repo layout

```
/docs
  /adr/                  # architecture decisions
  /api/                  # endpoint contracts
  /db/                   # schema.md, conventions.md
  deployment.md
/tasks
  /backend/BE-x.x.md
  /frontend/FE-x.x.md
  /database/DB-x.x.md
  /devops/OPS-x.x.md
/backend                 # Spring Boot project
/frontend                # Next.js project
.claude/agents/          # agent config files (copy from agents/ in this plan)
PROJECT_PLAN.md
```

## 6. Notes / open decisions for the user

These affect the schema and API contract, so architect-agent should resolve them (with
you) before E-BE-2 and E-DB-2 start in earnest:
- Auth: JWT-only vs JWT + refresh token vs session cookies?
- Image storage: local disk, S3-compatible (MinIO for local dev), or a managed service?
- IDs: bigint identity vs UUID for primary keys?
- Feed strategy: simple "query posts of followees" (fine at small scale) vs fan-out-on-write
  (needed later at larger scale) — start simple, document the migration path.
