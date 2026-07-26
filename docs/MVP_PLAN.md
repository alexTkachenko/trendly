# Trendly MVP Plan (~2.5h)

Goal: working demo — register, log in, post text+image, follow users,
see a feed, comment. Nothing more.

Agents: `builder` implements, `reviewer` checks each task in a fresh
context (task description + diff only). Reviewer verdict gates the next
task. That's the whole process.

Cut from scope: Flyway, CI/CD, tests (beyond smoke), likes, refresh
tokens, S3, profile editing, a11y/mobile polish, API docs.

## Tasks (in order)

**T1 — Stack up (~20 min)**
docker-compose with Postgres; Spring Boot skeleton (web, JPA, security,
`ddl-auto: update`); Next.js skeleton (TS + Tailwind). Both apps start.
✅ `docker compose up` + both dev servers run.

**T2 — Auth (~30 min)**
Entities: User. Endpoints: POST /auth/register, POST /auth/login (JWT),
GET /users/me. Frontend: register + login pages, token in localStorage,
redirect to /feed when logged in.
✅ Can register, log in, and see own username in the header.

**T3 — Posts (~35 min)**
Entities: Post (text, optional image path). Endpoints: POST /posts
(multipart, image saved to local disk, served statically), GET
/posts/{id}, GET /users/{username}/posts (paginated), DELETE /posts/{id}
(owner only). Frontend: composer with image preview + profile page
showing a user's posts.
✅ Can create a post with an image and see it on the profile page.

**T4 — Follow + feed (~30 min)**
Entities: Follow (unique pair, no self-follow). Endpoints: POST/DELETE
/users/{username}/follow, GET /feed (posts of followed users, paginated,
newest first). Frontend: follow button on profiles, /feed page with the
timeline.
✅ Following a user makes their posts appear in my feed.

**T5 — Comments (~25 min)**
Entities: Comment. Endpoints: POST + GET /posts/{id}/comments
(paginated). Frontend: comment list + form on the post view.
✅ Can comment on a post and see comments from others.

**T6 — Smoke pass (~10 min)**
Walk the whole flow with two users end to end; fix whatever broke.
✅ Register→post→follow→feed→comment works with 2 accounts.

Buffer: ~10 min. If running over, cut image upload from T3 (text-only
posts) — everything else still works.
