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

## Phase 2 (post-MVP)

T1–T6 above is still "the MVP" per CLAUDE.md — this phase builds on top
of it once T1–T6 is done and reviewed. Same builder/reviewer process,
same "speed over polish" rules (DTO shortcuts allowed, every list
endpoint paginated, every write endpoint owner-checked).

**T7 — Recommendations, reposts, DMs, subscriptions screen (~90 min)**

*Recommendations panel (~15 min)*
Entities: none — derived from existing User/Follow data.
Endpoint: GET /recommendations — top N users by follower count,
excluding self and anyone already followed.
Frontend: "Who to follow" panel on the feed page, most prominent when
the feed is empty (new user with no follows yet); reuses FollowButton.
✅ A new user with no follows sees suggested accounts on their feed and
can follow them without leaving the page.

*Repost (~25 min)*
Entities: Repost (user, post, created_at; unique per user+post — no
double-repost).
Endpoints: POST /posts/{id}/repost, DELETE /posts/{id}/repost.
Frontend: Repost button on PostCard; reposts appear (labeled "X
reposted", original content shown read-only) on the reposter's profile
and in their followers' feeds, interleaved with original posts by
timestamp.
✅ Reposting a post makes it show up, labeled, on the reposter's
profile and in their followers' feeds.

*Direct messaging (~35 min)*
Entities: Message (sender, recipient, content, created_at). No
separate Conversation table — a conversation is just the pair of
usernames.
Endpoints: GET /conversations (threads the caller is part of, most
recent message first), GET /conversations/{username}/messages
(paginated thread with one other user), POST
/conversations/{username}/messages (send). No real-time — client
polls or refreshes on navigation, consistent with T1-T6's no-websocket
approach.
Frontend: /messages (conversation list) and /messages/[username]
(thread + composer); a "Message" link on profile pages.
✅ Two users can exchange messages and see the persisted thread from
either account.

*Subscriptions screen (~15 min)*
Entities: none — reuses Follow.
Endpoint: GET /users/{username}/following (paginated).
Frontend: /[username]/following page listing followed accounts with an
inline Unfollow button (reuses FollowButton).
✅ A user can view everyone they follow and unfollow directly from
that list, without visiting each profile individually.

Cut from T7 scope: no read receipts or unread-message badges, no
repost counts or comments-on-a-repost, no personalized recommendations
(follower-count ranking only), no followers list (only the
"subscriptions" / following list that was asked for).
