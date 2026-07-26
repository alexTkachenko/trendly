You are working in the Trendly repo (a social network for fashion
discovery: Spring Boot backend, PostgreSQL database, Next.js frontend).

Before writing any code, read:
- docs/PROJECT_PLAN.md — full epic/task breakdown, build order, and the
  task lifecycle (Backlog → In Progress → Ready for Review → reviewer
  verdict → Done)
- .claude/agents/*.md — six specialized agent configs (architect,
  backend, frontend, database, devops, reviewer)
- tasks/devops/OPS-1.1.md through OPS-1.4.md
- tasks/database/DB-1.1.md through DB-1.3.md

Rules for how we work, from PROJECT_PLAN.md — follow these exactly:
1. Work one task at a time, in the build order listed in
   PROJECT_PLAN.md section 3.
2. When implementing a task, act as the owning agent named in that
   task's frontmatter/header (e.g. devops-agent for OPS tasks,
   database-agent for DB tasks) and follow that agent's conventions
   file in .claude/agents/.
3. When a task is implemented, do NOT mark it Done yourself. Instead:
   start a fresh subagent/session acting as reviewer-agent
   (.claude/agents/reviewer-agent.md), give it ONLY the task file and
   the resulting diff (not your implementation reasoning), and have it
   produce a verdict: APPROVED or CHANGES REQUESTED. If changes are
   requested, fix them and re-review before moving on.
4. After an APPROVED review, update that task's status in
   PROJECT_PLAN.md and move to the next task in build order.
5. If a task depends on a decision that isn't documented yet (auth
   strategy, image storage, ID type — see PROJECT_PLAN.md section 6),
   stop and ask me instead of guessing, and record the decision as an
   ADR in docs/adr/ once I answer.
6. If a task references a task file that doesn't exist yet (e.g.
   BE-1.1, FE-1.1), write that task file first, in the same format as
   the existing ones in tasks/, before implementing it.

Start now with:
- tasks/devops/OPS-1.1.md (docker-compose for the local stack)
- tasks/database/DB-1.1.md (provision local Postgres)

Then continue in build order through E-OPS-1 and E-DB-1 as a whole
before touching backend or frontend code. Give me a short status update
after each task's review verdict rather than working silently through
all of them.