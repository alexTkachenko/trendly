# Progress Log

Chronological record of what was built in this session, on branch `mvp`.
Process for every task throughout: implement → curl smoke test → real
browser verification (Chrome automation) → fresh reviewer subagent
(task description + diff only, no chat history) → commit. All reviews
came back APPROVED before moving on.

## Repo setup

- Wrote the initial `CLAUDE.md` from the pre-implementation repo state
  (planning docs + env scaffolding only), then updated it twice more as
  the repo went from empty → full MVP → Gradle-based, so it always
  reflects the current state rather than the original plan.
- Drafted a Phase 2 plan for T7 (see `docs/MVP_PLAN.md`), kept in a
  section explicitly separate from T1–T6 so it doesn't redefine what
  "the MVP" means in this repo.

## T1–T6 — the MVP (`docs/MVP_PLAN.md`)

| Task | What shipped |
|---|---|
| T1 | docker-compose Postgres; Spring Boot 3.5/Java 21 skeleton (web, JPA `ddl-auto: update`, security stub, Actuator); Next.js 16 App Router + TypeScript + Tailwind skeleton |
| T2 | `User` entity; `POST /auth/register`, `POST /auth/login` (JWT), `GET /users/me`; `JwtAuthenticationFilter`; register/login pages, token in localStorage, auth-aware header |
| T3 | `Post` entity (text + optional image); `POST /posts` (multipart), `GET /posts/{id}`, `GET /users/{username}/posts` (paginated), `DELETE /posts/{id}` (owner-only); composer with image preview, profile page |
| T4 | `Follow` entity; `POST`/`DELETE /users/{username}/follow` (+ a `GET` status endpoint the frontend needed but the plan hadn't specified), `GET /feed`; follow button, feed page |
| T5 | `Comment` entity; `POST`+`GET /posts/{id}/comments` (paginated); new post-detail page (`/posts/[id]`) to host the comment thread, since no such page existed yet |
| T6 | Full two-account smoke pass (register → post → follow → feed → comment) in a real browser — nothing broke |

Two real bugs were caught and fixed during T3, before review:
a `LazyInitializationException` on `Post.author` (switched the
association to eager fetch), and a `GlobalExceptionHandler` catch-all
that was swallowing Spring's static-resource `NoResourceFoundException`
and turning missing media into `500`s instead of `404`s.

One process slip: the T2 commit accidentally swept in pre-existing
`.DS_Store`/`.idea/` files that were staged before the session started.
Caught it, added `.idea/` to `.gitignore`, and fixed it with a follow-up
commit (`chore: stop tracking IDE/OS cruft...`) rather than amending.

## Build tool migration

Switched the backend from Maven to Gradle by request: `pom.xml`/`mvnw`/
`.mvn/` replaced with `build.gradle` (Groovy DSL) + Gradle wrapper 9.6.1,
same dependency set. Updated `.claude/settings.local.json`'s pre-approved
commands (`./mvnw` → `./gradlew` equivalents) and `CLAUDE.md`. Verified
from a clean checkout: compile, `AuthServiceTest`, and a full `bootRun`
auth flow all pass under Gradle.

## T7 — post-MVP phase 2 (`docs/MVP_PLAN.md`, "Phase 2" section)

Documented first, then built one sub-feature at a time:

1. **Recommendations panel** — `GET /recommendations`, users ranked by
   follower count (correlated subquery + matching `countQuery` so
   pagination totals are correct), excluding self and anyone already
   followed. "Who to follow" panel on the feed page; gave `FollowButton`
   an optional `onFollowChange` callback (backward compatible) so a
   followed user drops out of the panel immediately.

2. **Repost** — redesigned mid-build at the user's direction: instead of
   a separate `Repost` entity, `Post` gained a nullable self-referencing
   `originalPost` field. A repost is just another row in `posts`
   (author = reposter, no content of its own, points at the true
   original — reposting a repost resolves to the original, no chains).
   A unique constraint on `(author_id, original_post_id)` blocks
   duplicate reposts without affecting ordinary posts. Because it's the
   same table, the *existing* paginated feed/profile queries interleave
   reposts with original posts by `created_at` for free — no merge
   logic needed. Caught and fixed a real
   `TransactionRequiredException` on the cascade-delete-reposts query
   (needed an explicit `@Transactional` on the repository method).

3. **Direct messaging** — `Message` entity (sender, recipient, content,
   timestamp; no separate `Conversation` table). The single-thread
   endpoint (`GET /conversations/{username}/messages`) is a clean
   paginated JPQL query; the conversation-list endpoint
   (`GET /conversations`) can't be expressed that cleanly in JPQL (needs
   "latest message per distinct partner"), so it fetches all of the
   caller's messages, reduces to one per partner in Java, and paginates
   the result with a new `PageResponse.ofList` helper — an explicit,
   documented O(message-count) tradeoff, fine at MVP scale. New
   `/messages` and `/messages/[username]` pages; "Message" link on
   profiles.

4. **Subscriptions screen** — `GET /users/{username}/following`
   (paginated). New `/[username]/following` page listing followed
   accounts with an inline unfollow button. Works for any username, not
   just your own (mirrors a public "Following" tab — the follow button
   always reflects the *viewer's* own relationship to each account).

Along the way, browser-automation clicks occasionally silently no-op'd
(stale element refs after an HMR reload, viewport resizes invalidating
cached coordinates) — not app bugs; confirmed by dispatching clicks via
JS and checking the resulting network requests.

## Current state

- 17 commits on `mvp` since the session started, each independently
  reviewed and building/testing clean.
- Nothing is running — dev servers and Postgres are stopped after the
  last verification pass. To run locally: `docker compose up -d postgres`,
  then `./gradlew bootRun` in `backend/` and `npm run dev` in `frontend/`.
- Full endpoint surface: auth (`/auth/register`, `/auth/login`,
  `/users/me`), posts (`/posts`, `/posts/{id}`,
  `/users/{username}/posts`), follow (`/users/{username}/follow`,
  `/users/{username}/following`), feed (`/feed`), comments
  (`/posts/{id}/comments`), repost (`/posts/{id}/repost`),
  recommendations (`/recommendations`), messaging (`/conversations`,
  `/conversations/{username}/messages`).
